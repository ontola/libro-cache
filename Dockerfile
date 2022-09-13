FROM eclipse-temurin:17-jdk as builder

WORKDIR /app

COPY ./gradle /app/gradle
COPY ./gradlew /app/
COPY ./build.gradle.kts /app/
COPY ./gradle.properties /app/
COPY ./settings.gradle.kts /app/
# RUN ./gradlew --no-daemon clean

COPY . /app
RUN /app/gradlew --no-daemon stage

FROM eclipse-temurin:17-jre

EXPOSE 3080:3080

WORKDIR /app

COPY --from=builder /app/build/libs/libro-*all.jar /app/libro.jar
COPY ./server_version.txt /app/server_version.txt

CMD ["java", "-jar", "libro.jar"]
