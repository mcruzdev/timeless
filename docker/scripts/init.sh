#!/bin/bash

awslocal sqs create-queue --queue-name incoming-message.fifo --attributes '{"FifoQueue": "True"}'
awslocal sqs create-queue --queue-name recognized-message.fifo --attributes '{"FifoQueue": "True"}'

awslocal s3 mb s3://timeless-local-assets
