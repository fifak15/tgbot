FROM azul/zulu-openjdk-alpine:8u302 as mvn-build
WORKDIR /bot-src
COPY . .
RUN ./mvnw -v
RUN ./mvnw package -DskipTests

FROM azul/zulu-openjdk-alpine:8u302-8.56.0.21-jre-headless
RUN apk add --no-cache openssl curl jq bash
COPY --from=mvn-build /api-src/target/telegram.bot-0.0.1-SNAPSHOT.jar /app/telegram.bot-0.0.1-SNAPSHOT.jar
CMD java -Xmx2g -jar /app/telegram.bot-0.0.1-SNAPSHOT.jar
