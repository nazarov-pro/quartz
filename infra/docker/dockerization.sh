#!/bin/bash

COMMAND=""

if [ "$1" = "start" ]; then
   COMMAND="up"
elif [ "$1" = "stop" ]; then
   COMMAND="down"
elif [ "$1" = "logs" ]; then
   COMMAND="logs"
elif [ "$1" = "clean" ]; then
   COMMAND="down -v"
else
  echo "Only start(add -d to run it in the background)|stop|logs(add -f to follow lines)|clean are available commands"
  exit 100
fi

CURRENT_DIR="$(pwd)"

cd "$(dirname "$0")" || exit

echo "Current dir changed from $CURRENT_DIR to $(pwd)"
echo "All containers will $COMMAND"

DOCKER_COMPOSE_FILES=$(ls ./*docker-compose.yaml)
DOCKER_COMPOSE_FILES_CMD=""
while IFS= read -r SINGLE_FILE; do
    echo "+ $SINGLE_FILE"
    DOCKER_COMPOSE_FILES_CMD="$DOCKER_COMPOSE_FILES_CMD -f $SINGLE_FILE"
done <<< "$DOCKER_COMPOSE_FILES"

docker-compose $DOCKER_COMPOSE_FILES_CMD $COMMAND ${@:2}
