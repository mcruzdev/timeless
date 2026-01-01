# S3 Bucket
resource "aws_s3_bucket" "timeless_bucket" {
  bucket = var.bucket_assets_name
}

resource "aws_s3_bucket_acl" "timeless_bucket_acl" {
  bucket = aws_s3_bucket.timeless_bucket.id
  acl    = "private"
}

# IAM Users
resource "aws_iam_user" "timeless_whatsapp_app" {
  name = var.timeless_whatsapp_app_name
}

resource "aws_iam_user" "timeless_api_app" {
  name = var.timeless_api_app_name
}

# IAM Policies

resource "aws_iam_policy" "timeless_whatsapp_sqs_policy" {
  name = "AllowSendReceiveMessagePolicy"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "sqs:SendMessage",
          "sqs:GetQueueAttributes"
        ],
        Resource = ["${aws_sqs_queue.incoming_messages.arn}"]
      },
      {
        Effect = "Allow",
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ],
        Resource = ["${aws_sqs_queue.message_processed.arn}"]
      }
    ]
  })
}

resource "aws_iam_policy" "timeless_api_sqs_policy" {
  name = "AllowTimelessAPISendReceiveMessagePolicy"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "sqs:SendMessage",
          "sqs:GetQueueAttributes"
        ],
        Resource = ["${aws_sqs_queue.message_processed.arn}"]
      },
      {
        Effect = "Allow",
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ],
        Resource = ["${aws_sqs_queue.incoming_messages.arn}"]
      }
    ]
  })
}

resource "aws_iam_policy" "bucket_read_policy" {
  name        = "TimelessAPIBucketReadAccessPolicy"
  description = "Allows read access to ${var.bucket_assets_name} bucket"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "s3:GetObject"
        ],
        Resource = "${aws_s3_bucket.timeless_bucket.arn}/*"
      }
    ]
  })
}

resource "aws_iam_policy" "bucket_write_policy" {
  name        = "TimelessWhatsappBucketWriteAccessPolicy"
  description = "Allows write access to ${var.bucket_assets_name} bucket"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "s3:PutObject",
          "s3:PutObjectAcl",
          "s3:DeleteObject"
        ],
        Resource = "${aws_s3_bucket.timeless_bucket.arn}/*"
      }
    ]
  })
}

# IAM User Policy Attachments

resource "aws_iam_user_policy_attachment" "attach_bucket_write_policy" {
  user       = aws_iam_user.timeless_whatsapp_app.name
  policy_arn = aws_iam_policy.bucket_write_policy.arn
}

resource "aws_iam_user_policy_attachment" "attach_sqs_policy" {
  user       = aws_iam_user.timeless_whatsapp_app.name
  policy_arn = aws_iam_policy.timeless_whatsapp_sqs_policy.arn
}

resource "aws_iam_user_policy_attachment" "attach_api_bucket_read_policy" {
  user       = aws_iam_user.timeless_api_app.name
  policy_arn = aws_iam_policy.bucket_read_policy.arn
}

resource "aws_iam_user_policy_attachment" "attach_api_sqs_policy" {
  user       = aws_iam_user.timeless_api_app.name
  policy_arn = aws_iam_policy.timeless_api_sqs_policy.arn
}


# SQS Queues
resource "aws_sqs_queue" "incoming_messages" {
  fifo_queue                  = true
  content_based_deduplication = true
}

resource "aws_sqs_queue" "message_processed" {
  fifo_queue                  = true
  content_based_deduplication = true
}
