#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "Missing $JAR"
  echo "Copy gradle-wrapper.jar from STELLARPILOT/android/gradle/wrapper/ first."
  exit 1
fi
JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$JAVA_CMD" -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
