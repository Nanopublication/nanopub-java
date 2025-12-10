FROM eclipse-temurin:23-alpine AS jre-build

ARG VERSION

RUN apk add --no-cache curl
RUN apk add --no-cache bash
RUN apk add --no-cache git

WORKDIR /app

COPY . .

RUN ./mvnw && ./mvnw versions:set -DnewVersion=${VERSION} && ./mvnw package -Dmaven.test.skip=true
RUN cp target/nanopub-${VERSION}-*.jar bin/

ENV PATH="/app/bin/:$PATH"

# Download the jar using np a first time
RUN /app/bin/np help

ENTRYPOINT [ "np" ]
CMD [ "help" ]
