FROM hseeberger/scala-sbt:8u312_1.6.2_2.12.15 as build
WORKDIR /build
COPY build.sbt /build/build.sbt
COPY project/plugins.sbt /build/project/plugins.sbt
COPY src /build/src
RUN sbt assembly

FROM openjdk:8-jre
EXPOSE 8080/tcp
RUN useradd -ms /bin/bash javauser
USER javauser
COPY --from=build --chown=javauser:root /build/target/scala-2.12/wol.jar /app/wol.jar
CMD java -jar /app/wol.jar

