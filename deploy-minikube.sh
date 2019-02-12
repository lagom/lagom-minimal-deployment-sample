#!/bin/bash


eval $(minikube docker-env)

sbt clean docker:publishLocal
kustomize build deployment/overlays/minikube | kubectl apply -f -

