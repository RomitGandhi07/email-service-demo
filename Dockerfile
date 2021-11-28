FROM openjdk:8-alpine

COPY target/uberjar/email-service.jar /email-service/app.jar

EXPOSE 3001

CMD ["java", "-jar", "/email-service/app.jar"]
