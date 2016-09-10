# Spock JDBC extensions

JDBC related extensions for [Spock](http://spockframework.org).

[![Build Status](https://travis-ci.org/robfletcher/spock-jdbc.svg?branch=master)](https://travis-ci.org/robfletcher/spock-jdbc)
[![Bintray](https://img.shields.io/bintray/v/robfletcher/maven/spock-jdbc.svg?maxAge=2592000)](https://github.com/robfletcher/spock-jdbc)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/robfletcher/spock-jdbc/master/LICENSE)

## Installation

```groovy
dependencies {
  testCompile "co.freeside:spock-jdbc:1.0.0"
}
```

## Automatic cleanup of test data

If you have tests that insert data to a database it's important to ensure the data is cleaned up between tests.
Instead of having to write a `cleanup` method in each specification class you can use the `@TruncateTables` annotation.

The annotation can be applied to a `java.sql.Connection`, `javax.sql.DataSource` or `groovy.sql.Sql` property of the specification.
In the cleanup phase the extension will use the annotated field to connect to the database and simply delete everything from all tables.

For example you could use the annotation in a Spring integration test with a dependency-injected `DataSource` instance like this:

```groovy
@TruncateTables @Autowired DataSource dataSource
```

### Foreign key constraints

The extension will analyze foreign key constraints on the tables it finds and delete data in an order that will not cause constraint violation exceptions.

### Using a custom connection source

If you want to use `@TruncateTables` with something other than a `Connection`, `DataSource` or `Sql` field you can write an implementation of `Connector` or `TypedConnector` and specify it on the annotation.

For example, if you were using [JDBI](http://jdbi.org/) and have a `org.skife.jdbi.v2.DBI` field you could annotate the field with:

```groovy
@TruncateTables(DBIConnector) DBI dbi
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

### Connection state

The connection will be closed after data is deleted.

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
