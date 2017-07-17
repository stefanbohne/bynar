.. role:: versailles(code)
    :language: versailles
.. default-role:: versailles
       
===================
Versailles Tutorial
===================

Motivational Example
====================

Hello, world.

.. code::
    
    call write(stdout, "Hello, world!");
    
Greeting.

.. code::

    call write(stdout, "Who TF are you?");
    let name = read(stdin);
    call write(stdout, "Oh, hi there, " ++ name ++ "!");
    
.. code::

    call write(stdout, 'Oh, hi there $name$!')
    
Greeting function.

.. code::

    def greeting(question: String, greeting1: String, greeting2: String): Unit = {
        call write(stdout, question);
        let name = read(stdin);
        call write(stdout, greeting1 ++ name ++ greeting2);
    };
    
    call greeting("Who you be?", "Hi, ", "!");
    
Greeting functional.

.. code::

    def greeting(question: String, greeting: String -> String): Unit = {
        call write(stdout, question);
        let name = read(stdin);
        call write(stdout, greeting(name));
    };
    def mygreeting(name: String): String = 'Hello, $name$!';
    
    call greeting("What's your name?", mygreeting);
    
.. code::

    call greeting("What's your name?", name => 'Hello, $name$!');
        
Biased greeting.

.. code::

    def mygreeting(name: String): String = {
        if name == "stefan" then
            let result = 'OK, $name$.';
        else
            let result = 'Welcome, $name$!';
        return result;
    };

[TODO: Maybe?]

.. code::

    def mygreeting(name: String): String = {
        if name.begins_with("s") then
            return 'OK, $name$.';
        else
            return 'Welcome, $name$!';
    };
    
`name.beginswith(x)` is equivalent to `begins_with(name)(x)` which is 
equivalent to `x.(name.begins_with)`.    

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

