.. role:: versailles(code)
    :language: versailles
.. default-role:: versailles
       
===================
Versailles Tutorial
===================

Introduction
============

This text teaches you the basics of Versailles. We assume some prior experience
with other programming languages.

Versailles is a statically typed language. This means that before a Versailles
program is executed a first pass is made that assigns each expression
a data type. Types are for example `Integer` or `String`. During this pass it is 
checked that there a no nonsensical expressions like adding an integer and string.

Versailles is also a pure functional language. This essentially means that
the values of variables cannot be changed. More generally speaking functions
cannot have side effects. So the result of every function call is
solely determined by its arguments.

First Steps
===========

TODO: how to start the repl.

Versailles makes a clear distinction between *expressions* and *statements* [#fexprstmt]_.
The repl expects statements by default. In the beginning we will focus on 
expressions, but one very useful statement is the `let`-statement. The 
`let`-statement allows you define variables. For example::

    > let pi = 3.14159265359
    
allows you to later use the variable `pi` in place of the more verbose number.

Versailles does not have reserved words. It is absolutely possible to define 
a variable called `let` via::

    > let let = "let"

Versaille's syntax is crafted in such a way that keywords (like `let`) 
never conflict with variable names. This ensures that extensions to the 
language will never make your programs invalid due to the addition of new
reserved words. But unfortunately it also means that expressions and 
statements cannot be mixed as they are many other languages.

In order to tell the repl to treat the input as an expression, we begin the
line with an equals sign (`=`) [#fequstmt]_. So, for example to see the
value of `pi / 2` you type::

    > = pi / 2
    1.5707963268   

.. rubric:: Footnotes

.. [#fexprstmt] There are other syntactical categories (types, janus classes and
                tuple components) which we will get into later.
.. [#fequstmt] The `=` sign here is actually a special statement that simply
               prints its single argument. The `=` statement only exists in
               the repl. 

Simple Types
============

Numbers
-------

One of the most basic things to do with a computer is arithmetic. For example
type `= 1 + 2` and press Enter. You will see::

    > = 1 + 2
    3

Other operators are `-`, `*`, `/` and `mod` for subtraction, multiplication,
division and remainder. The operators bind as they do in math: `*`, `/` and `mod` 
bind stronger than `+` and `-`. Parenthesis can be used to make the binding 
explicit. So `= 1 + 2 * 3` is the same as `= 1 + (2 * 3)` and results in `7`, but 
`= (1 + 2) * 3` would be `9`. Of course, `-` can also be used as unary 
operator. Other functions are called by name, like `sqrt(2)` or `sin(0.5)`.
The parenthesis in such function calls are required.

Versailles also supports writing numbers in different bases by prefixing the 
numbers with zero and one of the letters x, o, b or d:

* Hexadecimal: `0x1234567890ABCDEF`
* Octal: `0o12345670`
* Binary: `0b1010`
* Decimal: `0d1234567890`

You can sprinkle in some underscores to make large numbers more readable::

    > = 123_456_789
    123456789
    
Real numbers can be written using the dot (`.`) as decimal separator as seen 
before. Versailles also support scientific notation::
 
    > let g = 6.67408e-11
    
Note that in order to support scientific notation for hexadecimal we 
cannot use `e` for the exponent. Thus numbers that use a base prefix must
use `p` instead. For example, here is (an approximation of) the same number 
in hexadecimal::

    > let g = 0x6.AC908p-11
    
.. note::

    The default type of numbers in Versailles are so called 
    `computable numbers <https://en.wikipedia.org/wiki/Computable_number>`_
    which have infinite precision. The advantage of using these numbers is that
    you never have to worry about overflows, underflows or rounding errors. The
    disadvantage is a performance impact and the inability to compare to numbers
    for equality. We will later learn how to use other types of numbers.   

Strings
-------

A string is written using quotation marks (`"`). Characters can be escaped with
a backslash as in many other languages. For example, the string containing a 
single quotation mark is written `"\""`.

An extended form called *string interpolation* is written using apostrophes 
(`'`). Inside such strings the dollar sign (`$`) has a special meaning.
The dollar sign encloses expressions which are not constant strings but are
computed into string. For example::

    > let name = "Bob"
    > let age = 42
    > = 'Hello $name$ of age $age$!'
    "Hello Bob of age 42!" 

TODO: string functions, concatenation (++), substring, search

Booleans
--------

The basic truth values are called `true` and `false`. They are produced
for example by the comparison operators `==`, `!=`, `>=`, `<=`, `>` 
and `<` which return `true` if and only if the two operands are respectively
equal, unequal, greater or equal, less or equal, greater, or less. For combining
booleans we have the usual operators `&&`, `||` and `!` for logical and, 
or and not.

The `if`-statement lets us make decisions based on a boolean expression::

    > let x = 42
    > if x mod 2 == 0 then
          let result = "even"
      else
          let result = "odd"
    > = 'x is $result$'
    "x is even"
    
It takes a boolean as condition and if the condition is `true` the statement following 
`then` will be executed. Otherwise the statement following `else` will be 
executed. The `else`-part can be omitted, in which case the the empty statement 
(`pass`) will be executed if the condition is `false`. So

.. code::

    if is_even(x) then
      = "It's even"
      
is equivalent to 

.. code::
  
    if is_even(x) then
      = "It's even"
    else
      pass
      
The keen reader might wonder what happens when some variables are only defined
in one branch. The answer is that only variables that are defined in **both** 
branches are available after the `if`-statement. So the following will 
result in an error::  

    > x = 42
    > if x mod 2 == 0 then
        let result = "even"
    > = 'x is $result$'
    Error: Unknown variable `result`.
    
TODO: Check actual error message
    
.. _tuples:    
    
Tuples
------

We can write pairs of numbers with a tuple expression like `(3, 4)`. Tuple 
expressions can have zero or more components. Singleton tuples like `(3,)` must
add an extra comma to differentiate it from simple parenthesis. We can also give names 
to the components, like `(x = 3, y = 4, z = 5)`.

Tuple components can be accessed via the dot-operator. If `t` is some tuple then
its first component is `t.0`, it's second component `t.1` and so on. Named
components can also be accessed by their name, like `t.x`.

Another way of accessing tuple components is by using the `let`-statement::

    > let (a, b) = (3, 4)
    
for example simultaneously defines two variables `a` and `b` with values `3` and
`4` respectively. This general idea is more broadly explained in 
:ref:`patternmatching`.

There is also a notation that unpacks a tuple inside another tuple::

    > let pos = (x = 1, y = 2)
    > let size = (w = 10, h = 20)
    > = (pos, size)
    ((x = 1, y = 2), (w = 10, h = 20))
    > = (*pos, *size)
    (x = 1, y = 2, w = 10, h = 20)
    
So when we prefix a tuple component with `*` it has to be a tuple itself. Its
components are then inserted into the tuple at that position in their respective
order and with their respective names.

Lists
-----

Tuples usually have a fixed number of components. To store a
variable number of values we use lists.

Lists are written using square brackets, like `[1, 2, 3]` or `[]` for the empty
list. Similar to tuples, there is also a notation for expanding lists::

    > let x = [1]
    > let x2 = [x, 2, *x]
    > = x2
    [1, 2, 1]
    
Accessing elements of a list uses parenthesis, same as function calls::

    > = x2(0)
    3
    > = x2(1)
    2
    
But we can also use a square brackets to access multiple elements at the same time::

    > = x2[0, 2]
    [3, 1]
    > = x2[2, 1, 0]
    [1, 2, 3]
    > = x2[]
    []
    
There is a special notation for defining ranges of numbers. `[1..5]` is equivalent 
to `[1, 2, 3, 4]` -- the second number is not included in the list. This range 
notation can be combined with the simple list notation and is especially useful
for accessing a sublist of a list. For example::

	> = "1234567890"[0..2, 3, 5, 7..10]
	"1246890"
	    
TODO: useful list functions (`range`, `++`, `flatten`).

Duplicate::

    > = [42][0, 0, 0, 0]
    [42, 42, 42, 42]
    > = [4, 2] * 4
    [4, 2, 4, 2, 4, 2, 4, 2]
    
From mathematics we know set-comprehensions. Versailles also has list-comprehensions::

    > [n * n for n from [1..10] where is_prime(n)] // squares of prime numbers less than 10
    [4, 9, 25, 49]
    
Dictionaries
------------

A dictionary is a data structure that associates a number of keys with a number
of entries. Dictionaries are written similar to lists except that each entry
is a key and a value separated by an equals sign (`=`)::

    > let d1 = ["yes" = 1]
    > let d2 = ["no" = 0, "maybe" = 2]
    > = d1("yes")
    1
    > = [*d1, *d2]["maybe", "no", "yes"]
    [2, 1, 0]
    > = [v = 'he says $k$' for k = v in [*d2, *d1]]
    [0 = "he says no", 2 = "he says maybe", 1 = "he says yes"]
    
Dictionary comprehension::

	> = [n.toString = n for n in [1..4]]
	["1" = 1, "2" = 2, "3" = 3]
    
Statements
==========

Pass-statement
--------------

Occasionally we need a statement that does nothing. This is written `pass` in Versailles.

Switch, Try, Reject
-----------------------

The switch statement is the most general way in Versailles to execute different 
statements depending on some conditions. It's written like this::

	switch {
	case a;
	case b;
	...
	case z;
	}
	
where `a`, `b`, ..., `z` can be any statement. This will try to execute `a`,
and if that rejects try `b`, and so on, and reject if everything fails.

How can a statement fail? That's what the `fail` statement is for. It fails 
unconditionally and is thus only rarely useful.

The normal  `let`-expression will result in an error if the pattern cannot 
be matched against the value. Therefore we cannot us it in a `switch`-statement.

Using the `try`-statement we can turn those errors into match failures.
So instead of failing completely a failure to match the pattern would backtrack 
and try the next `case` in a surrounding `switch`.

If-statement
------------



Block-expressions
-----------------

Statements allow you to define new variables, but how do you use them in an expression?
That what block statements are for. A block statement is basically a sequence of
statements with a expression that give the overall value and but can also use all the 
variables defined by the statements. 
    
Functions
=========
    
Defining Simple Functions
-------------------------

.. code::

    > def double(x: Number): Number = x + x
    > = double(double(11.5))
    42

As you can see we usually annotate the type of the arguments and the result 
with a colon (`:`). But result types can usually be inferred automatically
by the type checker. So it is OK to omit them (unless the type checker complains)::

    > def double(x: Number) = x + x
    
There is also a syntax that lets you assign the result as a variable::

    > def double(x: Number) => (*y: Number) {
          let y = x + x
      }

The meaning of the `*` is as explained in :ref:`tuples`. If it wasn't 
there the function would return a singleton-tuple and would technically not
be the same the previous definition.

Reversible Functions
====================

Now ... this is the part Versailles was invented for. Take a very simple 
arithmetic function like::

    > def add3(x: Number) = x + 3
    
We would like to know if `5` was the the result of `add3(x)`, what was `x`?
Well, thanks to general education the answer is pretty easy: `2`.

Now, let's phrase this problem a little differently. Let's introduce a new 
operator `~` that computes the *inverse* of a function. The problem is:
what is the value of `x` after::

    > let x = add3~(5)
    
In this reading `add3~` is a new function with the special property that for 
every `x` `add3~(add3(x)) == x` and for every `y` `add3(add3~(y)) == y`.
    
The way Versailles solves how to find `add3~` is by using the idea that if a function
only uses reversible functions to compute its result, the function itself is
also reversible. 

In this case `x + 3` actually is interpreted as the multiple
function call `add(3)(x)` to the built-in higher-order function `add`. `add`
itself is irreversible, but for any number it returns a reversible function. So,
`add3~` is solved to be `add(3)~`, which is defined to be equal to `subtract(3)`.
And `substract(3)(5)` will give us `2`.

.. _patternmatching:

Pattern Matching
----------------

In Versailles pattern matching has two main ideas. The first is that 
    
.. code::
  
    > let f(y) = x
    
is equivalent to

.. code::

    > let y = f~(x) 
    
For example::

    > let y + 3 = 5
    > = y
    2
    
.. note::
    
    For reversible operators it is always the left operand which can be reversed, 
    i.e., `y + 3` is really short for `\`+\`(3)(y)` -- the operands get reversed.
    And so `let y + 3 = 5` is short for `let \`+\`(3)(y) = 5` which is equivalent 
    to `let y = \`+\`(3)~(5)` which is equivalent to `let y = 5 - 3`.
      
The second idea is that such a pattern may fail and in this case you can define
another pattern to try. The following defines the fast exponentiation function
using two patterns - one for even numbers and one for odd numbers::

    > def fastexp(x: Integer, e: Integer) =
          e.{
          case n * 2 => { let tmp = fastexp(x, n); return tmp * tmp }
          case n * 2 + 1 => { let tmp = fastexp(x, n); return tmp * tmp * x }
          }
        
Cases expression::    

    > def fastexp = {
      case (x, n * 2) => { let tmp = fastexp(x, n); return tmp * tmp }
      case (x, n * 2 + 1) => { let tmp = fastexp(x, n); return tmp * tmp * x }
      }
        
More general: `switch`-statement::

    > def fastexp(x: Integer, e: Integer) => (y: Integer) {
          switch {
              case {
                  try n * 2 = e
                  let tmp = fastexp(x, n)
                  let y = tmp * tmp
              }
              case {
                  let n * 2 + 1 = e
                  let tmp = fastexp(x, n)
                  let y = tmp * tmp * x
              }
          }
      }
      
Generalized `if`-statement::

    > if { try 2 * n = e } then {
          let tmp = fastexp(x, n)
          let y = tmp * tmp
      } else {
          let 2 * n + 1 = e
          let tmp = fastexp(x, n)
          let y = tmp * tmp * x
      }
      
      
Basics
======

Variables, let, def, type, tuples, String, Number, Boolean

Pattern Matching
================

switch, if, short-let, cases

Lambda Expressions and Reversible Functions
===========================================

lambda, generic januses, linear variables

Advanced Types
==============

algebraic, dependent function types

