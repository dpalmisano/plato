
# Setting AWS credentials
AWS_ACCESS_KEY_ID=`awsc personal key`
AWS_SECRET_ACCESS_KEY=`awsc personal secret`

sbt ecr:login ecr:push
