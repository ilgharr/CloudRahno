#!/bin/bash

echo "Starting the deployment process..."

# Navigate to the deployment directory
cd /home/ec2-user/deployment

# Build the new Docker image
echo "Building the new Docker image..."
docker build -t my_app:latest .

# Stop and remove old container if it exists
if [ "$(docker ps -aq -f name=my_app_container)" ]; then
  echo "Removing existing container..."
  docker stop my_app_container && docker rm my_app_container
fi

# Run the new Docker container
echo "Running the new Docker container..."
docker run --rm --name my_app_container -d -p 8443:8443 my_app:latest

echo "Deployment completed successfully!"