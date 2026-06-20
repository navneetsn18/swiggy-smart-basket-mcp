variable "region" {
  default = "ap-south-1"
}

variable "app_name" {
  default = "smart-basket-mcp"
}

variable "container_port" {
  default = 8080
}

variable "github_repo" {
  description = "owner/repo allowed to assume the deploy role via OIDC"
  default     = "navneetsn18/swiggy-smart-basket-mcp"
}

variable "db_username" {
  default = "postgres"
}

variable "db_password" {
  description = "RDS master password — supply via TF_VAR_db_password, never commit"
  type        = string
  sensitive   = true
}

variable "api_key" {
  description = "Value clients must send as X-API-Key — supply via TF_VAR_api_key"
  type        = string
  sensitive   = true
}

variable "image_tag" {
  description = "ECR image tag App Runner runs"
  default     = "latest"
}

variable "create_github_oidc_provider" {
  description = "false if the GitHub OIDC provider already exists in the account"
  type        = bool
  default     = true
}
