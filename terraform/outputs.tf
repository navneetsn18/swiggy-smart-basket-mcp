output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "app_runner_url" {
  description = "Public HTTPS URL of the service"
  value       = "https://${aws_apprunner_service.this.service_url}"
}

output "github_actions_role_arn" {
  description = "Set as the GitHub secret AWS_DEPLOY_ROLE_ARN"
  value       = aws_iam_role.gha_deploy.arn
}
