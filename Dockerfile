FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/flexblock.jar /flexblock/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/flexblock/app.jar"]
