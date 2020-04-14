FROM openjdk:8-jre
# FROM maven:3-jdk-8

WORKDIR /app

## The Dockerfile download the latest jar release
# It could also be used to build the jar file

## Re-download dependencies only if change to pom.xml
# COPY ./pom.xml ./pom.xml
# RUN mvn dependency:go-offline -B

COPY ./src ./src
COPY ./bin ./bin
COPY ./scripts ./scripts

## Build  jar
# RUN mvn clean install
# RUN cp target/*.jar ??

# Put np in the $PATH
RUN cp bin/np /bin/np

# Download the jar using np a first time
RUN np help

ENTRYPOINT [ "np" ]
CMD [ "help" ]