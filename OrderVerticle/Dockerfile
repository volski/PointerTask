FROM maven:3.6.0-jdk-11-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package
FROM openjdk:11-jre-slim
COPY --from=build /home/app/target/OrderVerticle-1.0-SNAPSHOT-fat.jar /usr/local/lib/OrderVerticle-1.0-SNAPSHOT-fat.jar
# ENTRYPOINT ["java","-jar","/usr/local/lib/RestVerticle-1.0-SNAPSHOT-fat.jar"]
CMD java -jar /usr/local/lib/OrderVerticle-1.0-SNAPSHOT-fat.jar
