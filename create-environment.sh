#!/bin/bash

# Unified script to create environment with or without GUI
# Usage: ./create-environment.sh --vagrant --gui --memory 8192 --cpus 4
# Usage: ./create-environment.sh --vagrant --no-gui --memory 4096 --cpus 2
# Usage: ./create-environment.sh --aws --instance-type t3a.medium --region us-east-1
# Usage: ./create-environment.sh --local-debug

set -e

# Default values
MEMORY=4096
CPUS=2
GUI=false
WITH_GUI=""
INSTALL_BROWSER=""
PROVIDER="vagrant"
AWS_INSTANCE_TYPE="t3a.medium"
AWS_REGION="us-east-1"
AWS_KEY_NAME=""
AWS_AMI_ID=""
AWS_SUBNET_ID=""
AWS_SECURITY_GROUP_ID=""
AWS_AUTO_APPROVE=false
AWS_PUBLIC_IP=""
USER_NAME_INSTALL="vagrant"

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Vagrant Environment Options (default):"
    echo "  --vagrant               Install environment using Vagrant (default)"
    echo "  --gui                   Install environment with GUI (Xubuntu + Firefox)"
    echo "  --no-gui                Install environment without GUI (terminal only)"
    echo "  --memory MB             Memory in MB (default: 4096)"
    echo "  --cpus NUM              Number of CPUs (default: 2)"
    echo ""
    echo "AWS Environment Options:"
    echo "  --aws                   Deploy to AWS EC2 via Terraform"
    echo "  --instance-type TYPE    AWS instance type (default: t3a.medium)"
    echo "  --region REGION         AWS region (default: us-east-1)"
    echo "  --key-name KEY          AWS key pair name (required for AWS)"
    echo "  --ami-id AMI            AWS AMI ID (optional)"
    echo "  --subnet-id SUBNET      AWS subnet ID (optional)"
    echo "  --security-group SG     AWS security group ID (optional)"
    echo "  --public-ip IP          Your public IP address for AWS security group access (required for AWS)"
    echo "  --auto-approve          Auto-approve terraform apply (default: false)"
    echo ""
    echo "Local Debug Options:"
    echo "  --local-debug           Execute local setup script for debugging"
    echo "  --user-name NAME        User name for local debug installation (default: vagrant)"
    echo ""
    echo "General Options:"
    echo "  --help                  Show this help message"
    echo ""
    echo "Examples:"
    echo "  Vagrant with GUI (default):"
    echo "    $0 --vagrant --gui --memory 8192 --cpus 4"
    echo "    $0 --gui --memory 8192 --cpus 4"
    echo "    $0 --no-gui --memory 2048 --cpus 1"
    echo ""
    echo "  AWS deployment:"
    echo "    $0 --aws --instance-type t3a.large --key-name my-key"
    echo "    $0 --aws --instance-type t3a.xlarge --region us-west-2 --key-name my-key"
    echo "    $0 --aws --instance-type t3a.medium --ami-id ami-12345 --key-name my-key"
    echo ""
    echo "  AWS with custom networking:"
    echo "    $0 --aws --instance-type t3a.medium --key-name my-key \\"
    echo "        --subnet-id subnet-12345 --security-group sg-12345"
    echo ""
    echo "  AWS with your public IP:"
    echo "    $0 --aws --instance-type t3a.medium --key-name my-key --public-ip 192.168.1.100"
    echo ""
    echo "  AWS with auto-approve:"
    echo "    $0 --aws --instance-type t3a.medium --key-name my-key --auto-approve"
    echo ""
    echo "  Local debug setup:"
    echo "    $0 --local-debug"
}

# Function to validate AWS prerequisites
validate_aws_prerequisites() {
    echo "🔍 Validating AWS prerequisites..."
    
    # Check if AWS CLI is installed
    if ! command -v aws &> /dev/null; then
        echo "❌ AWS CLI not found. Please install it first:"
        echo "   https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html"
        exit 1
    fi
    
    # Check if Terraform is installed
    if ! command -v terraform &> /dev/null; then
        echo "❌ Terraform not found. Please install it first:"
        echo "   https://developer.hashicorp.com/terraform/downloads"
        exit 1
    fi
        
    # Check if key name is provided
    if [ -z "$AWS_KEY_NAME" ]; then
        echo "❌ AWS key name is required. Use --key-name option."
        exit 1
    fi
    
    # Warn about auto-approve if enabled
    if [ "$AWS_AUTO_APPROVE" = true ]; then
        echo "⚠️  WARNING: Auto-approve is enabled!"
        echo "   This will apply Terraform changes in AWS directly."
        echo "   Make sure you have reviewed the plan before proceeding."
        echo ""
        read -p "Press Enter to continue or Ctrl+C to cancel..."
    fi
    
    echo "✅ AWS prerequisites validated!"
}

# Function to create Terraform configuration
create_terraform_config() {
    echo "📝 Creating Terraform configuration..."
    
    # Create terraform directory if it doesn't exist
    mkdir -p terraform
    
    # Generate configuration using the dedicated script
    cd terraform
    
    # Build command with all parameters
    GENERATE_CMD="./play-terraform.sh \
        --instance-type \"$AWS_INSTANCE_TYPE\" \
        --region \"$AWS_REGION\" \
        --key-name \"$AWS_KEY_NAME\" \
        --volume-size 50"
    
    if [ -n "$AWS_AMI_ID" ]; then
        GENERATE_CMD="$GENERATE_CMD --ami-id \"$AWS_AMI_ID\""
    fi
    
    if [ -n "$AWS_SUBNET_ID" ]; then
        GENERATE_CMD="$GENERATE_CMD --subnet-id \"$AWS_SUBNET_ID\""
    fi
    
    if [ -n "$AWS_SECURITY_GROUP_ID" ]; then
        GENERATE_CMD="$GENERATE_CMD --security-group \"$AWS_SECURITY_GROUP_ID\""
    fi
    
    GENERATE_CMD="$GENERATE_CMD --public-ip \"$AWS_PUBLIC_IP\""
    
    # Execute the command
    eval $GENERATE_CMD
    
    cd ..
    
    echo "✅ Terraform configuration created!"
}

# Function to deploy to AWS
deploy_to_aws() {
    echo "🚀 Deploying to AWS EC2..."
    
    # Validate prerequisites
    validate_aws_prerequisites
    
    # Create terraform directory if it doesn't exist
    mkdir -p terraform
    
    # Create Terraform configuration
    create_terraform_config
    
    # Initialize Terraform
    echo "🔧 Initializing Terraform..."
    cd terraform
    terraform init
    
    # Plan deployment
    echo "📋 Planning deployment..."
    terraform plan
    
    # Deploy
    echo "🚀 Deploying infrastructure..."
    if [ "$AWS_AUTO_APPROVE" = true ]; then
        echo "⚠️  Auto-approve enabled - applying changes without confirmation"
        terraform apply -auto-approve

        echo ""
        echo "=========================================="
        echo "✅ AWS deployment completed."
        echo "=========================================="
    else
        echo ""
        echo "=========================================="
        echo "✅ Terraform plan completed."
        echo "=========================================="
        echo ""
    fi
    
    cd ..
}

# Function to deploy to Vagrant
deploy_to_vagrant() {
    # Vagrant deployment
    echo "🛑 Stopping and destroying existing VM..."
    vagrant halt
    vagrant destroy -f
    sleep 2

    echo "🚀 Creating new VM..."
    if [ "$GUI" = true ]; then
        echo "📺 Installing with GUI (Xubuntu + Firefox)..."
        WITH_GUI=1 INSTALL_BROWSER=1 vagrant up
    else
        echo "💻 Installing without GUI (terminal only)..."
        vagrant up
    fi
    sleep 2

    echo "⏸️  Stopping VM for configuration..."
    vagrant halt
    sleep 2

    echo "⚙️  Configuring VM resources..."
    VM_NAME=$(VBoxManage list vms | grep "kind-nginx" | awk -F\" '{print $2}')
    VBoxManage modifyvm $VM_NAME --memory $MEMORY --cpus $CPUS

    echo "🔄 Reloading VM with Kind/K8s setup..."
    SETUP_KIND_K8S=1 vagrant reload --provision

    echo ""
    echo "=========================================="
    echo "✅ Environment created successfully!"
    echo "=========================================="
    echo "VM Configuration:"
    echo "  - GUI Mode: $([ "$GUI" = true ] && echo "Enabled" || echo "Disabled")"
    echo "  - Memory: ${MEMORY}MB"
    echo "  - CPUs: ${CPUS}"
    echo "  - Kind/K8s: Installed and configured"
    echo ""
    if [ "$GUI" = true ]; then
        echo "🌐 Access URLs (after VM is ready):"
        echo "  - Hello Apache: http://domain.local:30001/hello-apache/"
        echo "  - Kubernetes Dashboard: https://domain.local:30002/"
        echo "  - Harbor Registry: http://core.harbor.domain:30001/"
        echo "  - pgAdmin: http://pgadmin.local:30001/"
        echo "  - Grafana: http://grafana.local:30001/"
        echo "  - Jaeger: http://jaeger.local:30001/"
    else
        echo "💻 Terminal Access:"
        echo "  - SSH: vagrant ssh"
        echo "  - Test: curl http://domain.local:30001/hello-apache/"
    fi
    echo "=========================================="
}

# Function to execute local debug setup
deploy_local_debug() {
    echo "🔧 Executing local debug setup..."
    echo "👤 Using user name: $USER_NAME_INSTALL"
    USER_NAME_INSTALL="$USER_NAME_INSTALL" ./local/local-setup.sh
    
    echo ""
    echo "=========================================="
    echo "✅ Local debug setup completed successfully!"
    echo "=========================================="
    echo "Local environment is now configured for debugging."
    echo "User name used: $USER_NAME_INSTALL"
    echo "Check the output above for any errors or warnings."
    echo "=========================================="
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --vagrant)
            PROVIDER="vagrant"
            shift
            ;;
        --gui)
            GUI=true
            WITH_GUI="1"
            INSTALL_BROWSER="1"
            shift
            ;;
        --no-gui)
            GUI=false
            WITH_GUI=""
            INSTALL_BROWSER=""
            shift
            ;;
        --aws)
            PROVIDER="aws"
            shift
            ;;
        --local-debug)
            PROVIDER="local-debug"
            shift
            ;;
        --user-name)
            USER_NAME_INSTALL="$2"
            shift 2
            ;;
        --instance-type)
            AWS_INSTANCE_TYPE="$2"
            shift 2
            ;;
        --region)
            AWS_REGION="$2"
            shift 2
            ;;
        --key-name)
            AWS_KEY_NAME="$2"
            shift 2
            ;;
        --ami-id)
            AWS_AMI_ID="$2"
            shift 2
            ;;
        --subnet-id)
            AWS_SUBNET_ID="$2"
            shift 2
            ;;
        --security-group)
            AWS_SECURITY_GROUP_ID="$2"
            shift 2
            ;;
        --public-ip)
            AWS_PUBLIC_IP="$2"
            shift 2
            ;;
        --auto-approve)
            AWS_AUTO_APPROVE=true
            shift
            ;;
        --memory)
            MEMORY="$2"
            shift 2
            ;;
        --cpus)
            CPUS="$2"
            shift 2
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ "$PROVIDER" = "vagrant" ]; then
    if [ "$GUI" != true ] && [ "$GUI" != false ]; then
        echo "❌ Error: You must specify either --gui or --no-gui for vagrant deployment"
        show_usage
        exit 1
    fi
elif [ "$PROVIDER" = "aws" ]; then
    if [ -z "$AWS_KEY_NAME" ]; then
        echo "❌ Error: AWS key name is required. Use --key-name option."
        show_usage
        exit 1
    fi
    
    if [ -z "$AWS_PUBLIC_IP" ]; then
        echo "❌ Error: Your public IP address is required for AWS deployment."
        echo "   This IP will be used to allow your access to the AWS instance."
        echo "   Use --public-ip option to provide your public IP address."
        show_usage
        exit 1
    fi
elif [ "$PROVIDER" = "local-debug" ]; then
    # No additional validation needed for local-debug
    :
fi

echo "=========================================="
echo "Creating Environment"
echo "=========================================="
echo "Provider: $PROVIDER"
if [ "$PROVIDER" = "vagrant" ]; then
    echo "GUI Mode: $([ "$GUI" = true ] && echo "Enabled" || echo "Disabled")"
    echo "Memory: ${MEMORY}MB"
    echo "CPUs: ${CPUS}"
elif [ "$PROVIDER" = "aws" ]; then
    echo "Instance Type: $AWS_INSTANCE_TYPE"
    echo "Region: $AWS_REGION"
    echo "Key Name: $AWS_KEY_NAME"
    echo "Auto-approve: $([ "$AWS_AUTO_APPROVE" = true ] && echo "Enabled" || echo "Disabled")"
    if [ -n "$AWS_AMI_ID" ]; then
        echo "AMI ID: $AWS_AMI_ID"
    fi
    echo "Your Public IP: $AWS_PUBLIC_IP"
elif [ "$PROVIDER" = "local-debug" ]; then
    echo "Local Debug Setup: Enabled"
    echo "User Name: $USER_NAME_INSTALL"
fi
echo "=========================================="

if [ "$PROVIDER" = "aws" ]; then
    deploy_to_aws
elif [ "$PROVIDER" = "local-debug" ]; then
    deploy_local_debug
else
    deploy_to_vagrant
fi 