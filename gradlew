#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVACMD=${JAVA_HOME:+$JAVA_HOME/bin/}java
if ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo "Java was not found. Set JAVA_HOME or add java to PATH." >&2
    exit 1
fi
exec "$JAVACMD" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    "-Dgradle.wrapper.apphome=$APP_HOME" org.gradle.wrapper.GradleWrapperMain "$@"
