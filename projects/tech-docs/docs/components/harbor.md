# Harbor Container Registry

Harbor is an open-source container registry that secures artifacts with policies and role-based access control.

## Access

URL: [http://core.harbor.greencap:30001](http://core.harbor.greencap:30001)

```
Username: admin
Password: Harbor12345
```

!!! warning "Change Default Password"
    Make sure to change the default password after first login!

## Getting Started

### Create a Project

1. Login to Harbor UI
2. Click **Projects** → **New Project**
3. Enter project name (e.g., `greencap-apps`)
4. Set access level (Public/Private)
5. Click **OK**

### Configure Docker to Use Harbor

### Login to Harbor

```bash
docker login core.harbor.greencap:30001
# Username: admin
# Password: Harbor12345
```

### Push an Image

```bash
# Tag your image
docker tag myapp:latest core.harbor.greencap:30001/greencap-apps/myapp:latest

# Push to Harbor
docker push core.harbor.greencap:30001/greencap-apps/myapp:latest
```

### Pull an Image

```bash
docker pull core.harbor.greencap:30001/greencap-apps/myapp:latest
```

## Project Management

### User Roles

| Role | Permissions |
|------|-------------|
| Project Admin | Full control |
| Maintainer | Manage images, scan, replicate |
| Developer | Push/pull images |
| Guest | Pull images only |
| Limited Guest | Pull images (limited) |

### Adding Members

1. Go to **Projects** → Select project → **Members**
2. Click **User** or **Group**
3. Search and select user
4. Assign role
5. Click **OK**

## Robot Accounts

Create service accounts for automation:

1. Go to **Projects** → Select project → **Robot Accounts**
2. Click **New Robot Account**
3. Enter name and description
4. Set expiration
5. Select permissions
6. Click **Add** and save the token

**Use in CI/CD:**
```bash
docker login core.harbor.greencap:30001 \
  --username robot$myapp \
  --password <robot-token>
```

## Best Practices

### Security

- Change default passwords
- Enable vulnerability scanning

### Image Naming

Use semantic versioning:
```
core.harbor.greencap:30001/project/app:v1.2.3
core.harbor.greencap:30001/project/app:latest
core.harbor.greencap:30001/project/app:1.2.3-alpine
```

### Tag Management

- Use retention policies
- Tag images with build numbers
- Maintain latest, stable, dev tags
- Clean up regularly
