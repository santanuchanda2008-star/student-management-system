FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN javac -cp ".:lib/ojdbc8.jar" WebServer.java DatabaseConnection.java

EXPOSE 8081

CMD ["java", "-cp", ".:lib/ojdbc8.jar", "WebServer"]
