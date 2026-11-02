#!/usr/bin/env sh
# Gradle wrapper launcher
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" -jar "$CLASSPATH" "$@"
