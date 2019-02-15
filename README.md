# Lagom Openshift Smoke Tests

This is a minimal Lagom application used to demo and smoke-test deployment on Openshift.

### Components

1. a `hello` service with a single endpoint responding with an echo
1. the `hello` service is setup to require 3 nodes and build an Akka Cluster
1. a `hello-proxy` service with a single endpoint forwarding the request to `hello` service

The above will test:

* cluster bootstrap to startup `hello`
* service discovery for intra-service (using kubernetes-api)
* service discovery for inter-service (using DNS)

**NOTE**: this minimal sample avoids using any DB or broker to reduce the resources required to run.

## Sample license

Written in 2019 by Lightbend, Inc.

To the extent possible under law, the author(s) have dedicated all copyright and related
and neighboring rights to this template to the public domain worldwide.
This template is distributed without any warranty. See <http://creativecommons.org/publicdomain/zero/1.0/>.