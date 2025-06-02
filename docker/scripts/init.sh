#!/bin/bash

awslocal sqs create-queue --queue-name incoming-messages
awslocal sqs create-queue --queue-name messages-processed
awslocal s3 mb s3://timeless-local-assets
