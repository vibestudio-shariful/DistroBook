#!/bin/bash
# Rebuild Gradle Wrapper Script
# This script deletes any corrupt/existing gradle-wrapper.jar and regenerates it.

set -e

echo "=== Deleting existing gradle-wrapper.jar ==="
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    rm -f gradle/wrapper/gradle-wrapper.jar
    echo "Deleted existing gradle/wrapper/gradle-wrapper.jar"
else
    echo "No existing gradle-wrapper.jar found."
fi

echo "=== Regenerating Gradle Wrapper ==="
gradle wrapper

echo "=== Verification ==="
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "Successfully regenerated gradle-wrapper.jar!"
    ls -lh gradle/wrapper/gradle-wrapper.jar
else
    echo "ERROR: gradle-wrapper.jar was not generated."
    exit 1
fi
