# Database

## Overview

Flexblock uses a flexible database system, and can work with almost
any SQL database. Although Flexblock is currently only configured to
use PostgreSQL, this functionality can be easily expanded by adding
the appropriate JDBC driver to the dependencies list in `project.clj`,
and updating the environment configuration to use that driver.

## Table Setup

Flexblock currently uses three tables: `users`, `rooms`, and
`users_rooms`. The details of these tables are explained below. If you
have a database that does not have these tables, Flexblock will
automatically create them for you, so you don't need to worry about
setting up tables.

## Configuration

The database connection is configured entirely by the system described
in [Environment](environment.md). Below an example `config.edn`
for a PostgreSQL database running on the local machine.

```edn
{:database
 {:connection
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname     "//localhost:5432/my_db"
   :user        "user"
   :password    "password"}
  :seed-user
  {:name     "The Administrator"
   :email    "admin@school.edu"
   :password "password"
   :admin    true
   :teacher  false}}}
```

If it is more convenient, such as when running on Heroku, you can
instead provide a `:jdbc-database-url`

```edn
{:jdbc-database-url "MY_JDBC_URL"
 :database
  :seed-user
  {:name     "The Administrator"
   :email    "admin@school.edu"
   :password "password"
   :admin    true
   :teacher  false}}
```


### Connection

`:connection` should contain a map that constitutes a valid
[JDBC](https://github.com/clojure/java.jdbc) connection. If no
connection is provided, an in-memory H2 database will automatically be
run. However, this is only suitable for development and testing, as
entries to the H2 database are reset every time `flexblock` is run.

### Seed User

`:seed-user` should contain a USERS entry, as shown above. If this
user does not already exist in the database, it will be added on
startup. If there is no seed user in the config, a user will be
created for you, with the email `example@example.com`, and the
password `password`. As with the default H2 database, this is only
suitable for development and testing, and should not be relied upon in
production.
