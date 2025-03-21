#!/bin/bash

ERROR_LOG="run_build_error"

echo "Starting the deployment process..."

# Navigate to the deployment directory
cd /home/ec2-user/deployment || { echo "Failed to navigate to deployment directory" | tee -a "$ERROR_LOG"; exit 1; }

# Build the new Docker image
echo "Building the new Docker image..."
docker build -t my_app:latest . 2>>"$ERROR_LOG"
if [ $? -ne 0 ]; then
  echo "Docker build failed, check ${ERROR_LOG} for details." | tee -a "$ERROR_LOG"
  exit 1
fi

# Stop and remove old container if it exists
if [ "$(docker ps -aq -f name=my_app_container)" ]; then
  echo "Removing existing container..."
  docker stop my_app_container 2>>"$ERROR_LOG" && docker rm my_app_container 2>>"$ERROR_LOG"
  if [ $? -ne 0 ]; then
    echo "Failed to stop/remove the container, check ${ERROR_LOG} for details." | tee -a "$ERROR_LOG"
    exit 1
  fi
fi

# Run the new Docker container
echo "Running the new Docker container..."
docker run --rm --name my_app_container -d -p 8443:8443 my_app:latest 2>>"$ERROR_LOG"
if [ $? -ne 0 ]; then
  echo "Failed to run the Docker container, check ${ERROR_LOG} for details." | tee -a "$ERROR_LOG"
  exit 1
fi

# Restart the httpd service
echo "Restarting the httpd service..."
sudo systemctl restart httpd 2>>"$ERROR_LOG"
if [ $? -ne 0 ]; then
  echo "Failed to restart httpd service, check ${ERROR_LOG} for details." | tee -a "$ERROR_LOG"
  exit 1
fi

echo "Deployment completed successfully!"