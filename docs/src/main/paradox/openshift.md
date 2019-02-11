
```
export TOKEN=<my-token>
export OPENSHIFT_SERVER=c

## Use a project name that will not clash with other deployments on the cluster
export OPENSHIFT_PROJECT=lagom-scala-minimal-deployment-example
export IMAGE_HELLO=hello-lagom
export IMAGE_HELLO_PROXY=hello-lagom-proxy
export TAG=1.0-SNAPSHOT

## The registry should be accessible from the cluster where you deploy
export DOCKER_REGISTRY_SERVER=my-docker-registry.mycompany.com
export DOCKER_REGISTRY=$DOCKER_REGISTRY_SERVER/$OPENSHIFT_PROJECT
```


Start an `oc` session:
```
oc login https://$OPENSHIFT_SERVER --token=$TOKEN
oc new-project $OPENSHIFT_PROJECT
```

Create and push the images:

```
sbt docker:publishLocal

docker login -p $TOKEN -u unused $DOCKER_REGISTRY_SERVER
docker tag $IMAGE_HELLO:$TAG $DOCKER_REGISTRY/$IMAGE_HELLO:$TAG
docker tag $IMAGE_HELLO_PROXY:$TAG $DOCKER_REGISTRY/$IMAGE_HELLO_PROXY:$TAG

docker push $DOCKER_REGISTRY/$IMAGE_HELLO:$TAG
docker push $DOCKER_REGISTRY/$IMAGE_HELLO_PROXY:$TAG
```

Deploy:

```
$ kustomize build deployment/overlays/openshift | oc apply -f -
```

Await until all pods are ready:

```
$ oc get all
NAME                                                                  READY     STATUS    RESTARTS   AGE
pod/hello-lagom-minimal-service-v1-0-snapshot-9f467d85-5vx4h          1/1       Running   0          19m
pod/hello-lagom-minimal-service-v1-0-snapshot-9f467d85-srsms          1/1       Running   0          19m
pod/hello-lagom-minimal-service-v1-0-snapshot-9f467d85-wm72g          1/1       Running   0          19m
pod/hello-proxy-lagom-minimal-service-v1-0-snapshot-77c99fb7fdmj6tb   1/1       Running   0          23m

NAME                                        TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)    AGE
service/hello-lagom-minimal-service         ClusterIP   172.30.178.145   <none>        9000/TCP   23m
service/hello-proxy-lagom-minimal-service   ClusterIP   172.30.137.163   <none>        9000/TCP   23m

...
```

Test:
```
$ curl -H "Host: my-lagom-minimal.example.org"         http://$OPENSHIFT_PROJECT.$OPENSHIFT_SERVER/api/hello/alice
```