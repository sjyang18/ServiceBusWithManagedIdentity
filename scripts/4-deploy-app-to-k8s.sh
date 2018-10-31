#!/bin/bash

set -e

set -x
SELECTOR_NAME=java-sb-app
# had to create the identity in the generated RG
# https://github.com/Azure/aad-pod-identity/issues/38
# you will need to replace the resource group below with the correct name
# you may need to look in the Azure Portal to find the correct name
# or you can use the CLI with something like
# $az group list | grep 'k8s'

if [ -z "$ACR_SERVER" ]
then
      echo "ACR_SERVER Not Set. Set the env variable with the following command:"
      echo "export ACR_SERVER=\"Your ACR SERVER\" "
      return 1
fi

if [ -z "$DOCKER_IMG" ]
then
      echo "DOCKER_IMG Not Set. Set the env variable with the following command:"
      echo "export DOCKER_IMG=\"DOCKER_IMG NAME\" "
      return 1
fi

if [ -z "$DOCKER_IMG_TAG" ]
then
      echo "DOCKER_IMG_TAG Not Set. Set the env variable with the following command:"
      echo "export DOCKER_IMG_TAG=\"v1 or something\" "
      return 1
fi

if [ -z "$SB_NAMESPACE" ]
then
      echo "SB_NAMESPACE Not Set. Set the env variable with the following command:"
      echo "export DOCKERSB_NAMESPACE_IMG_TAG=\"your service bus namespace name\" "
      return 1
fi

if [ -z "$SB_QUEUENAME" ]
then
      echo "SB_QUEUENAME Not Set. Set the env variable with the following command:"
      echo "export SB_QUEUENAME=\"your queue name in your service bus namespace\" "
      return 1
fi

cat <<EOF | kubectl apply -f -
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  labels:
    app: ${SELECTOR_NAME}
    aadpodidbinding: ${SELECTOR_NAME}
  name: ${SELECTOR_NAME}
  namespace: default
spec:
  template:
    metadata:
      labels:
        app: ${SELECTOR_NAME}
        aadpodidbinding: ${SELECTOR_NAME}
    spec:
      containers:
      - name: ${SELECTOR_NAME}
        image: "${ACR_SERVER}/${DOCKER_IMG}:${DOCKER_IMG_TAG}"
        imagePullPolicy: Always
        command:
          - sleep
          - infinity
        env:
        - name: MY_POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: MY_POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: MY_POD_IP
          valueFrom:
            fieldRef:
              fieldPath: status.podIP
        - name: SB_NAMESPACE
          value: "${SB_NAMESPACE}"
        - name: SB_QUEUENAME
          value: "${SB_QUEUENAME}"
EOF