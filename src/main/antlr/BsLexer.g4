lexer grammar BsLexer;
@header { package bsx.compiler.parser.antlr; }

START_BLOCK: [\u0002];
END_BLOCK: [\u0003];

START_INLINE: '{' -> pushMode(DEFAULT_MODE);
END_INLINE: '}' -> popMode;

START_GROUP: '(' -> pushMode(DEFAULT_MODE);
END_GROUP: ')' -> popMode;

COLON: ':';
SEMICOLON: ';';
GREEK_QUESTION_MARK: [\u037E];

// KEYWORDS

THIS: '€this';
TRUE: [Tt][Rr][Uu][Ee];
FALSE: [Ff][Aa][Ll][Ss][Ee];
NULL: 'null';
NOTHING: 'Nothing';
UNDEFINED: 'undefined';
NADA: 'nada';
EMPTY: 'Empty';
NAN: 'NaN';
INFINITY: 'Infinity';
PIE: '🥧';
NEW: 'new';
PARENT: 'parent';
GOTO: 'goto';
UNLESS: 'unless';
ELSE: 'else';
DO: 'do';
AND: 'and';
ECHO: 'echo';
PASS: 'pass';
RETURN: 'return';
DELETE: 'Delete';
ARRAYS_OF: 'arrays of';
ARRAY_OF: 'array of';
PUBLIC: 'public';
PROTECTED: 'protected';
PRIVATE: 'private';
STATIC: 'static';
FINAL: 'final';
READONLY: 'readonly';
FUNCTION: 'function';
CLASS: 'class';
EXTENDS: 'extends';
IS_PROBABLY_A: 'isProbablyA';
IS_PROBABLY_AN: 'isProbablyAn';
ARE_PROBABLY: 'areProbably';

// LITERALS

INTEGER: '-'?[0-9]+;
FLOAT: '-'?[0-9]+('.'[0-9]+)?([eE][0-9]+)?;

fragment UNTERMINATED_ANSI: '\'\'' (~['\\\r\n] | '\\' (. | EOF))*;
ANSI: UNTERMINATED_ANSI '\'\'';

fragment UNTERMINATED_ASCII: '\'' (~['\\\r\n] | '\\' (. | EOF))*;
ASCII: UNTERMINATED_ASCII '\'';

fragment UNTERMINATED_EBCDIC: '""' (~["\\\r\n] | '\\' (. | EOF))*;
EBCDIC: UNTERMINATED_EBCDIC '""';

fragment UNTERMINATED_DBCS: '"' (~["\\\r\n] | '\\' (. | EOF))*;
DBCS: UNTERMINATED_DBCS '"';

START_UTF256_INTERP: '««' -> pushMode(INTERPOLATED_STRING);

fragment UNTERMINATED_UTF256: '«' (~[«»\\\r\n] | '\\' (. | EOF))*;
UTF256: UNTERMINATED_UTF256 '»';

WHITESPACE: [ \t\n] -> skip;

VARIABLE: '€'IDENT;
IDENT: [_\p{Alpha}\p{General_Category=Other_Letter}][_\p{Alnum}\p{General_Category=Other_Letter}]*;

INSTANCE_ACCESS: '->';
STATIC_ACCESS: '::';

CHAINED_OPERATOR: ',=' | '+=' | '-=' | '*=' | '/=' | '%=';
MINUS_OPERATOR: '-';
BANG_OPERATOR: '!';
COMMA: ',';
INFIX_OPERATOR_NO_COMMA_NO_MINUS: '!!=!' | '!=!' | '!=' | '==' | '<=' | '<' | '>=' | '>' | '+' | '*' | '/' | '%'  | '&&'  | '||';

EQUAL: '=';
INLINE_PLUS: '++';
INLINE_MINUS: '--';

DOT: '.';

mode INTERPOLATED_STRING;

INTERPOLATED_STRING_TEXT: (~[«»\\\r\n$] | '\\' (. | EOF))+;
INTERPOLATED_STRING_EXPR: '${' -> pushMode(DEFAULT_MODE);
INTERPOLATED_STRING_END: '»»' -> popMode;
