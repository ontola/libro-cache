FROM openjdk:16-jdk as builder

RUN mkdir /app
WORKDIR /app

COPY ./gradle /app/gradle
COPY ./gradlew /app/
COPY ./build.gradle.kts /app/
COPY ./gradle.properties /app/
COPY ./settings.gradle /app/
RUN ./gradlew --no-daemon clean

COPY . /app
RUN /app/gradlew --no-daemon stage

FROM ghcr.io/graalvm/graalvm-ce:latest

EXPOSE 3080:3080

RUN mkdir /app
WORKDIR /app

COPY --from=builder /app/build/install/cache/ /app/

CMD ["./bin/cache"]
