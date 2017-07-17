.. role:: versailles(code)
    :language: versailles
.. default-role:: versailles
       
===========================
Versailles Standard Library
===========================

Types
=====

`Boolean`

`String`

`Number`

`Type`

`Janus_class`

Values
======

`true: Boolean`

`false: Boolean`

`if: {A:: Boolean -> Type} --> (cond: Boolean) --> (then: () -> A(true)) --> (else: () -> A(false)) --> A(cond)`

`\`+\`: Number -> Number <-> Number`

	Addition
	
`\`-\`: Number -> Number <-> Number`

	Subtraction

`\`*\`: Number -> Number <-> Number`

	Multiplication
	
`\`/\`: Number -> Number <-> Number`

	Division
	
`div: Number -> Number -> Number`

	Integer division rounded towards negative infinity.	

`mod: Number -> Number -> Number`

	Integer modulo. `mod(a, b)` returns `a - div(a, b) * b`.	

`muladd: Number -> {Number, Number} >-> Number`

	`muladd k (a, b)` returns `a * k + b`
	
`divmod: Number -> Number <-< {Number, Number}`
 
    `divmod k c` returns `(div(c, k), mod(c,  k))`
