# List commands utils to debug kubernetes.

# Kubernetes.
## List logs of a pod.
kubectl logs -n ingress-nginx <pod-name>

# Postgres.
## PostgreSQL can be accessed via port 5432 on the following DNS names from within your cluster:
postgres-17-postgresql.postgresql.svc.cluster.local - Read/Write connection

## To get the password for "postgres" run:
export POSTGRES_PASSWORD=$(kubectl get secret --namespace postgresql postgres-17-postgresql -o jsonpath="{.data.postgres-password}" | base64 -d)

## To connect to your database run the following command:
kubectl run postgres-17-postgresql-client --rm --tty -i --restart='Never' --namespace postgresql --image docker.io/bitnami/postgresql:17.5.0 --env="PGPASSWORD=$POSTGRES_PASSWORD" \
    --command -- psql --host postgres-17-postgresql -U postgres -d postgres -p 5432

## Create database.
kubectl run postgres-17-postgresql-client --rm --tty -i --restart='Never' --namespace postgresql --image docker.io/bitnami/postgresql:17.5.0 --env="PGPASSWORD=$POSTGRES_PASSWORD" \
    --command -- psql --host postgres-17-postgresql -U postgres -d postgres -p 5432 -c "CREATE DATABASE ecom_python;"

## To connect to your database from outside the cluster execute the following commands:
kubectl port-forward --namespace postgresql svc/postgres-17-postgresql 5432:5432 &
PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U postgres -d postgres -p 5432

## Reinstall postgres.
kubectl delete pvc data-postgres-17-postgresql-0 -n postgresql
helm install postgres-17 bitnami/postgresql --namespace postgresql --set image.tag=17.5.0

# Harbor.
## Connect to harbor database.
kubectl run harbor-postgres-client --rm --tty -i --restart='Never' --namespace harbor --image docker.io/bitnami/postgresql:17.5.0 --env="PGPASSWORD=changeit" \
    --command -- psql --host harbor-database -U postgres -d postgres -p 5432

## Uninstall harbor.
helm uninstall harbor -n harbor

## Delete pvc.
kubectl delete pvc --all -n harbor

