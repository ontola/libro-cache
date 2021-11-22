FROM openjdk:16-jdk as builder

RUN mkdir /app
WORKDIR /app
COPY . /app
RUN /app/gradlew --no-daemon stage

FROM openjdk:16-jdk

EXPOSE 8080:8080

RUN mkdir /app
COPY --from=builder /app/build/install/cache/ /app/
WORKDIR /app/bin

CMD ["./cache"]
