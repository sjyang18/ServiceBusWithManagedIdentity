FROM maven:3.5-jdk-8 as BUILD
 
COPY src /usr/src/myapp/src
COPY pom.xml /usr/src/myapp
RUN mvn -f /usr/src/myapp/pom.xml clean package -DskipTests

FROM openjdk:8
ENV SB_QUEUENAME queue1
ENV SB_NAMESPACE seyan-dev2sb2
WORKDIR /app/
COPY --from=BUILD /usr/src/myapp/target/*-jar-with-dependencies.jar /app/app.jar
CMD java -XX:+PrintFlagsFinal -XX:+PrintGCDetails $JAVA_OPTIONS -jar app.jar

