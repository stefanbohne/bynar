.. Versailles documentation master file, created by
   sphinx-quickstart on Wed Jul 12 19:30:04 2017.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Welcome to Versailles's documentation!
======================================

.. toctree::
   :maxdepth: 2
   :caption: Contents:

Types
=====

Built-in Types
--------------

Boolean
^^^^^^^

The type for truth values. It has two possible values: ``true`` and ``false``.

Number
^^^^^^

Decimal integers: ``0``, ``1``, ``42``, ``-127``.

Hexadecimal integers: ``0xdeadbeef``.

Binary integers: ``0b1100101``.

Decimal floating point: ``3.14``, ``1.2p10``.

Hexadecimal floating point: ``0x3.243F6``, ``0x1.2p10``.

Binary floating point: ``0b11.00100``, ``0b1.101p42``.

Explicit Decimal: ``0d1234``, ``0d3.14p14``.

Underscores in the middle of numbers are allowed to group digits.

String
^^^^^^

``"Text"``

Escape sequence are tbd.

.. seealso::
	
	:ref:`interpolated_text`


Tuple Types (short form)
------------------------

A tuple is an ordered set of values. Tuples are written using parenthesis and 
commas. For example ``(1, "abc")`` is a pair of numbers containing the number ``1`` as
its first component and the string ``"abc"`` as its second component. A tuple can contain
any number of components, even zero. The components also can have different data
types, even tuples.

Tuples that contain only one component must have an extra comma to differentiate
them from simple parenthesis. For example ``(1)`` is just the number ``1``,
but ``(1,)`` is the tuple that contains the number one. Additional commas can 
be inserted anywhere in a tuple if you feel the need.

Tuple components can be given names. For example ``(x = 1, y = 2, z = 3)`` has
three components named ``x``, ``y`` and ``z``. Named and unnamed components 
can be mixed, but the unnamed components must always be in front of the named
components.

Tuple components can be accessed in two ways. First, the ``.``-operator can be
used to retrieve one of its components, either by its name (if it has one) or 
by its position (starting from zero). For example, let ``t = (1, "abc", b = true)``‚
then ``t.1`` returns ``"abc"`` and ``t.b`` returns ``true``. Named components
can of course also be accessed by their position. So, in the example ``t.2`` is
equivalent to ``t.b``. 

The second way to access tuple components is with a pattern matching. So, for
example ``let (a, b, c) = t;`` would assign the three components of ``t`` to
the variables ``a``, ``b`` and ``c``. [TODO:named]
The pattern must match exactly the number of components that the tuple has or
the match fails. 
 
A tuple type defines the types for each component. For example, ``{Integer, String}`` 
is describes pairs of integers and strings. A tuple type may also describe
the names of its components. For example, ``{x: Integer, y: Integer, z: Integer}`` 
is a tuple type with three integer components with the names ``x``, ``y`` and ``z``.

The singleton tuple tuple is written ``{A,}``. Curly braces serve the same
grouping purpose for types as parenthesis do for values. So, if the comma is ommitted 
as in ``{A}`` the whole expression stand just for the type ``A``.

The empty tuple type is ``Unit`` (defined as ``tuple { pass }``, see next 
section) which is sometimes useful. Its only value is the empty tuple ``()``.

Tuples (long form)
-----------------------

Tuples and tuple types also have a more verbose form with more features. For example,
the tuple type ``{x: Integer, y: Integer, z: Integer}`` can also be written as::

	tuple {
	  def x: Integer;
	  def y: Integer;
	  def z: Integer;
	}
	
The long form for the tuple ``(x = 1, y = 2, z = 3)`` is::

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

A function maps values of one type to values of another type. So, a function 
type describes these two types. ``A -> B``.

The result type of a function may depend on the actual value of the argument.
In this case, the argument is given as a tuple expression: ``(x: A) --> B(x)``.
The argument type is then the type of the argument expression. Note that the
arrow is one minus sign longer. This is necessary to resolve some ambiguities
in the syntax. Otherwise ``A -> B`` is truly just an abbreviation of 
``(x: A) --> B`` where ``x`` cannot not appear in ``B``. 

Januses (Reversible Functions)
------------------------------

A janus is a function that can be run in reverse. Reversible functions cannot
be dependently typed. A janus type replaces ``->`` with one of the following
symbols. 

``<>-<>``
	Generic Janus
	
	A generic janus, ``f: A <>-<> B`` has a reverse ``~f: B <>-<> A`` and that's
	it. Every janus is also a function, and so is its reverse.

``>->``
	Semi-inverse Janus

	If ``f(x)`` is defined then ``~f(f(x)) == x``.
	
``<-<``
	Cosemi-inverse Janus
	
	If ``~f(x)`` is defined then ``f(~f(x)) == x``.
	
``<->``
	Inverse Janus

	``f`` is semi-inverse and cosemi-inverse.
	
``<>->``
	Semi-pseudoinverse Janus
    
    If ``f(x)`` is defined then ``f(~f(f(x)) == x``.
    
``<-<>``
    Cosemi-pseudoinverse Janus
    
    If ``~f(x)`` is defined then ``~f(f(~f(x)) == x``.
	
``>-<``
    Pseudoinverse Janus

   ``f`` is semi-pseudoinverse and cosemi-pseudoinverse.
   
Dependent Functions
-------------------
   
Algebraic Types
---------------

Expressions
===========

Literals
--------

Variables
---------

.. _interpolated_text:

Interpolated Text
-----------------

Value Application
-----------------

Type Application
----------------

If-Expressions
--------------

When-Expressions
----------------

Anonymous Function Literals
---------------------------

Anonymous Case Function Literals
--------------------------------

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

``def f(x: A)(y: B): C = stuff;`` is short for 
``def f: (x: A) --> (y: B) --> C = (x: A) -> (y: B) -> stuff;``.

``def f(x: A)(y: B) <->: C = stuff;`` is short for 
``def f: (x: A) --> B <-> C = (x: A) -> (y: B) <-> stuff;``.

``def f(x: A)(y: B) <-> (z: C) { stuff; };`` is short for 
``def f: (x: A) --> B <-> C = (x: A) --> (y: B) <-> { stuff; return (z: C); };``.

``def f(x: A)(y: B) <-> g(z: C) { stuff; };`` is short for 
``def f: (x: A) --> B <-> C = (x: A) --> (y: B) <-> { stuff; return (z: C); };
def g: (x: A) --> C <-> B = ~f;``.


``def f(x: A)(y: B): C;`` is short for 
``def f: (x: A) --> (y: B) --> C;``.

``def f(x: A)(y: B) <->: C;`` is short for 
``def f: (x: A) --> B <-> C;``.

``def f(x: A)(y: B) <-> (z: C);`` is short for 
``def f: (x: A) --> B <-> C = (x: A) --> (y: B) <-> C;``.

``def f(x: A)(y: B) <-> g(z: C);`` is short for 
``def f: (x: A) --> B <-> C; def g: (x: A) --> C <-> B = (x: A) -> ~f(x);``.

.. _type-statement:

Type-Statements
---------------

.. _if-statement:

If-Statements
-------------

Loop-Statements
---------------

Return-Statements
-----------------

Yield-Statements
----------------

Module-Statements
-----------------

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`
