# Building native images

## Prerequisites

* GraalVM
* `native-image` via graalvm updater `gu`
* "build-essentials", i.e. gcc and friends
* static libraries zlib and libc (glibc for arm64)

## Tracing Agent

See [GraalVM docs: Assisted Configuration with Tracing Agent](https://www.graalvm.org/22.0/reference-manual/native-image/Agent/)

### Motivation

`native-image` has some limitations on what it can discover at build-time. For example, it can discover some reflection usage in your code, but not all. Also loading resources from classpath won't work at run-time as resource are not included by default. That's where the Tracing Agent comes in and helps to discover these edge cases. The Tracing Agent is used while running the unit tests, therefore it is very important to cover as much of the critical code paths as possible.

### Access Filters configuration

During unit test execution the Tracing Agent will record more critical behavior than necessary. For example, JUnit itself makes heavy use of reflections, however, JUnit is not part of the final distribution. Therefore, the Tracing Agent needs to be instructed to ignore some discoveries.

This is done in file `agent-confg/access-filter.json`, which includes Patterns of classes to exclude from discovery.

### Usage

The Tracing Agent is used during unit test execution. In file `build.gradle` it is added to the `test` task like so:

```
    jvmArgs "-agentlib:native-image-agent=access-filter-file=native-image/agent-config/access-filter.json,config-output-dir=native-image/config-classpath/META-INF/native-image"
```

* Add `native-image-agent` as `-agentlib` to the JVM used for test execution.
* Set option `access-filter-file` to point to the aforementioned configuration.
* Set option `config-output-dir` to point to the directory where the Tracing Agent will save its discoveries.
* The configurations created by the agent are automatically picked up by `native-image` it they are on the classpath.
* _NOTE: Unit tests **must** run in GraalVM!_

## Native Image

### Prerequisites

* Tracing Agent recorded hints for native image creation (see previous chapter).
* Gradle task `installDist` prepared the jars needed for distribution as classic java application (in folder `build/install/...`)

### Native Image build

```sh
$ native-image \
        --allow-incomplete-classpath \
        --no-fallback \
        --static \
        --libc=glibc \
        --class-path './config-classpath/:../build/install/app/lib/*' \
        com.github.lbarnkow.minchir.test.App \
        app
```
