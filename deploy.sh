#!/bin/bash

export OPENSHIFT_SERVER=centralpark2.lightbend.com
export OPENSHIFT_PROJECT=lagom-scala-openshift-smoketests
export IMAGE_HELLO=hello-lagom
export IMAGE_HELLO_PROXY=hello-proxy-lagom
export TAG=1.7-SNAPSHOT

export DOCKER_REGISTRY_SERVER=docker-registry-default.centralpark2.lightbend.com
export DOCKER_REGISTRY=$DOCKER_REGISTRY_SERVER/$OPENSHIFT_PROJECT

## 1. login to the cluster
login() {
    echo "Attempting login to Openshift cluster (this will fail on PR builds)"
    if [ -z ${CP2_PLAY_PASSWORD+x} ]; then echo "CP2_PLAY_PASSWORD is unset."; else echo "CP2_PLAY_PASSWORD is available."; fi
    oc login https://$OPENSHIFT_SERVER --username=play-team --password=$CP2_PLAY_PASSWORD  || exit 1
}


## 2. Recreate the Openshift Project
# 2.1 while the project is deleted, we build the docker images
deleteProject() {
    oc delete project $OPENSHIFT_PROJECT
}

## 3. create the docker images
buildImages() {
    sbt clean docker:publishLocal
}

# 2.2 Do create the project (deletion often takes time to complete and propagate)
createProject() {
    oc new-project $OPENSHIFT_PROJECT
}

pushImages() {
    ## 4. `docker login` to the image stream of the project (aka, use the docker registry 
    ##    dedicated to our openshift project)
    # `docker login` to the openshift registry only works with token (not username/password credentials)
    # so we inject the token via stdin and ignore the username argument.
    oc whoami -t | docker login -u unused --password-stdin $DOCKER_REGISTRY_SERVER

    ## 5. Tag images and push them
    echo "Tagging image: $IMAGE_HELLO:$TAG $DOCKER_REGISTRY/$IMAGE_HELLO:$TAG"
    echo "  - $IMAGE_HELLO:$TAG"
    echo "  - $DOCKER_REGISTRY/$IMAGE_HELLO:$TAG"
    docker tag $IMAGE_HELLO:$TAG $DOCKER_REGISTRY/$IMAGE_HELLO:$TAG

    echo "Tagging image: $IMAGE_HELLO_PROXY:$TAG $DOCKER_REGISTRY/$IMAGE_HELLO_PROXY:$TAG"
    echo "  - $IMAGE_HELLO_PROXY:$TAG"
    echo "  - $DOCKER_REGISTRY/$IMAGE_HELLO_PROXY:$TAG"
    docker tag $IMAGE_HELLO_PROXY:$TAG $DOCKER_REGISTRY/$IMAGE_HELLO_PROXY:$TAG

    docker push $DOCKER_REGISTRY/$IMAGE_HELLO:$TAG
    docker push $DOCKER_REGISTRY/$IMAGE_HELLO_PROXY:$TAG
}

deploy() {
    kustomize build deployment/overlays/openshift | oc apply -f -
}

# Waits for the application to come up and be ready
# Will time out after 300 seconds
# This is a simplified version of https://github.com/lightbend/openshift-deployment-guide/blob/master/bin/testAllLagomShoppingCarts.sh
waitForApp() {
    CONTAINERS="1/1" # all pods on this app use a single container
    REPLICAS=4 # we expect a total of 3+1 pods to be ready 
    count=0
    echo -n "Waiting for smoketest to be provisioned."
    while [ "$(oc get pods --no-headers=true | grep Running | grep "$CONTAINERS" | wc -l)" -lt $REPLICAS ]
    do
        sleep 2 ## sleep 2 * $count 150 == 300 sec timeout
        echo -n "."
        (( count = count + 1 ))
        if [ $count -gt 150 ]
        then
            echo " failed."
            # Find any nodes that are in error, and output their logs
            for pod in $(oc get pods --no-headers | grep -v Running | cut -f1 -d" ")
            do
                echo Logs for $pod:
                oc logs --all-containers $pod || :
            done
            exit 1
        fi
    done

    echo " Pods up!"
}


testDeployment() {
    echo "Try this deployment using:"
    echo "export OPENSHIFT_SERVER=centralpark2.lightbend.com"
    echo "export OPENSHIFT_PROJECT=lagom-scala-openshift-smoketests"
    echo 'curl -H "Host: my-lagom-openshift-smoketests.example.org"  https://$OPENSHIFT_PROJECT.$OPENSHIFT_SERVER/proxy/rest-hello/alice'

    count=0
    echo -n "Waiting for smoketest to be form cluster and serve traffic."
    while [ "$(curl -H "Host: my-lagom-openshift-smoketests.example.org"  https://$OPENSHIFT_PROJECT.$OPENSHIFT_SERVER/proxy/rest-hello/alice | grep "Hi alice" | wc -l )" -ne 1 ]
    do
        sleep 2 ## sleep 2 * $count 150 == 300 sec timeout
        echo -n "."
        (( count = count + 1 ))
        if [ $count -gt 150 ]
        then
            echo " Pods are up but service is failing."
            # Find any nodes that are in error, and output their logs
            for pod in $(oc get pods --no-headers | grep -v Running | cut -f1 -d" ")
            do
                echo Logs for $pod:
                oc logs --all-containers $pod || :
            done
            exit 1
        fi
    done

    echo " Service up and running."


}



login
deleteProject
buildImages
createProject
pushImages
deploy
waitForApp
testDeployment
