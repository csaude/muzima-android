#!/bin/bash

OLD_PACKAGE="mz.org.csaude.muzimamobile"
NEW_PACKAGE="mz.org.csaude.muzimamobile"
SRC_DIR="./app/src/main/java"

echo "Updating package declarations..."
grep -rl "$OLD_PACKAGE" "$SRC_DIR" | xargs sed -i "s|$OLD_PACKAGE|$NEW_PACKAGE|g"

echo "Renaming directory structure..."
OLD_DIR="${SRC_DIR}/mz/org/csaude/emuzima"
NEW_DIR="${SRC_DIR}/mz/org/csaude/muzimamobile"

if [ -d "$OLD_DIR" ]; then
  mkdir -p "$(dirname "$NEW_DIR")"
  mv "$OLD_DIR" "$NEW_DIR"
  echo "Moved $OLD_DIR to $NEW_DIR"
else
  echo "Directory $OLD_DIR not found!"
fi

echo "Done. Please sync Gradle and rebuild your project."

