FROM openjdk:17
WORKDIR /app
COPY ./target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
