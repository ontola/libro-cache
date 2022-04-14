# Cache Readme

**Generated docs: https://ontola.gitlab.io/cache/index.html**

This server is the Cache + Front-end server for Argu.
It serves as the entry point, hosts the web application, serves cached resources, and queries Apex when needed.

_This project is likely to be renamed to `libro-server` in the near future, as it no longer serves as just a cache_.

## Setup

- `Apex` and other Ontola services must be running. See [`core`](https://gitlab.com/ontola/core/).
- Make sure you have a `.env` from [`core`](https://gitlab.com/ontola/core/) symlinked to this directory.
- See [resources/application.conf] for configuration options
- Run `./gradlew`
