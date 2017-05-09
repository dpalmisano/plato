.DEFAULT_GOAL := error

AWS_ACCESS_KEY_ID := $(shell awsc personal key)
AWS_SECRET_ACCESS_KEY := $(shell awsc personal secret)

login:
	AWS_ACCESS_KEY_ID=$(AWS_ACCESS_KEY_ID) AWS_SECRET_ACCESS_KEY_ID=$(AWS_SECRET_ACCESS_KEY) sbt ecr:login 

publish:
	AWS_ACCESS_KEY_ID=$(AWS_ACCESS_KEY_ID) AWS_SECRET_ACCESS_KEY_ID=$(AWS_SECRET_ACCESS_KEY) sbt ecr:push

error:
	@echo "Please choose one of the following target: login, publish"
	@exit 2
