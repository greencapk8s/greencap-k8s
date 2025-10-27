[![pt-br](https://img.shields.io/badge/lang-pt--br-green.svg)](./docs/readme-translations/index/pt-br/RELEASE_NOTES.md)

# Release Notes - GreenCap K8s

## [v0.4.5] - 2025-10-26

### Customizable Installation System.

#### ✨ Updates:
- **Added** `--setup-type` parameter to `greencap.sh` with three modes:
    - `minimal`: installs only core components (kind, kubectl, cilium, ingress, apache-hello, kube-dashboard).
    - `full`: installs all available components including monitoring, harbor, gitlab, postgres, and ecom-python.
    - `custom`: reads from `greencap.ini` file for selective component installation.
- Reduces code duplication and improves maintainability.
- Desktop shortcuts for the urls: Dashboard, Hello Apache and Monitoring.

## [v0.4.4] - 2025-10-22

### Minor improvements and documentation updates.

#### ✨ Updates:
- **README.md**: Improved usage instructions and details for English and Portuguese users.
- **Documentation PT-BR**: Docs and installation/cleanup flows improved and aligned with the main README.

---

## [v0.4.3] - 2025-10-14

###  New installation manager.

#### ✨ New Features (Focused on DevEx):
- **greencap.sh**: Created a new installation manager improving DevEx.
- **New --clean parameter**: New parameter for cleaning up environments (vagrant, aws, local) after studies and testing. 

---

## [v0.4.2] - 2025-10-11

###  GitLab Support.

#### ✨ New Features:
- **GitLab**: GitLab is a complete DevSecOps platform that helps teams manage the entire software development lifecycle, from version control and code collaboration to automation, testing and deployment.
- **English language in portal**: Added support for English language in the portal https://www.greencapk8s.dev/.

---

## [v0.4.1] - 2025-09-23

### Added new parameter to installer.

#### ✨ New Features:
- **Local Debug**: New parameter (--local-debug) for local installation without VM. Recommended to speed up development of new features. 

---

## [v0.4.0] - 2025-08-XX

### ☁️ AWS Support (BETA) and Observability Improvements.

#### ✨ New Features:
- **AWS Deployment**: Complete support via Terraform
- **Cluster Metrics**: Metrics Server for `kubectl top` commands
- **Public IP Detection**: Automatic system for security group configuration
- **Environment-Specific Scripts**: Separation between Vagrant and EC2
- **Configuration Validation**: Pre-requirements: checks

---

## [v0.3.0] - 2025-07-XX

#### ✨ Added Features:
- **Container Registry**: Harbor for Docker image management
- **Observability Stack**: Prometheus + Grafana + Jaeger

---

## [v0.2.0] - 2025-07-XX

#### ✨ Added Features:
- **Kubernetes Dashboard**: Web interface for cluster management
- **Database**: PostgreSQL installed via Helm
- **Administration Interface**: pgAdmin for PostgreSQL management
- **Python Application**: FastAPI API connecting to PostgreSQL

---

## [v0.0.1] - 2025-07-XX

### 🎉 Initial Release

**First stable version of the project with basic features.**

#### ✨ Added Features:
- **Local Environment with Vagrant**: Complete VM provisioning with Ubuntu 22.04
- **Kubernetes Cluster with Kind**: Kubernetes IN Docker for local development
- **Ingress Controller**: Nginx Ingress Controller configured
- **Sample Application**: Hello Apache App for demonstration
- **Automation Scripts**: Modular installation system

