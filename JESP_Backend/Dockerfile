FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests package dependency:copy-dependencies -Dmdep.outputDirectory=target/dependency

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/target/classes ./target/classes
COPY --from=build /workspace/target/dependency ./target/dependency

EXPOSE 5000 5001

ENTRYPOINT ["java", "-cp", "target/classes:target/dependency/*", "jesp.JespBackendMain"]
