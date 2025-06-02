#!/bin/bash

awslocal sqs create-queue --queue-name incoming-messages.fifo --attributes '{"FifoQueue": "True"}'
awslocal sqs create-queue --queue-name messages-processed.fifo --attributes '{"FifoQueue": "True"}'

awslocal s3 mb s3://timeless-local-assets
