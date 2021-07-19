terraform {
  backend "s3" {
    bucket = "skycomposer-istio"
    key    = "terraform/backend"
    region = "us-west-2"
  }
}
