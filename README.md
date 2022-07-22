# Cache Readme

**Generated docs: https://ontola.gitlab.io/cache/index.html**

This server is the Cache + Front-end server for Argu.
It serves as the entry point, hosts the web application, serves cached resources, and queries Apex when needed.

_This project is likely to be renamed to `libro-server` in the near future, as it no longer serves as just a cache_.

## Quickstart
- Open the project in [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
- Copy the environment file `cp .env.template .env`
- Start the run configuration (libro > Tasks > application > run) in the gradle 
  menu (By default on the right)

Alternatively, use `KTOR_ENV=development ./gradlew run` to start the project 
from the command line.

## Setup with backend

- `Apex` and other Ontola services must be running. See [`core`](https://gitlab.com/ontola/core/).
- Make sure you have a `.env` from [`core`](https://gitlab.com/ontola/core/) symlinked to this directory.
- See [resources/application.conf] for configuration options
- Run `KTOR_ENV=development ./gradlew run`

## Gradle tasks

- `application > run` Starts the server.
- `build > assemble` Builds the project.
- `build > build` Builds the project and runs the tests.
- `build > clean` Removes any build caches.
- `verification > allTests` Runs the test suite.
- `verification > ktlintCheck` Checks for linting issues.
- `formatting > ktlintFormat` Fixes any auto-fixable linting issues.
- `documentation > dokkaHtml, dokkaGhm, etc` Builds the documentation.
- `documentation > dokkaServe` Serves local html version of the docs.
