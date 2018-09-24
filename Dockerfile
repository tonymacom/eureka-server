FROM java:8-jre

MAINTAINER mabo <itmabo@163.com>

ADD ./target/eureka-server*.jar /app/eureka-server.jar
CMD ["java", "-jar", "/app/eureka-server.jar"]

EXPOSE 8761