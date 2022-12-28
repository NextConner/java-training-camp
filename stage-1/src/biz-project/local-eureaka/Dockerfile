FROM openjdk:8u312-jre-slim-buster

WORKDIR /data/cycube/
COPY  /target/local-eureka-0.0.1-SNAPSHOT.jar /data/cycube/

EXPOSE 12345

ENV APP_PORT=12345
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx256m -Xss256k -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+UseCGroupMemoryLimitForHeap -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV EXT_ARG="-Dserver.port=12345 "

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENTRYPOINT java  $JAVA_OPTS $EXT_ARG -Dserver.port=$APP_PORT -jar /data/cycube/local-eureka-0.0.1-SNAPSHOT.jar