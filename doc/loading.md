# Loading Messages

## Overview

Flexblock uses (arguably) witty loading messages to not only improve the user experience while waiting for the page to load, but also because adding to these messages is easy, and makes a great first contribution! The conde changes to add a new loading message are extremely simple (you just need to add your new message to a list of strings), so that you can focus on the contributing process rather than complex code. This guide will walk you through the steps of adding a new loading message. This guide assumes you have read [CONTRIBUTING.md](CONTRIBUTING.md), and have created your own fork of flexblock. This guide only covers the changes in the code.

## Adding a New Message

The loading messages are stored in the file `src/clj/flexblock/views/loading.clj`. Inside there is a var definition that looks something like this:

```clojure
(def messages
  ["A fun loading message!"
   "Another fun loading message!"
   ,,,])
```

You can add your loading message by simply adding it to the list. Make sure to keep the list in alphabetical order!

```clojure
(def messages
  ["A fun loading message!"
   "Another fun loading message!"
   "My brand new super awesome loading message!"
   ,,,])
```
