# GitLab CI/CD

GitLab provides a complete DevOps platform with Git repository management and CI/CD pipelines.

!!! info "Installation Time"
    GitLab installation can take 10-15 minutes. Please be patient!

## Access

URL: [http://gitlab.greencap:30001](http://gitlab.greencap:30001)

```
Username: root
Password: (check installation output or reset via CLI)
```

## Getting Started

### Create Your First Project

1. Login to GitLab
2. Click **New project** → **Create blank project**
3. Enter project details:
   - Project name
   - Visibility level
4. Click **Create project**

## CI/CD Pipelines

### Basic Pipeline Example

Create `.gitlab-ci.yml` in your repository:

```yaml
stages:
  - build
  - test
  - deploy

variables:
  DOCKER_IMAGE: core.harbor.greencap:30001/greencap-apps/myapp

build:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  script:
    - docker build -t $DOCKER_IMAGE:$CI_COMMIT_SHA .
    - docker tag $DOCKER_IMAGE:$CI_COMMIT_SHA $DOCKER_IMAGE:latest
    - docker login -u $HARBOR_USER -p $HARBOR_PASSWORD core.harbor.greencap:30001
    - docker push $DOCKER_IMAGE:$CI_COMMIT_SHA
    - docker push $DOCKER_IMAGE:latest
  only:
    - main

test:
  stage: test
  image: python:3.11
  script:
    - pip install -r requirements.txt
    - pytest tests/
  only:
    - main
    - merge_requests

deploy:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/myapp myapp=$DOCKER_IMAGE:$CI_COMMIT_SHA -n production
    - kubectl rollout status deployment/myapp -n production
  only:
    - main
  when: manual
```

### Pipeline Stages

Common pipeline stages:

1. **Build**: Compile code, build Docker images
2. **Test**: Run unit tests, integration tests
3. **Security**: Vulnerability scanning, code quality
4. **Deploy**: Deploy to environments
5. **Verify**: Smoke tests, health checks
