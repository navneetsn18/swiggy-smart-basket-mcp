# Terraform — AWS deploy (App Runner)

Provisions ECR, RDS PostgreSQL (private), Secrets Manager, an App Runner service,
and a GitHub OIDC deploy role. CI (`.github/workflows/deploy.yml`) builds the image,
pushes to ECR, and triggers an App Runner deployment.

> ⚠️ Hosting runs the **mock** Swiggy gateway — real Swiggy needs the browser/5-day-token
> flow that doesn't work headless (issue #1). Basket/memory/substitution/AI-basket/refill
> work; live Swiggy ordering does not until delegated auth is in place.

## Prerequisites
- Terraform ≥ 1.5, AWS credentials with admin-ish permissions.
- Docker + AWS CLI (only if pushing the bootstrap image by hand).

## Secrets (never commit)
```bash
export TF_VAR_db_password='<strong-password>'
export TF_VAR_api_key='<client-X-API-Key>'
```

## Bootstrap order (App Runner needs an image to exist first)
```bash
cd terraform
terraform init

# 1. Create ECR (+ supporting) first so an image can be pushed
terraform apply -target=aws_ecr_repository.app -target=aws_iam_role.gha_deploy

# 2. Push an initial image (either run the GitHub workflow once, or manually:)
aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin \
  "$(aws sts get-caller-identity --query Account --output text).dkr.ecr.ap-south-1.amazonaws.com"
docker build -t smart-basket-mcp ..
docker tag smart-basket-mcp:latest "<ecr_repository_url>:latest"
docker push "<ecr_repository_url>:latest"

# 3. Apply the rest (RDS, App Runner, etc.)
terraform apply
```

## After apply
- `terraform output github_actions_role_arn` → add as GitHub repo secret **`AWS_DEPLOY_ROLE_ARN`**.
- `terraform output app_runner_url` → your service URL. Call MCP at `<url>/sse` with header
  `X-API-Key: <TF_VAR_api_key>`.
- Push to `main` → the workflow builds, pushes, and redeploys automatically.

## Notes
- Health check is **TCP** (the API-key filter 401s unauthenticated HTTP probes).
- Set `-var create_github_oidc_provider=false` if the GitHub OIDC provider already exists
  in the account.
- RDS is private (reachable only from App Runner via the VPC connector).
