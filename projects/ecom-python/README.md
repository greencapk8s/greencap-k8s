# API FastAPI - ecom-python

Esta API lista produtos de uma base PostgreSQL chamada `ecom_python`.

## 1. Pré-requisitos
- Python 3.8+
- PostgreSQL rodando e com a base/tabela criada:

```sql
CREATE DATABASE ecom_python;
\c ecom_python
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
INSERT INTO products (name) VALUES ('Produto 1'), ('Produto 2');
```

## 2. Instalação

```sh
pip install -r requirements.txt
```

## 3. Configuração

Ajuste a string de conexão no arquivo `main.py` ou defina a variável de ambiente `DATABASE_URL`:

```
postgresql://postgres:<senha>@<host>:5432/ecom_python
```

## 4. Execução

```sh
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Acesse:
```
http://localhost:8000/products
```

## 5. Exemplo de resposta
```json
[
  {"id": 1, "name": "Produto 1"},
  {"id": 2, "name": "Produto 2"}
]
```

## 5. Docker

Para rodar a API em um container Docker:

```sh
# Build da imagem
docker build -t ecom-python-api .

# Rodar o container (ajuste a string de conexão se necessário)
docker run --rm -e DATABASE_URL=postgresql://postgres:<senha>@<host>:5432/ecom_python -p 8000:8000 ecom-python-api
```

Acesse:
```
http://localhost:8000/products
``` 