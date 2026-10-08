FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY generator/pom.xml generator/pom.xml
COPY generator/src generator/src
COPY sentiment-consumer/pom.xml sentiment-consumer/pom.xml
COPY sentiment-consumer/src sentiment-consumer/src
COPY support-consumer/pom.xml support-consumer/pom.xml
COPY support-consumer/src support-consumer/src
ARG MODULE_NAME
RUN mvn -B -pl ${MODULE_NAME} -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG MODULE_NAME
COPY --from=build /workspace/${MODULE_NAME}/target/${MODULE_NAME}-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
