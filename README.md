# Spock JDBC extensions

JDBC related extensions for [Spock](http://spockframework.org).

[![Build Status](https://travis-ci.org/robfletcher/spock-jdbc.svg?branch=master)](https://travis-ci.org/robfletcher/spock-jdbc)
[![Bintray](https://img.shields.io/bintray/v/robfletcher/maven/spock-jdbc.svg?maxAge=2592000)](https://github.com/robfletcher/spock-jdbc)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/robfletcher/spock-jdbc/master/LICENSE)

## Installation

```groovy
repositories {
  jcenter()
}

dependencies {
  testCompile "co.freeside:spock-jdbc:1.0.0"
}
```

## Automatic cleanup of test data

If you have tests that insert data to a database it's important to ensure the data is cleaned up between tests.
Unfortunately, writing a `cleanup` method that carefully deletes things from the database in the right order is really boring.
Also, you and I both know you're going to do slightly different things in different tests and 6 months from now something will start leaking and it will take you all morning to figure out why.

Instead you can use the `@TruncateTables` annotation.

The annotation can be applied to a `java.sql.Connection`, `javax.sql.DataSource` or `groovy.sql.Sql` property of the specification.
In the cleanup phase the extension will use the annotated field to connect to the database and simply delete everything from all tables.

For example you could use the annotation in a Spring integration test with a dependency-injected `DataSource` instance like this:

```groovy
@TruncateTables @Autowired DataSource dataSource
```

### Foreign key constraints

The extension will analyze foreign key constraints on the tables it finds and delete data in an order that will not cause constraint violation exceptions.

### Using a custom connection source

If you want to use `@TruncateTables` with something other than a `Connection`, `DataSource` or `Sql` field you can write an implementation of `Connector` or `TypedConnector` and specify it on the annotation's `connector` property.

For example, if you were using [JDBI](http://jdbi.org/) and have a `org.skife.jdbi.v2.DBI` field you could annotate the field with:

```groovy
@TruncateTables(connector = DBIConnector) DBI dbi
```

… and implement a connector like this:

```groovy
static class DBIConnector extends TypedConnector<DBI> {
  DBIConnector() { super(DBI) }

  @Override
  protected Connection apply(DBI source) {
    source.open().connection
  }
}
```

For convenience if the connector is the only thing you need to specify on the annotation you can supply it as the annotation's `value` property:

```groovy
@TruncateTables(DBIConnector) DBI dbi
```

### Connection state

The connection will be closed after data is deleted.

If you're annotating a raw `Connection` field that might not be ideal.
`DataSource` or something like it that can be used to acquire a fresh connection is really the optimal use-case.

### Logging activity

To log what the extension does:

```groovy
@TruncateTables(verbose = true)
```

### Ignoring exceptions

To ignore any exceptions encountered when deleting data:
 
```groovy
@TruncateTables(quiet = true)
```
### Questions

#### Can I make it only delete _some_ data?

Not at the moment.
If you have a set of "baseline" data you will need to re-insert it in your `setup()` method.

#### Is this useful for tests that run in their own transaction like if I'm using Spring Boot's `@DataJpaTest` or a Grails integration test?

Not really.
I have no idea whether the deletion or the transaction rollback would happen first.
Either way would be bad.

#### Shouldn't I just drop the entire database schema and recreate it for each test?

That's also a valid approach.
`@TruncateTables` is for people who don't want to do that for some reason.

#### I have a circular foreign key constraint, will that be a problem?

Yes. 
Yes it will. 
You monster.
