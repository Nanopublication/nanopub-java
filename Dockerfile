FROM eclipse-temurin:23-alpine

WORKDIR /app

COPY target/nanopub-*-jar-with-dependencies.jar bin/
RUN find bin/ -name "nanopub-*-jar-with-dependencies.jar" -exec mv {} bin/np.jar \;

ENTRYPOINT ["java", "-jar", "/app/bin/np.jar"]
CMD ["help"]
