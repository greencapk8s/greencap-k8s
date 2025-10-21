[![pt-br](https://img.shields.io/badge/lang-pt--br-green.svg)](../../docs/readme-translations/harbor/pt-br/README.md)

# Harbor - Container Registry

This document explains how to access Harbor for the first time after installation via Helm.

## Access Address

Harbor is exposed via Ingress at the address:

```
https://core.harbor.greencap:30002
```

## Default Credentials

- **Username:** `admin`
- **Password:** `Harbor12345`

> **Note:** If you customized the password during Helm installation, use the password defined in the values file.

## First Access

1. Open the browser and access: `https://core.harbor.greencap:30002`
2. Log in with the credentials above
3. On the first login, Harbor may request that you change the `admin` user password

## 5. Docker Operations

### Simple Examples

#### Login to Harbor
```bash
docker login core.harbor.greencap:30001
# Username: admin
# Password: Harbor12345
```

#### Push an image to Harbor
```bash
# Tag your image
docker tag hello-world:latest core.harbor.greencap:30001/library/hello-world:latest

# Push to Harbor
docker push core.harbor.greencap:30001/library/hello-world:latest
```

#### Pull an image from Harbor
```bash
# Pull from Harbor
docker pull core.harbor.greencap:30001/library/hello-world:latest
```

## References

- [Harbor Official Documentation](https://goharbor.io/docs/)
- [Harbor Helm Chart](https://artifacthub.io/packages/helm/harbor/harbor) 