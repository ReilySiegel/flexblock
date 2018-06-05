# Documentation

Welcome to the Flexblock Documentation! This is the best place to
learn about how Flexblock works! If you are looking for something
specific, see the table of contents below. Otherwise, read on!

## Table of Contents

- [Learn Clojure](clojure.md)

## Getting Started With Flexblock's Documentation

### Comments and Docstrings

If you are looking for specific details about the implementation of a
certain function, you probably won't find it here. You should look in
the [source code](/src). Below is an example function, showing how
documentation might be formatted in code. For more information on
clojure, see [Learn Clojure](clojure.md)

``` clojure
(defn add-two-numbers
  "This is a Docstring. Docstings come before the
  argument vecor of a function, and gives an overview of how it
  works. This particular function adds `x` and `y`."
  [x y]
  ;; This is a comment. Comments start with one or more semicolons.
  ;; Comments usually describe a specific detail of what happens on
  ;; the line below the comment.

  ;; Here is where we do the adding.
  (+ x y))
  ```

----
