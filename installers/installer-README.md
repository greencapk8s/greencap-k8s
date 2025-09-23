# Installer.py - Script Python para Execução de Shell Scripts

Um script Python robusto e especializado em executar comandos e scripts shell com funcionalidades avançadas.

## 🚀 Funcionalidades

- ✅ **Execução segura** de comandos shell
- 🔄 **Retry automático** em caso de falha
- ⏱️ **Timeout configurável** para evitar travamentos
- 📊 **Monitoramento de recursos** (CPU, memória)
- 🔍 **Validação de pré-requisitos** do sistema
- 📝 **Logging detalhado** com diferentes níveis
- 🎯 **Execução paralela** de comandos
- 📋 **Relatórios JSON** de execução
- 🛡️ **Tratamento de sinais** (Ctrl+C)
- 🧪 **Modo dry-run** para simulação

## 📦 Instalação

```bash
# Instalar dependências
pip install psutil pyyaml

# Tornar executável
chmod +x installer.py
```

## 🎯 Uso Básico

### 1. Executar comando único
```bash
python installer.py --command "echo 'Hello World'"
```

### 2. Executar script shell
```bash
python installer.py --script "./installers/install-kind.sh"
```

### 3. Executar baseado em configuração YAML
```bash
python installer.py --config installer-config.yaml
```

## ⚙️ Opções Avançadas

```bash
# Com timeout personalizado
python installer.py --command "sleep 100" --timeout 30

# Com retry
python installer.py --command "curl http://example.com" --retry 5

# Modo dry-run (simulação)
python installer.py --config config.yaml --dry-run

# Log verboso
python installer.py --command "ls -la" --verbose

# Salvar log em arquivo
python installer.py --config config.yaml --log-file install.log

# Gerar relatório JSON
python installer.py --config config.yaml --report report.json
```

## 📋 Arquivo de Configuração YAML

Exemplo de `installer-config.yaml`:

```yaml
# Configurações gerais
settings:
  stop_on_error: true
  validate_prerequisites: true
  monitor_resources: true

# Comandos shell
commands:
  - "echo 'Verificando sistema...'"
  - "whoami"
  - "pwd"

# Scripts para executar
scripts:
  - path: "./installers/install-kind.sh"
    args: []
    description: "Instalar Kind"
  
  - path: "./installers/install-kubectl.sh"
    args: ["--version", "latest"]
    description: "Instalar kubectl"
```

## 📊 Relatório de Execução

O script gera relatórios detalhados incluindo:

- ✅ Taxa de sucesso
- ⏱️ Tempo de execução
- 💻 Uso de recursos (CPU, memória)
- 📝 Logs de cada comando
- ❌ Detalhes de falhas

Exemplo de relatório:
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "summary": {
    "total_commands": 10,
    "successful_commands": 9,
    "failed_commands": 1,
    "success_rate": 90.0,
    "total_execution_time": 45.2
  },
  "resource_usage": {
    "duration": 45.2,
    "peak_cpu": 85.5,
    "peak_memory": 65.2
  }
}
```

## 🔧 Exemplos Práticos

### Instalar ambiente Kubernetes
```bash
python installer.py --config k8s-install.yaml --report k8s-install-report.json
```

### Executar testes
```bash
python installer.py --config test-suite.yaml --parallel --max-jobs 4
```

### Backup com validação
```bash
python installer.py --config backup-config.yaml --validate-prerequisites
```

### Logs

```bash
# Log detalhado
python installer.py --verbose --log-file debug.log

# Verificar logs
tail -f debug.log
```
