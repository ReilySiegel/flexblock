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
automatically create them for you, if your database connection is set
up correctly.

### users

The `users` table stores data about teachers and students. This table
contains the following columns: `id`, `name`, `email`, `passwordhash`,
`teacher`, and `advisor`.

Note that there is currently no way of creating new users from withing
the application. All users must exist in the database through some
other means. This prevents students who do not attend Ellington
schools from using Flexblock.

### rooms

The `rooms` table contains information about the sessions a teacher
creates, and that students will join.

### users_rooms

The `users_rooms` table is a many-to-many wrapper around `users` and
`rooms`. This table tracks what students have joined, left, of have
not signed up for rooms. A row is created whenever a student joins a
room. The row is later deleted if the student leaves that room.

This table only contains three columns: `id`, `users_id`, and
`rooms_id`.

## Configuration

The database connection is configured entirely by the system described
in [environment.md](environment.md). Below an example `config.edn`
for a PostgreSQL database running on the local machine.

```edn
{:db
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

### Connection

`:connection` should contain a map that constitutes a valid
[JDBC](https://github.com/clojure/java.jdbc) connection.

### Seed User

`:seed-user` should contain a USERS entry, as shown above. If this
user does not already exist in the database, it will be added on
startup.
