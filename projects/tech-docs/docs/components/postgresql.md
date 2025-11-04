# PostgreSQL Database

PostgreSQL is a powerful, open-source relational database system used by applications in the GreenCap platform.

## Access

### pgAdmin Web Interface

URL: [http://pgadmin.greencap:30001](http://pgadmin.greencap:30001)

```
Username: admin@admin.com
Password: admin-user
```

## Using pgAdmin

### Add Server Connection

**Add Server**

Configure connection:
  - **General** tab:
    - Name: PostgreSQL-GreenCap
  - **Connection** tab:
    - Host: postgres-17
    - Port: 5432
    - Database: postgres
    - Username: postgres
    - Password: user-root123

  - Click **Save**

**Get the password from command line:**
```bash
kubectl get secret postgres-17 -n postgresql \
  -o jsonpath="{.data.POSTGRES_PASSWORD}" | base64 -d
```
