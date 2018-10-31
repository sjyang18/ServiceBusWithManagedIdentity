# Service Bus + Azure Managed Identity + Kubernetes 

This sample project shows the same service bus Java client app you run on VM with an assigned managed identity can be deployed on kubernetes with no code change. Using the same managed identity assigned to Pod, Pod will retrieves its access token from metadata service and calls Service Bus SDK API.

Refer to [Service-Bus-Java-SDK sample README document](https://github.com/Azure/azure-service-bus-java/blob/dev/README.md) for service bus setup instructions. 

Refer to [Assign Azure Active Directory Identities to kubernetes applications](https://github.com/Azure/aad-pod-identity) for setting up aadidentity and aadidentitybinding Kubernetet CRD.
