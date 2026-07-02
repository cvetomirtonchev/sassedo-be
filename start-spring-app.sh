#!/bin/bash

# Determine OS and set LOG_DIR accordingly
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    LOG_DIR="/home/nineteen/nineteen/logs"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    LOG_DIR="/Users/youruser/nineteen/logs"
else
    LOG_DIR="/default/log/directory"
fi

# Navigate to the working directory
cd /home/nineteen/nineteen/target

# Find the latest JAR file
JAR_FILE=$(ls -t *.jar | head -n 1)

# Check if the JAR file exists
if [ -z "$JAR_FILE" ]; then
  echo "No JAR files found in /home/nineteen/nineteen/target"
  exit 1
fi

# Start the JAR file with the log-path system property
echo "Starting $JAR_FILE with log-path=$LOG_DIR..."
exec java -Dfile.encoding=UTF-8 -Dlog-path="$LOG_DIR" -jar "$JAR_FILE"
