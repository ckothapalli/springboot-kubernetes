FROM adoptopenjdk/openjdk11:alpine-jre
RUN apk update && apk upgrade && apk add bash coreutils
ADD target/springboot-example.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]