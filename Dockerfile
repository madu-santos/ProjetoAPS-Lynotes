FROM maven:3.9.10-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn -Dmaven.test.skip=true package dependency:copy-dependencies -DincludeScope=runtime

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./dependency

EXPOSE 8080

CMD ["java", "-cp", "classes:dependency/*", "br.ufpb.dcx.projetos.App"]
