#!/bin/bash



export OPENSHIFT_SERVER=centralpark.lightbend.com
export OPENSHIFT_PROJECT=lagom-scala-minimal-deployment-example
export IMAGE_HELLO=hello-lagom
export IMAGE_HELLO_PROXY=hello-proxy-lagom
export TAG=1.6-SNAPSHOT

export DOCKER_REGISTRY_SERVER=docker-registry-default.centralpark.lightbend.com
export DOCKER_REGISTRY=$DOCKER_REGISTRY_SERVER/$OPENSHIFT_PROJECT

TOKEN=$1


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

