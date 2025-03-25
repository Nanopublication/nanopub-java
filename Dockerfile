FROM eclipse-temurin:23-alpine

RUN apk add --no-cache curl
RUN apk add --no-cache bash
RUN apk add --no-cache maven
RUN apk add --no-cache git

WORKDIR /app

COPY . .

RUN mvn clean install
RUN cp target/*.jar bin/

ENV PATH="/app/bin/:$PATH"

# Download the jar using np a first time
RUN /app/bin/np help

ENTRYPOINT [ "np" ]
CMD [ "help" ]
