## Overview

Flexblock uses [cprop](https://github.com/tolitius/cprop) to manage
environment variables. This library merges specified EDN files,
environment variables, and java arguments into a map that can be
accessed by the application. For additional information about how the
environment system works, see cprop's
[README](https://github.com/tolitius/cprop/blob/master/README.md).

## Example

This is an example `config.edn` file that shows how database
connection info might be stored.

```edn
{:db
 {:connection
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname     "//localhost:5432/my_db"
   :user        "user"
   :password    "password"}}}
```

As you can see, configuration is stored in a nested associative data
structure, similar to Clojure maps or JSON. EDN is actually a subset
of the Clojure programming language.

However, if providing a `config.edn` file is not feasible, the same
nested structure can be represented in plain UNIX environment
variables, using double underscore `__` as a nesting character. For
example, the above data structure might look like this in bash.

```bash
export DB__CONNECTION__CLASSNAME="org.postgresql.Driver"
export DB__CONNECTION__SUBPROTOCOL="postgresql"
export DB__CONNECTION__SUBNAME="//localhost:5432/my_db"
export DB__CONNECTION__USER="user"
export DB__CONNECTION__PASSWORD="password"
```

## Development

During development, either a `dev-config.edn` file can be used to
store configuration. This file should **NOT** be checked in to git, as
it will contain sensitive information, such as database connections.
