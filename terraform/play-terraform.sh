#!/bin/bash

# Script to generate Terraform configuration dynamically
# Usage: ./generate-config.sh --instance-type t3a.medium --region us-east-1 --key-name my-key

set -e

# Default values
INSTANCE_TYPE="t3a.medium"
REGION="us-east-1"
KEY_NAME=""
VOLUME_SIZE=50
SUBNET_ID=""
SECURITY_GROUP_ID=""
AMI_ID=""
PUBLIC_IP=""

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --instance-type TYPE    AWS instance type (default: t3a.medium)"
    echo "  --region REGION         AWS region (default: us-east-1)"
    echo "  --key-name KEY          AWS key pair name (required)"
    echo "  --volume-size GB        EBS volume size in GB (default: 50)"
    echo "  --ami-id AMI            AWS AMI ID (optional, uses data source if not specified)"
    echo "  --subnet-id SUBNET      AWS subnet ID (optional)"
    echo "  --security-group SG     AWS security group ID (optional)"
    echo "  --public-ip IP          Your public IP address for AWS security group access (required)"
    echo "  --help                  Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --instance-type t3a.large --key-name my-key"
    echo "  $0 --instance-type t3a.xlarge --region us-west-2 --key-name my-key"
    echo "  $0 --instance-type t3a.medium --ami-id ami-12345 --key-name my-key"
    echo "  $0 --instance-type t3a.medium --key-name my-key --public-ip 192.168.1.100"
}

# Function to validate AWS profile
validate_aws_profile() {
    echo "🔍 Validating AWS profile: k8s_playground"

    # Check if AWS CLI is installed
    if ! command -v aws &> /dev/null; then
        echo "❌ Error: AWS CLI is not installed"
        echo "Please install AWS CLI first: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html"
        exit 1
    fi

    # Check if AWS credentials directory exists
    if [ ! -d ~/.aws ]; then
        echo "❌ Error: AWS credentials directory not found at ~/.aws"
        echo "Please configure AWS credentials first:"
        echo ""
        echo "Run: aws configure --profile k8s_playground"
        echo "Or create the files manually:"
        echo ""
        echo "Create ~/.aws/credentials:"
        echo "  [k8s_playground]"
        echo "  aws_access_key_id = YOUR_ACCESS_KEY"
        echo "  aws_secret_access_key = YOUR_SECRET_KEY"
        echo ""
        echo "Create ~/.aws/config:"
        echo "  [profile k8s_playground]"
        echo "  region = us-east-1"
        echo "  output = json"
        exit 1
    fi

    # Check if credentials file exists
    if [ ! -f ~/.aws/credentials ]; then
        echo "❌ Error: AWS credentials file not found at ~/.aws/credentials"
        echo "Please configure AWS credentials first:"
        echo ""
        echo "Run: aws configure --profile k8s_playground"
        echo "Or create ~/.aws/credentials manually:"
        echo "  [k8s_playground]"
        echo "  aws_access_key_id = YOUR_ACCESS_KEY"
        echo "  aws_secret_access_key = YOUR_SECRET_KEY"
        exit 1
    fi

    # Check if config file exists
    if [ ! -f ~/.aws/config ]; then
        echo "❌ Error: AWS config file not found at ~/.aws/config"
        echo "Please create ~/.aws/config:"
        echo "  [profile k8s_playground]"
        echo "  region = us-east-1"
        echo "  output = json"
        exit 1
    fi

    # Check if k8s_playground profile exists in credentials
    if ! grep -q "\[k8s_playground\]" ~/.aws/credentials; then
        echo "❌ Error: Profile 'k8s_playground' not found in ~/.aws/credentials"
        echo "Please add the profile to your credentials file:"
        echo ""
        echo "Add to ~/.aws/credentials:"
        echo "  [k8s_playground]"
        echo "  aws_access_key_id = YOUR_ACCESS_KEY"
        echo "  aws_secret_access_key = YOUR_SECRET_KEY"
        exit 1
    fi

    # Check if k8s_playground profile exists in config
    if ! grep -q "\[profile k8s_playground\]" ~/.aws/config; then
        echo "❌ Error: Profile 'k8s_playground' not found in ~/.aws/config"
        echo "Please add the profile to your config file:"
        echo ""
        echo "Add to ~/.aws/config:"
        echo "  [profile k8s_playground]"
        echo "  region = us-east-1"
        echo "  output = json"
        exit 1
    fi

    # Test if the profile is working
    echo "🧪 Testing AWS profile authentication..."
    if ! aws sts get-caller-identity --profile k8s_playground &> /dev/null; then
        echo "❌ Error: AWS profile 'k8s_playground' authentication failed"
        echo "Please check your AWS credentials and permissions"
        echo ""
        echo "Test manually: aws sts get-caller-identity --profile k8s_playground"
        exit 1
    fi

    # Get account info
    ACCOUNT_INFO=$(aws sts get-caller-identity --profile k8s_playground --output json 2>/dev/null)
    if [ $? -eq 0 ]; then
        ACCOUNT_ID=$(echo "$ACCOUNT_INFO" | grep -o '"Account": "[^"]*"' | cut -d'"' -f4)
        USER_ARN=$(echo "$ACCOUNT_INFO" | grep -o '"Arn": "[^"]*"' | cut -d'"' -f4)
        echo "✅ AWS profile validated successfully!"
        echo "   Account ID: $ACCOUNT_ID"
        echo "   User ARN: $USER_ARN"
    else
        echo "❌ Error: Could not retrieve AWS account information"
        exit 1
    fi
}

# Function to validate parameters
validate_parameters() {
    if [ -z "$KEY_NAME" ]; then
        echo "❌ Error: --key-name is required"
        show_usage
        exit 1
    fi

    # Validate instance type
    case $INSTANCE_TYPE in
        t3a.medium|t3a.large|t3a.xlarge|t3a.2xlarge)
            ;;
        *)
            echo "❌ Error: Invalid instance type. Must be t3a.medium, t3a.large, t3a.xlarge, or t3a.2xlarge"
            exit 1
            ;;
    esac

    # Validate volume size
    if [ "$VOLUME_SIZE" -lt 8 ] || [ "$VOLUME_SIZE" -gt 1000 ]; then
        echo "❌ Error: Volume size must be between 8 and 1000 GB"
        exit 1
    fi
}

# Function to clean generated files and directories
clean_generated_files() {
    echo "🧹 Cleaning generated files and directories..."

    # List of generated files
    FILES=("main.tf" "variables.tf" "terraform.tfvars" "user_data.sh" ".terraform.lock.hcl" "main.tf.bak")

    for file in "${FILES[@]}"; do
        if [ -f "$file" ]; then
            rm -f "$file"
            echo "  - Removed: $file"
        fi
    done

    # Clean temporary directories if they exist (example: .terraform)
    if [ -d ".terraform" ]; then
        rm -rf .terraform
        echo "  - Removed directory: .terraform"
    fi

    # Clean Terraform state files if they exist
    for state_file in terraform.tfstate terraform.tfstate.backup; do
        if [ -f "$state_file" ]; then
            rm -f "$state_file"
            echo "  - Removed: $state_file"
        fi
    done

    echo "✅ Cleanup completed!"
}

# Function to get the public IP address
get_my_public_ip() {
    # If PUBLIC_IP is provided, use it
    if [ -n "$PUBLIC_IP" ]; then
        echo "$PUBLIC_IP"
        return
    fi
    
    # If no IP is provided, show error and exit
    echo "❌ Your public IP address is required."
    echo "Please provide your public IP address using --public-ip option."
    echo "This IP will be used to allow your access to the AWS instance."
    echo "Example: --public-ip 192.168.1.100"
    exit 1
}

# Function to replace ingress cidr_blocks with the user's public IP
replace_ingress_cidr_blocks() {
    local ip
    ip=$(get_my_public_ip)
    echo "🌐 Using your public IP: $ip"
    echo "   This IP will be used to allow your access to the AWS instance security groups."
    # Replace all ingress cidr_blocks with the detected IP in main.tf
    if [ -f "main.tf" ]; then
        # Only replace ingress blocks (not egress)
        # Assumes ingress blocks use cidr_blocks = ["127.0.0.1"]
        sed -i.bak "s/cidr_blocks = \[\"127\.0\.0\.1\"\]/cidr_blocks = [\"$ip\/32\"]/g" main.tf
        echo "✅ Security group ingress rules updated to allow access from $ip/32"
    else
        echo "⚠️  main.tf not found to update cidr_blocks."
    fi
}

# Function to generate main.tf
generate_main_tf() {
    echo "📝 Generating main.tf..."

    cat > main.tf << EOF
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  required_version = ">= 1.0"
}

provider "aws" {
  region = "${REGION}"
  profile = "k8s_playground"

  default_tags {
    tags = {
      Project     = "k8s-playground"
      Environment = "development"
      ManagedBy   = "terraform"
      category    = "k8s"
    }
  }
}

# VPC and networking
data "aws_vpc" "default" {
  default = true
}

data "aws_subnet" "default" {
  vpc_id = data.aws_vpc.default.id
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
  filter {
    name   = "state"
    values = ["available"]
  }
  filter {
    name   = "availability-zone"
    values = ["us-east-1a"]
  }
}

# Security group for K8S Playground
resource "aws_security_group" "k8s_playground" {
  name_prefix = "k8s-playground-"
  vpc_id      = data.aws_vpc.default.id
  description = "Security group for K8S Playground"

  # RDP access
  ingress {
    from_port   = 3389
    to_port     = 3389
    protocol    = "tcp"
    cidr_blocks = ["127.0.0.1"]
  }

  # All outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = {
    Name = "k8s-playground-sg"
  }
}

# Data source for Ubuntu AMI (only used if ami_id is not specified)
data "aws_ami" "ubuntu" {
  count = var.ami_id == null ? 1 : 0

  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
}

# EC2 instance
resource "aws_instance" "k8s_playground" {
  ami           = var.ami_id != null ? var.ami_id : data.aws_ami.ubuntu[0].id
  instance_type = "${INSTANCE_TYPE}"
  key_name      = "${KEY_NAME}"
  subnet_id     = data.aws_subnet.default.id
  iam_instance_profile = "ssm-access-role"

  vpc_security_group_ids = [aws_security_group.k8s_playground.id]

  root_block_device {
    volume_size = ${VOLUME_SIZE}
    volume_type = "gp3"
    encrypted   = true

    tags = {
      Name = "k8s-playground-root"
    }
  }

  user_data = base64encode(templatefile("\${path.module}/user_data.sh", {
    instance_type = "${INSTANCE_TYPE}"
    region        = "${REGION}"
  }))

  monitoring = true

  tags = {
    Name = "k8s-playground"
  }

  lifecycle {
    create_before_destroy = true
  }
}

# Outputs
output "setup_instructions" {
  description = "Instructions to setup K8S Playground"
  value = <<-EOT
    ==========================================
    ✅ AWS deployment completed!
    ==========================================

  EOT
}
EOF

    echo "✅ main.tf generated!"
}

# Function to generate terraform.tfvars
generate_tfvars() {
    echo "📝 Generating terraform.tfvars..."

    cat > terraform.tfvars << EOF
# Generated by generate-config.sh
aws_region = "${REGION}"
instance_type = "${INSTANCE_TYPE}"
key_name = "${KEY_NAME}"
volume_size = ${VOLUME_SIZE}
EOF

    if [ -n "$AMI_ID" ]; then
        echo "ami_id = \"${AMI_ID}\"" >> terraform.tfvars
    fi

    if [ -n "$SUBNET_ID" ]; then
        echo "subnet_id = \"${SUBNET_ID}\"" >> terraform.tfvars
    fi

    if [ -n "$SECURITY_GROUP_ID" ]; then
        echo "security_group_ids = [\"${SECURITY_GROUP_ID}\"]" >> terraform.tfvars
    fi

    echo "✅ terraform.tfvars generated!"
}

# Function to generate variables.tf
generate_variables_tf() {
    echo "📝 Generating variables.tf..."

    cat > variables.tf << EOF
variable "aws_region" {
  description = "AWS region to deploy the infrastructure"
  type        = string
  default     = "${REGION}"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "${INSTANCE_TYPE}"

  validation {
    condition     = can(regex("^t3a\\\\.(medium|large|xlarge|2xlarge)$", var.instance_type))
    error_message = "Instance type must be t3a.medium, t3a.large, t3a.xlarge, or t3a.2xlarge."
  }
}

variable "key_name" {
  description = "Name of the AWS key pair to use for SSH access"
  type        = string
  default     = "${KEY_NAME}"

  validation {
    condition     = can(regex("^[a-zA-Z0-9_-]+$", var.key_name)) && length(var.key_name) >= 3 && length(var.key_name) <= 50
    error_message = "Key name must be 3-50 characters long and contain only alphanumeric characters, hyphens, and underscores."
  }
}

variable "volume_size" {
  description = "Size of the root EBS volume in GB"
  type        = number
  default     = ${VOLUME_SIZE}

  validation {
    condition     = var.volume_size >= 50 && var.volume_size <= 1000
    error_message = "Volume size must be between 50 and 1000 GB."
  }
}

variable "subnet_id" {
  description = "Subnet ID to deploy the instance (optional, uses default if not specified)"
  type        = string
  default     = null
}

variable "security_group_ids" {
  description = "List of security group IDs (optional, creates new one if not specified)"
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Additional tags to apply to resources"
  type        = map(string)
  default = {
    Project     = "k8s-playground"
    Environment = "development"
    ManagedBy   = "terraform"
  }
}

variable "enable_monitoring" {
  description = "Enable detailed monitoring for the EC2 instance"
  type        = bool
  default     = true
}

variable "enable_encryption" {
  description = "Enable encryption for the EBS volume"
  type        = bool
  default     = true
}

variable "ami_id" {
  description = "AMI ID to use for the EC2 instance (optional, uses data source if not specified)"
  type        = string
  default     = null
}
EOF

    echo "✅ variables.tf generated!"
}

# Function to generate user_data.sh
generate_user_data() {
    echo "📝 Generating user_data.sh..."

    cat > user_data.sh << 'EOF'
#!/bin/bash

# User data script for K8S Playground EC2 instance
# This script runs when the instance first boots

set -e

# Update system
echo "🔄 Updating system packages..."
apt-get update
apt-get upgrade -y

# Install essential packages
echo "📦 Installing essential packages..."
apt-get install -y \
    curl \
    wget \
    git \
    unzip \
    software-properties-common \
    apt-transport-https \
    ca-certificates \
    gnupg \
    lsb-release \
    htop \
    tree \
    vim \
    nano

# Install Docker
echo "🐳 Installing Docker..."
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Start and enable Docker
# systemctl start docker
# systemctl enable docker

# Add ubuntu user to docker group
usermod -aG docker ubuntu

echo "=========================================="
echo "🚀 K8S Playground - AWS Instance Ready!"
echo "=========================================="

echo "🔄 Rebooting in 30 seconds to apply all changes..."
sleep 30
reboot
EOF

    echo "✅ user_data.sh generated!"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --instance-type)
            INSTANCE_TYPE="$2"
            shift 2
            ;;
        --region)
            REGION="$2"
            shift 2
            ;;
        --key-name)
            KEY_NAME="$2"
            shift 2
            ;;
        --volume-size)
            VOLUME_SIZE="$2"
            shift 2
            ;;
        --subnet-id)
            SUBNET_ID="$2"
            shift 2
            ;;
        --security-group)
            SECURITY_GROUP_ID="$2"
            shift 2
            ;;
        --ami-id)
            AMI_ID="$2"
            shift 2
            ;;
        --public-ip)
            PUBLIC_IP="$2"
            shift 2
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            echo "❌ Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Main execution
echo "=========================================="
echo "🔧 Terraform Configuration Generator"
echo "=========================================="

clean_generated_files
sleep 5
validate_aws_profile
validate_parameters
generate_main_tf
replace_ingress_cidr_blocks
generate_variables_tf
generate_user_data
generate_tfvars

echo ""
echo "✅ Configuration generated successfully!"
echo ""
echo "📋 Generated files:"
echo "  - main.tf"
echo "  - variables.tf"
echo "  - user_data.sh"
echo "  - terraform.tfvars"
echo ""
echo "=========================================="