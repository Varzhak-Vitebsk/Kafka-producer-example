ARG RUNTIME_IMAGE=openjdk:18-alpine

FROM ${RUNTIME_IMAGE}
ARG PACKAGE_NAME=order-service
COPY .entrypoint.sh /tmp/
RUN tr -d '\r' < /tmp/.entrypoint.sh > /entrypoint.sh && chmod 0755 /entrypoint.sh
COPY target/${PACKAGE_NAME}.jar /app/
ENV PACKAGE_NAME ${PACKAGE_NAME}

ENTRYPOINT ["/entrypoint.sh"]
