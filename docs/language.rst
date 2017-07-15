.. role:: versailles(code)
    :language: versailles
.. default-role:: versailles
       
=============================
Versailles Language Reference
=============================

Expressions
===========

Built-in Types
--------------

Boolean
^^^^^^^

The type for truth values. It has two possible values: `true` and `false`.

Number
^^^^^^

Decimal integers: `0`, `1`, `42`, `-127`.

Hexadecimal integers: `0xdeadbeef`.

Binary integers: `0b1100101`.

Decimal floating point: `3.14`, `1.2p10`.

Hexadecimal floating point: `0x3.243F6`, `0x1.2p10`.

Binary floating point: `0b11.00100`, `0b1.101p42`.

Explicit Decimal: `0d1234`, `0d3.14p14`.

Underscores in the middle of numbers are allowed to group digits.

String
^^^^^^

`"Text"`

Escape sequence are [TODO].

.. seealso::
    
    :ref:`interpolated_text`
    
.. _variables:
    
Variables
---------

Variable names consist of a letter followed by any number of letters, digits and
underscores. Versailles does not have reserved words like other languages.
Its syntax is such that words like `let` or `def` that are used elsewhere in
the language can always be differentiated from variables with such names.

Every variable has a scope -- the portion of the source code where that
variable is accessible. The scope usually starts with the expression where
the variable appears first and ends at the end of the enclosing function or file.
The value of a variable cannot change during its scope.

The scope of two variables with the same name may overlap. This can be achieved 
by prefixing `?` (question mark) to the beginning and end of the scope of 
the inner variable. For example::
    
    let sum = 1 + 2;
    let ?sum = 10 + 20;
    let y = ?sum * 3;
    return (sum, y);
    
This returns `(3, 90)`. Any outer variable is inaccessible as long as the
inner variable is visible. 

This feature can also be used, to simulate a variable that changes its value.
The `?`\s are very important in that case::

    let x = 1;
    let x = ?x * 2;
    let x = magic_function(?x, 42);  

You can also define a variable and immediately close its scope. This is done
by giving it the special name `_` (underscore). This is sometimes useful when you get a 
value that you don't need and don't want to give it a proper name. 

Variable names that start with an upper case letter are typically used for 
types. The reason is that Versailles' syntax for types does not allow to 
easily use variables that start with lower case letter.

There is also a form for variables that allows to use any character. For 
example, you could define are variable with the plus sign as its name like so:
`\`+\``. Any sequence of characters is allowed between the backticks. 
This is also a way to access lower-cased variables in types.

.. `` # fixes editor syntax highlighting

Binary Operators
----------------

.. list-table::

    * - Operator
      - Associativity
      - Argument Type
      - Result Type
    * - `=>`, `->`, `-->`, `<->`, `>->`, `<-<`, `>-<`, `<>->`,
        `<-<>`, `<>-<>`
      - right
      - any
      - any
    * - `==>`, `implies`
      - right
      - `(Boolean, Boolean)`
      - `Boolean`
    * - `<=>`, `iff`
      - none
      - `(Boolean, Boolean)`
      - `Boolean`
    * - `||`, `or`
      - right
      - `(Boolean, Boolean)`
      - `Boolean`
    * - `&&`, `and`
      - right
      - `(Boolean, Boolean)`
      - `Boolean`
    * - `==`, `!=`, `<=`, `>=`‚ `<`‚ `>`
      - none
      - `(A, A)`
      - `Boolean`
    * - `++`
      - right
      - `(A, A)`
      - `A`
    * - `+`, `-`
      - right
      - `(Number, Number)`
      - `Number`
    * - `*`, `/`, `div`, `mod`
      - right
      - `(Number, Number)`
      - `Number`
    * - `asserting`
      - none
      - `(A, Boolean)`
      - `A`
    * - `:`
      - none
      - `(A, Type)`
      - `A`
      

Unary Operators
---------------

.. list-table::

    * - Operator 
      - Position
      - Argument Type
      - Result Type
    * - `!`
      - prefix
      - `Boolean`
      - `Boolean`
    * - `-`
      - prefix
      - `Number`
      - `Number`
    * - `~`
      - prefix
      - `A >-j-> B`
      - `B <-j-< A`

Tuple Types (short form)
------------------------

A tuple is an ordered set of values. Tuples are written using parenthesis and 
commas. For example `(1, "abc")` is a pair of numbers containing the number `1` as
its first component and the string `"abc"` as its second component. A tuple can contain
any number of components, even zero. The components also can have different data
types. They can even be tuples again.

Tuples that contain only one component must have an extra comma to differentiate
them from simple parenthesis. For example `(1)` is just the number `1`,
but `(1,)` is the tuple that contains the number one. Additional commas can 
be inserted anywhere in a tuple if you feel the need.

Tuple components can be given names. For example `(x = 1, y = 2, z = 3)` has
three components named `x`, `y` and `z`. Named and unnamed components 
can be mixed, but the unnamed components must always be in front of the named
components.

Tuple components can be accessed in two ways. First, the `.`-operator can be
used to retrieve one of its components, either by its name (if it has one) or 
by its position (starting from zero). For example, let `t = (1, "abc", b = true)`‚
then `t(1)` returns `"abc"` and `t.b` returns `true`. Named components
can of course also be accessed by their position. So, in the example `t(2)` is
equivalent to `t.b`. 

The second way to access tuple components is with a pattern matching. So, for
example `let (a, b, c) = t;` would assign the three components of `t` to
the variables `a`, `b` and `c`. [TODO:named]
The pattern must match exactly the number of components that the tuple has or
the match fails. 
 
A tuple type defines the types for each component. For example, `{Integer, String}` 
is describes pairs of integers and strings. A tuple type may also describe
the names of its components. For example, `{x: Integer, y: Integer, z: Integer}` 
is a tuple type with three integer components with the names `x`, `y` and `z`.

The singleton tuple type is written `{A,}`. Curly braces serve the same
grouping purpose for types as parenthesis do for values. So, if the comma is ommitted 
as in `{A}` the whole expression stand just for the type `A`.

The empty tuple type is `Unit` (defined as `tuple { pass }`, see next 
section) which is sometimes useful. Its only value is the empty tuple `()`.

Tuples (long form)
-----------------------

Tuples and tuple types also have a more verbose form with more features. For example,
the tuple type `{x: Integer, y: Integer, z: Integer}` can also be written as::

    tuple {
        def x: Integer;
        def y: Integer;
        def z: Integer;
    }
    
The long form for the tuple `(x = 1, y = 2, z = 3)` is::

    {
        def x: Integer = 1;
        def y: Integer = 2;
        def z: Integer = 3;
    }
    
This form allows

* to document components using the :ref:`def-statement-values`
* computed members using the definite form of :ref:`def-statement-values`
* function members using :ref:`def-statement-functions`
* type components using :ref:`type-statement`
* local definititions using the :ref:`let-statement` 
* :ref:`if-statement` which may not depend on runtime values

Functions
---------

Functions are usually not written in the form explain in this section. Most 
functions are defined by using the :ref:`def-statement-functions`. You can
skip this section and still be able to write any program.

A function expression (or 'anonymous function' or 'lambda expression') is 
written `a -> b` where `a` and `b` can be any expression. `a` is called
the function's *pattern* and `b` is called its *body*. New variables
that appear in `a` will be assigned values that can then be used in `b`.
Variables that are used in `b` must of course have been defined earlier -- 
either in `a` or in the outer scope.

.. note::

    If you want to define a variable in a function's pattern with the same name 
    of a variable that is already defined, you have to prefix its name with `?`.
    See :ref:`variables`.

A function type describes the types of a function's input and output values.
A function type for functions that map values of type `A` to values of type
`B` is written `A -> B`. Even though the syntax of function types looks the
same here as that of function expressions, it is not. Since `A` and `B` are
types only type expression may appear in these places. So, variables that start
with lower case letters cannot be used (directly), for example.  

Case-Expressions
----------------

A function can be defined by multiple cases that are tried in order. The first
matching case determines the function result. The following function, for example,
converts booleans to strings::

    {
        case true => "true";
        case false => "false";
    }
    
Of course it is possible to have more complex patterns. The following example
implements the fast exponentiation function::

    let fastexp = {
        case (0, _)         => 1;
        case (n * 2, x)     => { 
            let xn = fastexp(n, x); 
            return xn * xn; 
        };
        case (n * 2 + 1, x) => { 
            let x2 = fastexp(n, x); 
            return xn * xn * x; 
        };
    };  

The `.`-operator can be used to immediately apply a case-expression to a
value. This is equivalent to pattern matching expressions in other languages::

    parse("123").{
        case Nothing => 0;
        case Some(n) => n;
    }

We use `=>` here, but any of the function or janus arrows may be used instead.
`=>` tries to guess which type of function or janus you are defining by choosing
the most restrictive arrow that still type checks. But you can always be specific
and give the arrow that you want. 

Januses (Reversible Functions)
------------------------------

A janus is a function that can be run in reverse. Reversible functions cannot
be dependently typed. A janus type replaces `->` with one of the following
symbols. 

`<>-<>` Generic Janus
    
    A generic janus, `f: A <>-<> B` has a reverse `~f: B <>-<> A` and that's
    it. Every janus is also a function, and so is its reverse.

`>->` Semi-inverse Janus

    If `f(x)` is defined then `~f(f(x)) == x`.
    
`<-<` Cosemi-inverse Janus
    
    If `~f(x)` is defined then `f(~f(x)) == x`.
    
`<->` Inverse Janus

    `f` is semi-inverse and cosemi-inverse.
    
`<>->` Semi-pseudoinverse Janus
    
    If `f(x)` is defined then `f(~f(f(x)) == x`.
    
`<-<>` Cosemi-pseudoinverse Janus
    
    If `~f(x)` is defined then `~f(f(~f(x)) == x`.
    
`>-<` Pseudoinverse Janus

   `f` is semi-pseudoinverse and cosemi-pseudoinverse.
   
A janus is really two functions. It has additional constraints. Of course every
function that is called inside a janus must be a janus. Otherwise, we cannot
hope to construct a reverse. There are also restrictions on how variables
are used, which are a bit unintuitive. Every variable must be used at least once.
Also, for some types like functions, variables of those types must be used 
exactly once.

This comes from the way the reverse of a janus is derived. The reverse of 
`a <>-<> b` is `b <>-<> a`. And since every variable must be defined before
it is used, `b` must contain the same variables as `a`, otherwise the reverse
is ill-defined. We call the variables that are define in the context of a janus
*linear*.

There is one exception, though, and this is where it gets unintuitive. In a
janus application like `f(x)`, `f[x]` or `x.f` linear variables that do
not appear linearly in `x` may appear non-linearly in `f`. For example,
the built-in function for addition is `\`+\`: Number -> Number <-> Number`.
We can write a function that returns the sum and difference of its arguments
in the following way::

    def symsum(?a: Number, ?b: Number): Number = {
        let ?sum = `+`(a)(?b);
        let ?diff = (~`+`(sum))(`*`(2)(?a));
        return (?sum, ?diff);
    };

The scopes of the linear variables have been explicitly marked with `?` to
make it clear where the places are that they are used linearly. `a` and `sum`
are also used non-linearly in the middle. `a` is used to construct the janus
`\`+\`(a)` which is then applied to `?b`. `b` is consumed and transformed 
into `sum`‚ but `a` is not consumed. It is still available afterwards and must
be consumed by some expression. 
   
Dependently Typed Functions
---------------------------
   
A dependent function type is written with an extended arrow `-->`. In this 
case, the argument is given as a tuple expression: `(x: A) --> B(x)`.
This allows the result type of the function to depend on the actual value of 
the argument. The argument type is then the type of the argument expression. 
Otherwise `A -> B` is truly just an abbreviation of `(_: A) --> B` where 
the actual argument cannot not appear in `B`.

Januses cannot have a dependent type.

There is no difference between the function expressions `a --> b` and `a -> b`.
Function expressions don't need a special syntax to be dependently typed.

Lists
-----

List are written `[1, 2, 3]`. Lists are similar to tuples, except that all
components have to have the same type and that the list type does not distinguish
between lists of different length. The empty list is written `[]`.

There is a special notation for ranges, for example `[2 .. 5] == [2, 3, 4]`
and `[5 .. 2] == [5, 4, 3]` and `[2 .. 2] == []`.

There is also a special list application `f[1, 2, 3]` that returns a new list
where the function is applied to each element of the list, so `[f(1), f(2), f(3)]`.

List comprehensions are like `[f(x) for x from list]`.

The list type is defined by the standard library as::

    type List{A} = algebraic {
        variant nil;
        variant cons(head: A, tail: List{A});
    };
    
and `[1, 2, 3]` is just syntactic sugar for `cons(1, cons(2, cons(3, nil)))`.

Dictionaries
------------

Dictionaries are lists of key value pairs, written like 
`["fst" = 1, "snd" = 2, "thd" = 3]`.  

Dictionary comprehensions are like `[name(x) = value(x) for x from list]`.

Algebraic Data Types
--------------------

.. _interpolated_text:

Interpolated Text
-----------------

If-Expressions
--------------

Asserting-Expressions
---------------------

Block Expressions
-----------------

Statements
==========

.. _let-statement:

Let-Statements
--------------

.. _def-statement-values:

Def-Statements for Values
-------------------------

.. _def-statement-functions:

Def-Statements for Functions
----------------------------

`def f(x: A)(y: B): C = stuff;` is short for 
`def f: (x: A) --> (y: B) --> C = (x: A) -> (y: B) -> stuff;`.

`def f(x: A)(y: B) <->: C = stuff;` is short for 
`def f: (x: A) --> B <-> C = (x: A) -> (y: B) <-> stuff;`.

`def f(x: A)(y: B) <-> (z: C) { stuff; };` is short for 
`def f: (x: A) --> B <-> C = (x: A) --> (y: B) <-> { stuff; return (z: C); };`.

`def f(x: A)(y: B) <-> g(z: C) { stuff; };` is short for 
`def f: (x: A) --> B <-> C = (x: A) --> (y: B) <-> { stuff; return (z: C); };
def g: (x: A) --> C <-> B = ~f;`.


`def f(x: A)(y: B): C;` is short for 
`def f: (x: A) --> (y: B) --> C;`.

`def f(x: A)(y: B) <->: C;` is short for 
`def f: (x: A) --> B <-> C;`.

`def f(x: A)(y: B) <-> (z: C);` is short for 
`def f: (x: A) --> B <-> C = (x: A) --> (y: B) <-> C;`.

`def f(x: A)(y: B) <-> g(z: C);` is short for 
`def f: (x: A) --> B <-> C; def g: (x: A) --> C <-> B = (x: A) -> ~f(x);`.

.. _type-statement:

Type-Statements
---------------

Like `def` but the expression after `=` is a type expression.

For example::

    type Vector3 = {x: Number, y: Number, z: Number}; 
    
is just short for::

    def Vector3: Type = {x: Number, y: Number, z: Number};
    
`type` allows to define functions returning types, similar to `def`::    
    
    type Id{A} = A;
    type List{A} = algebraic {
        variant Nil;
        variant Cons: (A, List{A});
    };
    type NList(n: Number){A} = (n.{
        case 0     => algebraic { variant Nil; };
        case n + 1 => algebraic { variant Cons: (A, NList(n){A}); };
    });

.. _if-statement:

If-Statements
-------------

Loop-Statements
---------------

Return-Statements
-----------------

Returns ends the current block specifying its value. If a block has no 
`return`-statement, a `return ()` is implied.

Yield-Statements
----------------

Module-Statements
-----------------

