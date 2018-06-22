# Components

## What are Compn

Flexblock uses [Mount](https://github.com/tolitius/mount) to manage
application state. These states are used to manage external resources,
such as an HTTP server, or a Database. States that manage these
external resources are called (in Flexblock, not in Mount)
components.

## Structure of a Component

Components contain two functions: a `:start` function, responsible for
initializing the component, and optionally a `:stop` function that
cleans up the state. Note that some components do not have stop
functions. Components look like this:

``` clojure
(mount/defstate
  "A docstring that describes how the component works."
  :start (start-function)
  :stop (stop-function))
```

For more information on components, see Mount's
[documentation](https://github.com/tolitius/mount).
