FROM squidfunk/mkdocs-material:latest

WORKDIR /docs

COPY mkdocs.yml .
COPY docs/ ./docs/

EXPOSE 8000

CMD ["serve", "--dev-addr=0.0.0.0:8000"]

