# Step-by-step AWS deployment guide

Deploys the Smart Basket MCP server to **AWS App Runner** with **RDS PostgreSQL**,
using **Terraform** for infra and **GitHub Actions** for CI/CD.

> Hosting runs the **mock** Swiggy gateway. The basket / memory / substitution /
> AI-basket / refill features work live; real Swiggy ordering stays local until
> delegated auth (issue #1). Region: **ap-south-1**.

> 💸 Costs money: RDS `db.t3.micro` + App Runner run ~$25–40/month. `terraform destroy`
> removes everything (step 9).

---

## Step 0 — Install tools (one-time, on your machine)

```bash
brew install awscli terraform   # macOS; or see each tool's install docs
docker --version                # install Docker Desktop if missing
aws --version && terraform -version && docker --version
```

## Step 1 — AWS account + credentials

1. Create/log into an AWS account.
2. In IAM, create a user with **AdministratorAccess** (for setup) and an **access key**.
3. Configure the CLI:
   ```bash
   aws configure          # paste Access Key, Secret, region = ap-south-1
   aws sts get-caller-identity     # should print your account id
   ```

## Step 2 — Get the code (merge the deploy PR, then pull)

1. Merge **PR #5** (`aws-deploy` → `main`) on GitHub.
2. Locally:
   ```bash
   cd /Users/navneetsn18/Documents/Swiggy
   git checkout main && git pull
   ```

## Step 3 — Set the secrets (never committed)

Pick a strong DB password and an API key your MCP clients will send:

```bash
export TF_VAR_db_password='ChooseAStrongPassword123!'
export TF_VAR_api_key='choose-a-long-random-client-key'
```

## Step 4 — Init Terraform

```bash
cd terraform
terraform init
terraform plan        # review what it will create; fix any errors before applying
```

## Step 5 — Bootstrap (create ECR first, push one image)

App Runner needs an image to exist before it can start.

```bash
# 5a. create just the ECR repo + deploy role
terraform apply -target=aws_ecr_repository.app -target=aws_iam_role.gha_deploy

# 5b. find your ECR URL + account, then log in
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGION=ap-south-1
ECR_URL="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/smart-basket-mcp"
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"

# 5c. build the app image and push it (run from repo root)
cd ..
docker build -t "$ECR_URL:latest" .
docker push "$ECR_URL:latest"
cd terraform
```

## Step 6 — Apply the rest (RDS, App Runner, secrets, OIDC)

```bash
terraform apply        # type 'yes'. RDS takes ~5–10 min.
```

## Step 7 — Wire up CI

```bash
terraform output github_actions_role_arn      # copy the ARN
terraform output app_runner_url               # your live URL
```

On GitHub → repo **Settings → Secrets and variables → Actions → New repository secret**:
- Name: `AWS_DEPLOY_ROLE_ARN`
- Value: the ARN from above.

## Step 8 — Test it

```bash
URL=$(terraform output -raw app_runner_url)

# Without the key → 401
curl -s -o /dev/null -w "%{http_code}\n" "$URL/sse"

# With the key → 200 (streams; Ctrl+C to stop)
curl -N -H "X-API-Key: $TF_VAR_api_key" "$URL/sse"
```

Or point an MCP client at it (bridges via mcp-remote, passing the key as a header):
```json
{ "mcpServers": { "smart-basket": {
  "command": "npx",
  "args": ["mcp-remote", "https://YOUR-APP-RUNNER-URL/sse", "--header", "X-API-Key:YOUR_KEY"]
} } }
```

## Ongoing — auto-deploy

After step 7, every push to `main` runs `.github/workflows/deploy.yml`:
build image → push to ECR → App Runner redeploys. No manual steps.

## Step 9 — Tear down (stop all charges)

```bash
cd terraform
terraform destroy
```

---

## Troubleshooting
- **`terraform plan` errors** — the HCL wasn't lintable on the author's machine; fix any
  resource/arg the plan flags, then continue.
- **OIDC provider already exists** — `terraform apply -var create_github_oidc_provider=false`.
- **App Runner stuck "Operation in progress"** — first deploy waits on RDS; give it ~10 min.
- **App Runner can't reach the DB** — confirm the VPC connector + RDS security group applied
  (they're in `vpc_rds.tf`).
- **Health check failing** — it's TCP on the app port; make sure the container starts (check
  App Runner application logs in CloudWatch).
