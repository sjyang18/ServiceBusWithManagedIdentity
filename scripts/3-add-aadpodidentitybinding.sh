#!/bin/bash

set -e

set -x
MANAGED_IDENTITY_NAME=seyan-dev3-aks-mi
SELECTOR_NAME=java-sb-app
# had to create the identity in the generated RG
# https://github.com/Azure/aad-pod-identity/issues/38
# you will need to replace the resource group below with the correct name
# you may need to look in the Azure Portal to find the correct name
# or you can use the CLI with something like
# $az group list | grep 'k8s'

if [ -z "$MC_RG" ]
then
      echo "K8S Resource Group Name Not Set. Set the env variable with the following command:"
      echo "export MC_RG=\"resource-group-name\" "
      return 1
fi

if [ -z "$SUB_ID" ]
then
      SUB_ID=$(az account show | jq -r .id)
      echo "Subscription ${SUB_ID} detected from environment"
fi

cat <<EOF | kubectl create -f -
apiVersion: "aadpodidentity.k8s.io/v1"
kind: AzureIdentityBinding
metadata:
  name: ${MANAGED_IDENTITY_NAME}-binding
spec:
  AzureIdentity: "${MANAGED_IDENTITY_NAME}"
  Selector: "${SELECTOR_NAME}"
EOF