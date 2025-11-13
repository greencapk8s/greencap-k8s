#!/bin/bash

set -e

# Default values
MEMORY=4096
CPUS=2
GUI=true
WITH_GUI="1"
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
SETUP_TYPE="minimal"
CLEAN_MODE=false

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Vagrant Environment Options (default):"
    echo "  --vagrant               Install environment using Vagrant with GUI (default)"
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
    echo "Local Options:"
    echo "  --local                 Execute local setup script"
    echo "  --user-name NAME        User name for local installation (default: vagrant)"
    echo "  --setup-type TYPE       Setup type: minimal, full, or custom (default: minimal)"
    echo ""
    echo "General Options:"
    echo "  --help                  Show this help message"
    echo "  --clean                 Clean the environment"
    echo ""
    echo "Examples:"
    echo "  Vagrant with GUI:"
    echo "    $0 --vagrant --memory 8192 --cpus 4"
    echo "    $0 --vagrant --memory 4096 --cpus 2"
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
    echo "  Local setup:"
    echo "    $0 --local"
    echo "    $0 --local --setup-type full"
    echo "    $0 --local --setup-type minimal --user-name myuser"
    echo ""
    echo "  Clean/Destroy environment:"
    echo "    $0 --clean --local            # Clean local environment"
    echo "    $0 --clean --vagrant          # Clean Vagrant environment"
    echo "    $0 --clean --aws              # Clean AWS environment"
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
deploy_aws() {
    echo "🚀 Deploying to AWS EC2..."

    # Validate prerequisites
    validate_aws_prerequisites

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
deploy_vagrant() {
    echo "🛑 Stopping and destroying existing VM..."
    vagrant halt
    vagrant destroy -f
    sleep 2

    echo "🚀 Creating new VM..."
    echo "Installing with GUI (Ubuntu + Xfce)..."
    WITH_GUI=1 vagrant up
    sleep 2

    echo "Stopping VM for configuration..."
    vagrant halt
    sleep 2

    echo "Configuring VM resources..."
    VM_NAME=$(VBoxManage list vms | grep "greencap-k8s" | awk -F\" '{print $2}')
    VBoxManage modifyvm $VM_NAME --memory $MEMORY --cpus $CPUS

    echo "Reloading VM setup..."
    SETUP_KIND_K8S=1 SETUP_TYPE="$SETUP_TYPE" vagrant reload --provision

    echo ""
    echo "=========================================="
    echo "GreenCap K8s installed."
    echo "Environment created successfully."
    echo "=========================================="
}

# Function to execute local setup
deploy_local() {
    PROVIDER="$PROVIDER" USER_NAME_INSTALL="$USER_NAME_INSTALL" SETUP_TYPE="$SETUP_TYPE" ./installers/run-installers.sh

    echo ""
    echo "=========================================="
    echo "✅ Local setup completed successfully!"
    echo "=========================================="
    echo "Local environment is now configured."
    echo "User name used: $USER_NAME_INSTALL"
    echo "Setup type used: $SETUP_TYPE"
    echo "Check the output above for any errors or warnings."
    echo "=========================================="
}

# Function to clean environment
clean_environment() {
    echo "=========================================="
    echo "🧹 Cleaning Environment"
    echo "=========================================="

    # Auto-detect provider if not specified
    if [ "$PROVIDER" = "vagrant" ]; then
        echo "Provider: Vagrant"
        clean_vagrant
    elif [ "$PROVIDER" = "aws" ]; then
        echo "Provider: AWS"
        clean_aws
    elif [ "$PROVIDER" = "local" ]; then
        echo "Provider: Local"
        clean_local
    else
        echo "❌ Provider not found"
        exit 1
    fi
}

# Function to clean Vagrant environment
clean_vagrant() {
    echo "🗑️  Cleaning Vagrant environment..."

    echo "🛑 Stopping VM..."
    vagrant halt 2>/dev/null || true

    echo "🗑️  Destroying VM..."
    vagrant destroy -f

    echo ""
    echo "=========================================="
    echo "Vagrant environment cleaned successfully."
    echo "=========================================="
}

# Function to clean AWS environment
clean_aws() {
    echo "🗑️  Cleaning AWS environment..."

    cd terraform

    if [ ! -f "terraform.tfstate" ] || [ ! -s "terraform.tfstate" ]; then
        echo "⚠️  No Terraform state found. No AWS resources to destroy."
        cd ..
        return
    fi

    echo "🔧 Initializing Terraform..."
    terraform init

    echo "📋 Planning destruction..."
    terraform plan -destroy

    echo ""
    echo "⚠️  WARNING: This will destroy all AWS resources created by Terraform!"
    echo "   This action cannot be undone."
    echo ""

    if [ "$AWS_AUTO_APPROVE" = true ]; then
        echo "⚠️  Auto-approve enabled - destroying resources without confirmation"
        terraform destroy -auto-approve
    else
        read -p "Are you sure you want to destroy the AWS environment? (yes/no): " -r
        echo
        if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
            terraform destroy
        else
            echo "❌ Destruction cancelled."
            cd ..
            exit 0
        fi
    fi

    cd ..

    echo ""
    echo "=========================================="
    echo "AWS environment cleaned successfully."
    echo "=========================================="
}

# Function to clean local environment
clean_local() {
    echo "🗑️  Cleaning local environment..."
    kind delete cluster --name greencap-k8s
    echo "Local environment cleaned successfully."
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --vagrant)
            PROVIDER="vagrant"
            shift
            ;;
        --aws)
            PROVIDER="aws"
            shift
            ;;
        --local)
            PROVIDER="local"
            shift
            ;;
        --user-name)
            USER_NAME_INSTALL="$2"
            shift 2
            ;;
        --setup-type)
            SETUP_TYPE="$2"
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
        --clean)
            CLEAN_MODE=true
            shift
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

# Check if we're in clean mode
if [ "$CLEAN_MODE" = true ]; then
    clean_environment
    exit 0
fi

# Validate required parameters
if [ "$PROVIDER" = "aws" ]; then
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
elif [ "$PROVIDER" = "local" ]; then
    # Validate setup type
    if [[ ! "$SETUP_TYPE" =~ ^(minimal|full|custom)$ ]]; then
        echo "❌ Error: Invalid setup type '$SETUP_TYPE'. Must be one of: minimal, full, custom"
        show_usage
        exit 1
    fi
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
elif [ "$PROVIDER" = "local" ]; then
    echo "Local Setup: Enabled"
    echo "User Name: $USER_NAME_INSTALL"
fi
echo "Setup Type: $SETUP_TYPE"
echo "=========================================="

if [ "$PROVIDER" = "aws" ]; then
    deploy_aws
elif [ "$PROVIDER" = "vagrant" ]; then
    deploy_vagrant
else
    deploy_local
fi