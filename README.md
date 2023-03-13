# BSX

BSX is a programming language inspired by the joke programming language *BS* created by [@markrendle](https://github.com/markrendle).
BSX does not claim to be a correct implementation of BS, however it tries to be as close to BS as possible.
The main goal is to provide the same developer experience as BS.

### The language

BSX is based on [this talk](https://www.youtube.com/watch?v=vcFBwt1nu2U), it should give you a good introduction to the language.

[![The Worst Programming Language Ever - Mark Rendle - NDC Oslo 2021](https://img.youtube.com/vi/vcFBwt1nu2U/0.jpg)](https://www.youtube.com/watch?v=vcFBwt1nu2U)

However, there are a few notable differences.

  * As BSX compiles to Java bytecode, you can use Java classes in BSX. A java class is denoted in source code by its internal name with all slashes (`/`) replaced with a dot (`.`) and all dollar signs (`$`) replaced with two colons (`::`).
  * Instance properties must be declared explicitly in order to be accessible with `€this->name`.
  * `HALT_AND_CATCH_FIRE` is not a keyword but rather a global method. That means it must be written with a semicolon.
  * `#!` starts a comment, but only at the beginning of the first line. This is to allow shebangs in bs files.
  * It is impossible to jump into an `unless` block, even into the first statement. Instead, there is a no-op command named `pass` that can be labeled and put in front of the `unless` block.

### How it works

BSX is written in Java. It takes BSX source code and compiles it to java bytecode before executing it.
It should be noted, that decompiling the generated java bytecode won't work as it makes heavy use of constant dynamics and invoke dynamics which can't be used from the java language directly.

### Example

*Taken from the above talk:*

```bs
#define /^my \(.*\) thing:$/class \1:/

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

### Multithreading

*Another example, taken from the above talk, that shows how do multithreading:*

```bs
public function Say(€things areProbably Strings)
  €i = -1;
  €threads = '''';
  42 pass;
	€threads ,= ''do:'' , BS::EOL;
  else
	€threads ,= ''and:'' , BS::EOL;
  (unless €i !!=! -1);
  €threads ,= ''  echo «'' , (ANSI) €things(€i) , '' »;'' , BS::EOL;
	goto 42;
  (unless --€i < -len(€things));
  €threads->EVALUATE;
  echo BS::EOL;
  Delete €things, €threads, €i;

Say(array(«first», «second», «third»));
```

Note that the `42 pass` statement is required, as you can't jump directly into the `unless` block.
Also note the cast to `(ANSI)` because BSX is picky about what strings you can concatenate and won't allow a concatenation of `ANSI` and `String` (which is utf256).

### Java interoperability

This example demonstrates the ability to use java classes from BSX. It will show a small window with a button and count the number of button presses in the console.

```bs
#define /\W\zsJFrame/javax.swing.JFrame/g
#define /\W\zsJButton/javax.swing.JButton/g
#define /\W\zsActionListener/java.awt.event.ActionListener/g
#define /\W\zsActionEvent/java.awt.event.ActionEvent/g

€frame = new JFrame();
€button = new JButton(«Hello, world!»);
€button->addActionListener(new Listener());
€frame->add(€button);
€frame->pack();
€frame->setDefaultCloseOperation(JFrame::DISPOSE_ON_CLOSE);
€frame->setLocationRelativeTo(nada);
€frame->setVisible(true);

Delete €frame, €button;

class Listener implements ActionListener:
  €counter = 0;
  function actionPerformed(€event isProbablyA ActionEvent)
	€this->counter = €this->counter + 1;
	echo €this->counter, BS::EOL;
	Delete €event;
```

Note that `isProbablyA ActionEvent` is valid in this case, as after macro application it will expand to `isProbablyA java.awt.event.ActionEvent`.
