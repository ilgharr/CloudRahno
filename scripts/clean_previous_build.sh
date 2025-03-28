#!/bin/bash

ERROR_LOG="clean_build_error"

echo "Starting cleanup of previous deployment..."

echo "Stopping httpd service..."
sudo systemctl stop httpd 2>>"$ERROR_LOG"


SPRING_PID=$(pgrep -f 'java -jar app.jar')
if [ ! -z "$SPRING_PID" ]; then
  echo "Stopping the current Spring Boot process..."
  kill "$SPRING_PID" 2>>"$ERROR_LOG"
else
  echo "No running Spring Boot process found."
fi

# stop and remove the old container if it exists
if [ "$(docker ps -q -f name=my_app_container)" ]; then
  echo "Stopping the old container..."
  docker stop my_app_container 2>>"$ERROR_LOG"
fi

if [ "$(docker ps -aq -f name=my_app_container)" ]; then
  echo "Removing the old container..."
  docker rm my_app_container 2>>"$ERROR_LOG"
fi

# remove the old image if it exists
if [ "$(docker images -q my_app:latest)" ]; then
  echo "Removing the old Docker image..."
  docker rmi -f my_app:latest 2>>"$ERROR_LOG"
fi

# clean dangling resources
echo "Pruning unused Docker resources..."
docker system prune -f 2>>"$ERROR_LOG"
# Start httpd after cleanup
echo "Starting httpd service..."
sudo systemctl start httpd 2>>"$ERROR_LOG"
echo "Cleanup completed!"