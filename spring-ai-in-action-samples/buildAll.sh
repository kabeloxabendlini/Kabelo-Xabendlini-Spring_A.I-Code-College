#!/bin/sh

find . -type f -name "gradlew" -print0 | sort -z | xargs -0 -I {} bash -c 'cd "$(dirname {})" && echo ">>> BUILDING : $(pwd)" && ./gradlew test
if [ $? -ne 0 ]; then
  echo ">>> BUILD FAILED. Current directory: $(pwd)"
fi'
