#!/usr/bin/env bash -ex

kubectl delete configmap config-repo-product-composite
kubectl delete configmap config-repo-product
kubectl delete configmap config-repo-recommendation
kubectl delete configmap config-repo-review 

kubectl delete secret rabbitmq-server-credentials

kubectl delete secret rabbitmq-credentials

kubectl delete secret mongodb-server-credentials

kubectl delete secret  mongodb-credentials

kubectl delete secret mysql-server-credentials

kubectl delete secret mysql-credentials


# Deploy the resources and wait for their pods to become ready
kubectl delete -f ../k3s