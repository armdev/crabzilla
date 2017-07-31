# crabzilla 

Yet another Event Sourcing experiment

# krabzilla

https://github.com/crabzilla/krabzilla is based on this work but with some parts rewriten in Kotlin and inspired on http://eventstorming.com/

## Status

Currently it's just some code with poor test coverage but the "~~dirty~~ explorations phase" is probably done. I think the future is more predictable: to write tests, documents, refactor, etc 

## Goal for version 1.0.0

To help you write your domain model with very little framework overhead and smoothly deploy it on a state of art Java 8 reactive applications platform backed by a rock solid relational database of your choice.

## How

The approach is to use functions [everywhere](crabzilla-core/src/main/java/crabzilla/stack/AggregateRootFunctionsFactory.java) within your domain. For example:

| Function      | Crabzilla     | [Vavr version](http://www.vavr.io/)  |
| :------------- | :------------- | :----- |
| State transition | [CustomerStateTransitionFn](crabzilla-example1/crabzilla-example1-core/src/main/java/crabzilla/example1/aggregates/customer/CustomerStateTransitionFn.java)| [CustomerStateTransitionFnJavaslang](crabzilla-example1/crabzilla-example1-core/src/main/java/crabzilla/example1/aggregates/customer/CustomerStateTransitionFnJavaslang.java)  |
| Command handling | [CustomerCmdHandlerFn](crabzilla-example1/crabzilla-example1-core/src/main/java/crabzilla/example1/aggregates/customer/CustomerCmdHandlerFn.java)  | [CustomerCmdHandlerFnJavaslang](crabzilla-example1/crabzilla-example1-core/src/main/java/crabzilla/example1/aggregates/customer/CustomerCmdHandlerFnJavaslang.java)|



Ideally your domain model code will be built of immutable data and plain functions so in the end it will be very testable, side effect free and with minimal dependencies. Then you need to add some configuration for both aggregate root [level](crabzilla-vertx-example1/src/main/java/crabzilla/example1/aggregates/CustomerModule.java) and bounded context  [level](crabzilla-vertx-example1/src/main/java/crabzilla/example1/Example1Module.java) to be able to deploy your domain model into a reactive engine built with [Vertx](http://vertx.io/). This engine provides verticles and components for the full CQRS / Events Sourcing lifecycle. 

## How to run the example

1. Clone [crabzilla-dependencies](https://github.com/crabzilla/crabzilla-dependencies) and build it:

```bash
git clone https://github.com/crabzilla/crabzilla-dependencies
cd crabzilla-dependencies
mvn clean install 
```

2. Clone Crabzilla and build it running unit tests but skipping integration tests:

```bash
git clone https://github.com/crabzilla/crabzilla
cd crabzilla
mvn clean install -DskipITs=true
```

2. Start a MySql instance. You can use docker-compose:

```bash
docker-compose up
```

3. Create the database schema using [Flyway](https://flywaydb.org/):

```bash
cd crabzilla-example1/crabzilla-example1-database
mvn compile flyway:migrate
```

4. Optionally, you may want to regenerate classes reflecting the database using [JOOQ](https://www.jooq.org/)

```bash
mvn clean compile -P jooq
```

5. Now you can run integration tests against database, skipping the unit tests:

```bash
# go back to crabzilla root
cd ../..
mvn verify -DskipUTs=true 
```

6. Now you finally can run the current [example](crabzilla-vertx-example1/src/main/java/crabzilla/example1/Example1Launcher.java):

```bash
java -jar crabzilla-vertx-example1/target/crabzilla-vertx-example1-1.0-SNAPSHOT-fat.jar 
```

## Wiki 

You can find more info on [wiki](https://github.com/crabzilla/crabzilla/wiki)
