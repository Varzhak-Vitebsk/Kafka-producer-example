#!/usr/bin/env sh
# shellcheck disable=SC1017 # Literal carriage return on Windows workstations
# shellcheck disable=SC2086 # Double quote to prevent globbing and word splitting in "exec" line

JAVA_OPTS="${JAVA_OPTS} -XX:+UseContainerSupport -XX:InitialRAMPercentage=20 -XX:MaxRAMPercentage=50"

if [ "${JAVA_DEBUG_ENABLED}" = "true" ]; then
    JAVA_OPTS="${JAVA_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
fi

exec java ${JAVA_OPTS} -jar /app/${PACKAGE_NAME}.jar
