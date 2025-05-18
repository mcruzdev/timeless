resource "aws_s3_bucket" "timeless_bucket" {
  bucket = var.bucket_assets_name
  acl    = "private"
}

resource "aws_iam_user" "timeless_assets_writer" {
  name = var.timeless_assets_writer_name
}

resource "aws_iam_policy" "bucket_write_policy" {
  name        = "BucketWriteAccessPolicy"
  description = "Allows write access to ${var.bucket_assets_name} bucket"
  policy      = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "s3:PutObject",
          "s3:PutObjectAcl",
          "s3:GetObject"
        ],
        Resource = "${aws_s3_bucket.timeless_bucket.arn}/*"
      }
    ]
  })
}

resource "aws_iam_user_policy_attachment" "attach_policy" {
  user       = aws_iam_user.timeless_assets_writer.name
  policy_arn = aws_iam_policy.bucket_write_policy.arn
}
