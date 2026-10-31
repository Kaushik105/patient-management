#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Ensure local user binaries (aws, lstk) are available in PATH
export PATH="$HOME/.local/bin:$HOME/.nvm/versions/node/v22.20.0/bin:$PATH"

ENDPOINT="${LOCALSTACK_ENDPOINT:-${ENDPOINT:-http://localhost:4566}}"
export LSTK_ENDPOINT_URL="$ENDPOINT"
export AWS_ENDPOINT_URL="$ENDPOINT"

export STACK_NAME="${STACK_NAME:-PatientManagementStack}"
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
export AWS_REGION="${AWS_REGION:-$AWS_DEFAULT_REGION}"
export LOCALSTACK_DEPLOY="${LOCALSTACK_DEPLOY:-true}"
export XDG_CACHE_HOME="${XDG_CACHE_HOME:-/tmp/localstack-cache}"
export JSII_RUNTIME_PACKAGE_CACHE_ROOT="${JSII_RUNTIME_PACKAGE_CACHE_ROOT:-/tmp/jsii-runtime-package-cache}"

TEMPLATE_FILE="./cdk.out/localstack.template.json"

# Determine AWS CLI command (prefer lstk aws if installed)
if command -v lstk >/dev/null 2>&1; then
    AWS_CMD="lstk aws"
    echo "Using LocalStack CLI: lstk aws"
else
    AWS_CMD="aws --endpoint-url=$ENDPOINT"
    echo "Using AWS CLI with endpoint: $ENDPOINT"
fi

# Build images if requested
if [ "$1" = "--build" ] || [ "${BUILD_IMAGES:-false}" = "true" ]; then
    echo "==> Building microservice Docker images..."
    docker build -t auth-service "$PROJECT_ROOT/auth-service"
    docker build -t billing-service "$PROJECT_ROOT/billing-service"
    docker build -t analytics-service "$PROJECT_ROOT/analytics-service"
    docker build -t patient-service "$PROJECT_ROOT/patient-service"
    docker build -t api-gateway "$PROJECT_ROOT/api-gateway"
    echo "==> Images built successfully."
fi

echo "==> Synthesizing CloudFormation template via AWS CDK..."
mvn -q compile exec:java
jq -c . "./cdk.out/PatientManagementStack.template.json" > "$TEMPLATE_FILE"

echo "==> Deleting previous stack (if any)..."
$AWS_CMD cloudformation delete-stack \
    --stack-name "$STACK_NAME" 2>/dev/null || true

echo "==> Waiting for stack deletion..."
$AWS_CMD cloudformation wait stack-delete-complete \
    --stack-name "$STACK_NAME" 2>/dev/null || true

echo "==> Deploying stack '$STACK_NAME' to LocalStack..."
$AWS_CMD cloudformation deploy \
    --stack-name "$STACK_NAME" \
    --template-file "$TEMPLATE_FILE" \
    --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM

echo "==> Querying Load Balancer DNS Name..."
$AWS_CMD elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text || true

echo "==> Deployment complete!"
