# Module Libro server

This is the technical reference in regard to the Libro server implementation.

# Package tools.empathy.color

This package defines a `Color` class with various calculations.

# Package tools.empathy.rdf.hextuples

Implementation of the [Hextuples](https://github.com/ontola/hextuples) RDF
serialization. Currently not activated in the server.

# Package tools.empathy.studio

This package contains code to host the Studio and resulting distributions.

# Package tools.empathy.url

This package contains extension methods for URLs.

# Package tools.empathy.libro

This package contains various utility functions.

# Package tools.empathy.libro.metadata

This package contains functions to extract metadata for sharing purposes.

# Package tools.empathy.libro.server

This package contains the main entrypoint to run the libro server.

# Package tools.empathy.libro.server.bulk

Ktor plugin to enable fetching of resources via the Bulk API.

# Package tools.empathy.libro.server.bundle

Handles reading Libro client bundles to serve to the browser.

# Package tools.empathy.libro.server.csp

Ktor plugin to manage [Content-Security-Policy](https://developer.mozilla.
org/en-US/docs/Web/HTTP/CSP).

# Package tools.empathy.libro.server.dataproxy

Ktor plugin to set rules for proxying certain requests to other servers.

# Package tools.empathy.libro.server.document

This package contains the logic for building the index page which boots the
Libro client.

# Package tools.empathy.libro.server.health

This package contains code to check the status of various components 
regarding the server and backend.

# Package tools.empathy.libro.server.invalidator

The invalidator boots with the server and listens to a redis stream for
`DeltaEvent` updates of a backend.

# Package tools.empathy.libro.server.landing

Contains the Libro management site.

# Package tools.empathy.libro.server.plugins

This package contains smaller Ktor plugins which don't have their own package.

# Package tools.empathy.libro.server.routes

This package contains smaller routes which don't have their own package.

# Package tools.empathy.libro.server.sessions

Ktor plugin to handle session management and backend (OIDC) authentication.

# Package tools.empathy.libro.server.statuspages

Status pages to display when (backend) errors occur.

# Package tools.empathy.libro.server.tenantization

Ktor plugin to manage tenant information with the connected backend. Also 
allows to configure libro-server only tenants.

# Package tools.empathy.libro.server.util

This package contains various utility functions.

# Package tools.empathy.libro.webmanifest

This package contains the WebManifest with Libro extensions to configure the 
client with.

# Package tools.empathy.model

This package contains implementations and builders of various core data models.

# Package tools.empathy.vocabularies

This package contains the expression of the terms in various namespaces.

# Package tools.empathy.serialization

This package contains the EmpJson serialization format and 
accompanying logic to work with resulting objects.
