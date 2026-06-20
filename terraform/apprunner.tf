# --- Access role: lets App Runner pull the image from ECR ---
data "aws_iam_policy_document" "apprunner_build_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["build.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "apprunner_access" {
  name_prefix        = "${var.app_name}-ar-access-"
  assume_role_policy = data.aws_iam_policy_document.apprunner_build_assume.json
}

resource "aws_iam_role_policy_attachment" "apprunner_ecr" {
  role       = aws_iam_role.apprunner_access.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess"
}

# --- Instance role: runtime permission to read the secrets ---
data "aws_iam_policy_document" "apprunner_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["tasks.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "apprunner_instance" {
  name_prefix        = "${var.app_name}-ar-inst-"
  assume_role_policy = data.aws_iam_policy_document.apprunner_tasks_assume.json
}

resource "aws_iam_role_policy" "apprunner_secrets" {
  role = aws_iam_role.apprunner_instance.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = [aws_secretsmanager_secret.db_password.arn, aws_secretsmanager_secret.api_key.arn]
    }]
  })
}

resource "aws_apprunner_service" "this" {
  service_name = var.app_name

  source_configuration {
    auto_deployments_enabled = false

    authentication_configuration {
      access_role_arn = aws_iam_role.apprunner_access.arn
    }

    image_repository {
      image_identifier      = "${aws_ecr_repository.app.repository_url}:${var.image_tag}"
      image_repository_type = "ECR"

      image_configuration {
        port = tostring(var.container_port)

        runtime_environment_variables = {
          SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.pg.address}:5432/smart_basket"
          SPRING_DATASOURCE_USERNAME = var.db_username
        }

        # Pulled from Secrets Manager at runtime (not baked into the image).
        runtime_environment_secrets = {
          SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.db_password.arn
          SMARTBASKET_API_KEY        = aws_secretsmanager_secret.api_key.arn
        }
      }
    }
  }

  instance_configuration {
    cpu               = "1024"
    memory            = "2048"
    instance_role_arn = aws_iam_role.apprunner_instance.arn
  }

  # TCP, not HTTP /sse: the API-key filter would 401 an unauthenticated HTTP probe.
  health_check_configuration {
    protocol = "TCP"
  }

  network_configuration {
    egress_configuration {
      egress_type       = "VPC"
      vpc_connector_arn = aws_apprunner_vpc_connector.this.arn
    }
  }
}
