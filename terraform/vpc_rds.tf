# Uses the account's default VPC/subnets — no custom networking to maintain.
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# App Runner egress SG (via the VPC connector) — source for the RDS ingress rule.
resource "aws_security_group" "apprunner" {
  name_prefix = "${var.app_name}-apprunner-"
  vpc_id      = data.aws_vpc.default.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# RDS reachable only from App Runner (not public).
resource "aws_security_group" "rds" {
  name_prefix = "${var.app_name}-rds-"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.apprunner.id]
  }
}

resource "aws_db_subnet_group" "rds" {
  name_prefix = "${var.app_name}-"
  subnet_ids  = data.aws_subnets.default.ids
}

resource "aws_db_instance" "pg" {
  identifier             = "${var.app_name}-db"
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  db_name                = "smart_basket"
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.rds.name
  vpc_security_group_ids  = [aws_security_group.rds.id]
  publicly_accessible    = false
  skip_final_snapshot    = true
  apply_immediately      = true
}

resource "aws_apprunner_vpc_connector" "this" {
  vpc_connector_name = "${var.app_name}-vpc"
  subnets            = data.aws_subnets.default.ids
  security_groups    = [aws_security_group.apprunner.id]
}
