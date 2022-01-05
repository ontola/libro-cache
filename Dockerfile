FROM openjdk:16-jdk as builder

RUN mkdir /app
WORKDIR /app

COPY ./gradle /app/gradle
COPY ./gradlew /app/
COPY ./build.gradle.kts /app/
COPY ./gradle.properties /app/
COPY ./settings.gradle.kts /app/
RUN ./gradlew --no-daemon clean

COPY . /app
RUN /app/gradlew --no-daemon stage

# Keep in sync with graal_version in gradle.properties
FROM ghcr.io/graalvm/graalvm-ce:21.3

EXPOSE 3080:3080

RUN mkdir /app
WORKDIR /app

COPY --from=builder /app/build/libs/cache-*all.jar /app/cache.jar

CMD ["java", "-jar", "cache.jar"]
