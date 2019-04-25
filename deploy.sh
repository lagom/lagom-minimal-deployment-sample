#!/bin/bash



export OPENSHIFT_SERVER=centralpark2.lightbend.com
export OPENSHIFT_PROJECT=lagom-scala-openshift-smoketests
export IMAGE_HELLO=hello-lagom
export IMAGE_HELLO_PROXY=hello-proxy-lagom
export TAG=1.7-SNAPSHOT

export DOCKER_REGISTRY_SERVER=docker-registry-default.centralpark2.lightbend.com
export DOCKER_REGISTRY=$DOCKER_REGISTRY_SERVER/$OPENSHIFT_PROJECT

TOKEN=$1

oc login https://$OPENSHIFT_SERVER --token=$TOKEN

oc delete project $OPENSHIFT_PROJECT
# while the project is deleted, we build the docker imges
sbt clean docker:publishLocal
oc new-project    $OPENSHIFT_PROJECT

docker login -p $TOKEN -u unused $DOCKER_REGISTRY_SERVER

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

kustomize build deployment/overlays/openshift | oc apply -f -

echo "Try this deployment using:"
echo 'curl -H "Host: my-lagom-openshift-smoketests.example.org"  https://$OPENSHIFT_PROJECT.$OPENSHIFT_SERVER/proxy/rest-hello/alice'