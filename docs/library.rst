===========================
Versailles Standard Library
===========================

Types
=====

``Boolean``

``String``

``Number``

``Type``

Values
======

``true: Boolean``

``false: Boolean``

``if: {A:: Boolean -> Type} --> (cond: Boolean) --> (then: () -> A(true)) --> (else: () -> A(false)) --> A(cond)``

:literal:`\`+\`: Number -> Number <-> Number`

	Addition
	
:literal:`\`-\`: Number -> Number <-> Number`

	Subtraction

:literal:`\`*\`: Number -> Number <-> Number`

	Multiplication
	
:literal:`\`/\`: Number -> Number <-> Number`

	Division
	
:literal:`\`//\`: {Number, Number} -> Number`

	Integer division rounded towards negative infinity.	

``mod: {Number, Number} -> Number``

	Integer modulo. ``mod(a, b)`` returns ``a - a // b * b``.	

``muladd: Number -> {Number, Number} >-> Number``

	``muladd k (a, b)`` returns ``a * k + b``
	
``divmod: Number -> Number <-< {Number, Number}``
 
    ``divmod k c`` returns ``(c // k, mod(c,  k))``
