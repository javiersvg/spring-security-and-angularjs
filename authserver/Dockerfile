FROM anapsix/alpine-java:8

ADD target/authserver*.jar /app.jar
RUN bash -c 'touch /app.jar'
EXPOSE 9999
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]