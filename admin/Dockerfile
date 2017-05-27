FROM anapsix/alpine-java:8

ADD target/admin*.jar /app.jar
RUN bash -c 'touch /app.jar'
EXPOSE 8082
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]