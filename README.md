# BSX

BSX is a programming language inspired by the joke programming language *BS* created by [@markrendle](https://github.com/markrendle).
BSX does not claim to be a correct implementation of BS, however it tries to be as close to BS as possible.
The main goal is to provide the same developer experience as BS.

### The language

BSX is based on [this talk](https://www.youtube.com/watch?v=vcFBwt1nu2U), it should give you a good introduction to the language.

[![The Worst Programming Language Ever - Mark Rendle - NDC Oslo 2021](https://img.youtube.com/vi/vcFBwt1nu2U/0.jpg)](https://www.youtube.com/watch?v=vcFBwt1nu2U)

However, there are a few notable differences.

  * As BSX compiles to Java bytecode, you can use Java classes in BSX. A java class is denoted in source code by its internal name with all slashes (`/`) replaced with a dot (`.`) and all dollar signs (`$`) replaced with two colons (`::`).
  * Instance properties must be declared explicitly in order to be accessible with `€this->name`;
  * `HALT_AND_CATCH_FIRE` is not a keyword but rather a global method. That means it must be written with a semicolon.
  * `#!` starts a comment, but only at the beginning of the first line. This is to allow shebangs in bs files.

### How it works

BSX is written in Java. It takes BSX source code and compiles it to java bytecode before executing it.
It should be noted, that decompiling the generated java bytecode won't work as it makes heavy use of constant dynamics and invoke dynamics which can't be used from the java language directly.

### Example

*Taken from the above talk:*

```bs
#define /^my (.*?) thing:$/class \1:/

my Greeter thing:

  €name;

  public function __construct(€name)
	  HALT_AND_CATCH_FIRE;
	(unless €name != null);
	€this->name = €name;
	Delete €name;

  public function say(€thing isProbablyA String, €times)
	42 echo €thing, « », €this->name, BS::EOL;
	  goto 42;
	(unless --€times !!=! 0);
	Delete €thing, €times;

€greeter = new Greeter(«world»);
€greeter->say(«Hello», 10);
Delete €greeter;
```
