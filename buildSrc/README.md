# Infra Cloud Build

This folder contains the custom plugins and conventions for the Infra build.
They are declared in the `build.gradle` file in this folder.

## Build Conventions

The `infra.cloud.conventions` plugin applies all conventions to the Framework build:

* Configuring the Java compiler, see `JavaConventions`
* Configuring testing in the build with `TestConventions` 


## Build Plugins

### Optional dependencies

The `infra.cloud.optional-dependencies` plugin creates a new `optional`
Gradle configuration - it adds the dependencies to the project's compile and runtime classpath
but doesn't affect the classpath of dependent projects.
This plugin does not provide a `provided` configuration, as the native `compileOnly` and `testCompileOnly`
configurations are preferred.

### RuntimeHints Java Agent

The `today-core-test` project module contributes the `RuntimeHintsAgent` Java agent.

The `RuntimeHintsAgentPlugin` Gradle plugin creates a dedicated `"runtimeHintsTest"` test task for each project.
This task will detect and execute [tests tagged](https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle)
with the `"RuntimeHintsTests"` [JUnit tag](https://junit.org/junit5/docs/current/user-guide/#running-tests-tags).
In the Infra test suite, those are usually annotated with the `@EnabledIfRuntimeHintsAgent` annotation.

By default, the agent will instrument all classes located in the `"cn.taketoday"` package, as they are loaded.
The `RuntimeHintsAgentExtension` allows to customize this using a DSL:

```groovy
// this applies the `RuntimeHintsAgentPlugin` to the project
plugins {
  id 'infra.cloud.runtimehints-agent'
}

// You can configure the agent to include and exclude packages from the instrumentation process.
runtimeHintsAgent {
  includedPackages = ["cn.taketoday", "io.spring"]
  excludedPackages = ["org.example"]
}

dependencies {
  // to use the test infrastructure, the project should also depend on the "today-core-test" module
  testImplementation(project(":today-core-test"))
}
```

With this configuration, `./gradlew runtimeHintsTest` will run all tests instrumented by this java agent.
The global `./gradlew check` task depends on `runtimeHintsTest`.            

NOTE: the "today-core-test" module doesn't shade "today-core" by design, so the agent should never instrument
code that doesn't have "today-core" on its classpath.