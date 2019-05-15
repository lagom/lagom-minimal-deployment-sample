#!/bin/bash

export OPENSHIFT_SERVER=centralpark2.lightbend.com
export OPENSHIFT_PROJECT=lagom-scala-openshift-smoketests
export IMAGE_HELLO=hello-lagom
export IMAGE_HELLO_PROXY=hello-proxy-lagom
export TAG=1.7-SNAPSHOT

export DOCKER_REGISTRY_SERVER=docker-registry-default.centralpark2.lightbend.com
export DOCKER_REGISTRY=$DOCKER_REGISTRY_SERVER/$OPENSHIFT_PROJECT

## 1. login to the cluster
oc login https://$OPENSHIFT_SERVER -p $CP2_PLAY_PASSWORD -u play-team || exit 1

## 2. Recreate the Openshift Project
# 2.1 while the project is deleted, we build the docker images
oc delete project $OPENSHIFT_PROJECT

## 3. create the docker images
sbt clean docker:publishLocal

# 2.2 Do create the project (deletion often takes time to complete and propagate)
oc new-project $OPENSHIFT_PROJECT

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

## 6. Deploy
kustomize build deployment/overlays/openshift | oc apply -f -

echo "Try this deployment using:"
echo "export OPENSHIFT_SERVER=centralpark2.lightbend.com"
echo "export OPENSHIFT_PROJECT=lagom-scala-openshift-smoketests"
echo 'curl -H "Host: my-lagom-openshift-smoketests.example.org"  https://$OPENSHIFT_PROJECT.$OPENSHIFT_SERVER/proxy/rest-hello/alice'