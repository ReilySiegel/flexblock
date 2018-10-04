# Learn Clojure

## Table of Contents

- [What is Clojure?](#what-is-clojure)
- [Clojure Syntax](#clojure-syntax)
- [Immutable Data Structures](#immutable-data-structures)

## What is Clojure?

[Clojure](https://clojure.org/) is a programming language. However,
it's syntax may be a bit foreign to someone who is new to programming,
or used to programming in languages like C, Python, or Java. If you
want to read a comprehensive, but humorous introduction to Clojure,
see [Clojure for the Brave and
True](https://www.braveclojure.com/introduction/). If you would prefer
reference style docs, see the ClojureDocs
[Quickref](https://clojuredocs.org/quickref). I will attempt to give
you a crash course, but these resources are probably much better than
what I could write.

## Clojure Syntax

This section will compare C code to Clojure code, as Ellington High
School's AP Computer Science Principles course teaches C. As this code
is written specifically for use at Ellington High School, we hope that
students of this class will consider contributing to
Flexblock. However, for contributors who are not taking this course,
we will try not to assume any specific prior knowledge.

Clojure is written using prefix notation. Here is a comparison of C
and Clojure.

``` C
// In C
printf("Hello, world!");
```

``` clojure
;; In Clojure
(printf "Hello, world!")
```

OMG! The function is **INSIDE** the parenthesis. Clojure follows the
following basic structure:

``` clojure
(function argument1 argument2 argument3 ...)
```

Unlike in C, Clojure's syntax is very uniform. Every function in
Clojure uses the same basic structure, shown above. The are no
exceptions. See the following example, adding 1 and 1.

``` C
// In C
1 + 1
```

``` clojure
;; In Clojure
(+ 1 1)
```

C code broke their convention of `function(arg1 arg2 ...);` here. That
is because in C, `+` is an operator, not a function. In Clojure,
everything is (or acts like) a function. No exceptions. For example,
here is how conditional logic works:

``` C
// In C
if (x == 1) {
	printf("Hello, world!");
} else {
	printf("Goodbye, world!);
}
```

``` clojure
;; In Clojure
(if (= x 1)
  (printf "Hello, world!")
  (printf "Goodbye, world!"))
```

Notice that in Clojure, if is (acts like) a simple function. It takes
three arguments: the predicate (in this case `(= x 1)`), the form to
evaluate if the predicate returns true, `(printf "Hello, world!")`,
and the form to evaluate if the predicate returns false, `(printf
"Goodbye, world!")`. However, we can simplify this Clojure code a
little bit. Because if returns the evaluated form, we can simply
write:

``` clojure
;; In Clojure
(printf (if (= x 1) "Hello, world!" "Goodbye, world!"))
```

Let me walk you through the evaluation (assuming x does equal 1).



First, `(= x 1)` is evaluated to `true`.

``` clojure
(printf (if true "Hello, world!" "Goodbye, world!"))
```

Then, because the predicate returned true, `if` evaluates and returns
the true branch, in this case `"Hello, world!"`

``` clojure
(printf "Hello, World!")
```

And then `printf` is evaluated as normal, and prints "Hello, world!".

## Immutable Data Structures

Clojure uses immutable data structures. That means that things like
lists, arrays, sets, and maps cannot be mutated (changed) in
place. Clojure's functions for manipulating data structures return new
copies, leaving the old copy unchanged.

``` clojure
(let [lista [1 2 3]
	  listb (conj lista 4)]
  (println "List A:" lista)
  (println "List B:" lisb))

;; => "List A: [1 2 3]"
;; => "List B: [1 2 3 4]"
```

Notice that the value of `lista` has not changed. Calling `conj`
(conjoin) on `lista` produces a brand new list, which is then bound to
`listb`.

Also notice that commas are not needed to separate elements. Commas
are considered whitespace in Clojure. You can use them if you want,
but don't need to.

``` clojure
(= [1 2 3] [1, 2, 3] [1,2,3])
;; => true
```
