#!/usr/bin/env bash -ex

kubectl create secret generic rabbitmq-server-credentials \
    --from-literal=RABBITMQ_DEFAULT_USER=rabbit-user-dev \
    --from-literal=RABBITMQ_DEFAULT_PASS=rabbit-pwd-dev \
    --save-config

kubectl create secret generic rabbitmq-credentials \
    --from-literal=SPRING_RABBITMQ_USERNAME=rabbit-user-dev \
    --from-literal=SPRING_RABBITMQ_PASSWORD=rabbit-pwd-dev \
    --save-config

kubectl create secret generic mongodb-server-credentials \
    --from-literal=MONGO_INITDB_ROOT_USERNAME=mongodb-user-dev \
    --from-literal=MONGO_INITDB_ROOT_PASSWORD=mongodb-pwd-dev \
    --save-config

kubectl create secret generic mongodb-credentials \
    --from-literal=SPRING_DATA_MONGODB_AUTHENTICATION_DATABASE=admin \
    --from-literal=SPRING_DATA_MONGODB_USERNAME=mongodb-user-dev \
    --from-literal=SPRING_DATA_MONGODB_PASSWORD=mongodb-pwd-dev \
    --save-config

kubectl create secret generic mysql-server-credentials \
    --from-literal=MYSQL_ROOT_PASSWORD=rootpwd \
    --from-literal=MYSQL_DATABASE=review-db \
    --from-literal=MYSQL_USER=mysql-user-dev \
    --from-literal=MYSQL_PASSWORD=mysql-pwd-dev \
    --save-config

kubectl create secret generic mysql-credentials \
    --from-literal=SPRING_DATASOURCE_USERNAME=mysql-user-dev \
    --from-literal=SPRING_DATASOURCE_PASSWORD=mysql-pwd-dev \
    --save-config


# Deploy the resources and wait for their pods to become ready
kubectl apply -f ../k3s
kubectl wait --timeout=600s --for=condition=ready pod --all
kubectl wait --timeout=600s --for=condition=available deployment --all