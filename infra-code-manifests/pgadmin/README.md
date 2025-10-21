[![pt-br](https://img.shields.io/badge/lang-pt--br-green.svg)](../../docs/readme-translations/pgadmin/pt-br/README.md)

# Accessing pgAdmin on Kubernetes

This document explains how to access pgAdmin4 installed on the Kubernetes cluster via Helm and Ingress.

```
http://pgadmin.greencap:30001/
```

## Login

- **Username:** The email defined in the Helm installation (e.g., `admin@admin.com`)
- **Password:** The password defined in the Helm installation (e.g., `admin-user`)

## Connect to ecom-python database via pgadmin

Add New Server:

- **Hostname/address:** Postgres service (e.g., postgres-17)
- **Username:** postgres
- **Password:** Password provided during installation (e.g., user-root123)

## References
- [pgadmin4 Helm Chart (runix)](https://artifacthub.io/packages/helm/runix/pgadmin4)
- [pgAdmin Official Documentation](https://www.pgadmin.org/)
