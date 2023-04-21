parser grammar BsParser;
@header { package bsx.compiler.parser.antlr; }
options { tokenVocab=BsLexer; }

program: code* EOF;
code: class | interface | function | statement;

class: modifiers CLASS typeName super? implements? COLON START_BLOCK member+ END_BLOCK;
interface: modifiers INTERFACE typeName interfaceSuper? COLON START_BLOCK interfaceMember+ END_BLOCK;
super: EXTENDS typeName;
implements: IMPLEMENTS typeNames;
interfaceSuper: EXTENDS typeNames;
member: function | property;
interfaceMember: abstractFunction | function;

property: modifiers variable (EQUAL expression)? SEMICOLON;

function: modifiers FUNCTION memberName functionParamList typeHint? START_BLOCK statement+ END_BLOCK;
abstractFunction: modifiers FUNCTION memberName functionParamList typeHint? SEMICOLON;
functionParamList: START_GROUP (functionParam (COMMA functionParam)*)? END_GROUP;
functionParam: variable typeHint?;

modifiers: modifier*;
modifier: modifierPublic | modifierProtected | modifierPrivate | modifierStatic | modifierFinal | modifierReadonly;
modifierPublic: PUBLIC;
modifierProtected: PROTECTED;
modifierPrivate: PRIVATE;
modifierStatic: STATIC;
modifierFinal: FINAL;
modifierReadonly: READONLY;

statement: labelledStatement | statementContent;
labelledStatement: INTEGER statementContent;

statementContent: echoStatement | gotoStatement | passStatement | deleteStatement | returnStatementValued | returnStatement
  | doAndStatement | unlessElseStatement | unlessStatement | assignStatement | updateStatement | chainedOperatorStatement
  | expressionStatement;

expressionStatement: expression SEMICOLON;
echoStatement: ECHO expression SEMICOLON;
gotoStatement: GOTO INTEGER SEMICOLON;
passStatement: PASS SEMICOLON;
deleteStatement: DELETE variable (COMMA variable)* SEMICOLON;
returnStatementValued: RETURN expression SEMICOLON;
returnStatement: RETURN SEMICOLON;
chainedOperatorStatement: variable CHAINED_OPERATOR expression SEMICOLON;

doAndStatement: doStatement andStatement*;
doStatement: DO COLON START_BLOCK statement+ END_BLOCK;
andStatement: AND COLON START_BLOCK statement+ END_BLOCK;

unlessStatement: START_BLOCK statement+ END_BLOCK START_GROUP UNLESS expression END_GROUP GREEK_QUESTION_MARK;
unlessElseStatement: START_BLOCK statement+ END_BLOCK ELSE unlessStatement;

assignStatement: target=expressionNoInstanceOf EQUAL expression SEMICOLON;
updateStatement: target=expressionNoInstanceOf paramList EQUAL expression SEMICOLON;

operatorLiteralInfix: INFIX_OPERATOR_NO_COMMA_NO_MINUS | MINUS_OPERATOR | COMMA;
operatorLiteralInfixNoComma: INFIX_OPERATOR_NO_COMMA_NO_MINUS | MINUS_OPERATOR;
operatorLiteralPrefix: BANG_OPERATOR | MINUS_OPERATOR;

expression: expressionNoOperator (operatorLiteralInfix expressionNoOperator)*;
expressionNoComma: expressionNoOperator (operatorLiteralInfixNoComma expressionNoOperator)*;
parenExpression: START_GROUP expression END_GROUP;
expressionNoOperator: instanceOf | expressionNoInstanceOf;
expressionNoInstanceOf: typeCast | prefixOperator | instancePropertyOrApplyCall | expressionNoProperty;
expressionNoProperty: parenExpression | staticProperty | parentProperty | literal | objectCreation
  | inlineIncrementVariableFirst | inlineIncrementVariableLast | variable | name;

paramList: START_GROUP (expressionNoComma (COMMA expressionNoComma)*)? END_GROUP;

literal: literalThis | literalTrue | literalFalse | literalNull | literalNothing | literalUndefined
  | literalNada | literalEmpty | literalNaN | literalInfinity | literalPie | literalInt | literalFloat
  | literalAscii | literalAnsi | literalDbcs | literalEbcdic | literalUtf256 | literalInterpolatedUtf256;
literalThis: THIS;
literalTrue: TRUE;
literalFalse: FALSE;
literalNull: NULL;
literalNothing: NOTHING;
literalUndefined: UNDEFINED;
literalNada: NADA;
literalEmpty: EMPTY;
literalNaN: NAN;
literalInfinity: INFINITY;
literalPie: PIE;
literalInt: INTEGER;
literalFloat: FLOAT;
literalAscii: ASCII;
literalAnsi: ANSI;
literalDbcs: DBCS;
literalEbcdic: EBCDIC;
literalUtf256: UTF256;
literalInterpolatedUtf256: START_UTF256_INTERP interpolatedPart* INTERPOLATED_STRING_END;
interpolatedPart: interpolatedText | interpolatedExpression;
interpolatedText: INTERPOLATED_STRING_TEXT;
interpolatedExpression: INTERPOLATED_STRING_EXPR expression END_INLINE;

instancePropertyOrApplyCall: instancePropertyOrApplyCall instancePropertyOrApplyCallPart | expressionNoProperty instancePropertyOrApplyCallPart;
instancePropertyOrApplyCallPart: INSTANCE_ACCESS IDENT | paramList;
staticProperty: stype STATIC_ACCESS IDENT;
parentProperty: PARENT STATIC_ACCESS IDENT;

prefixOperator: operatorLiteralPrefix expressionNoInstanceOf;
objectCreation: NEW stype paramList;
instanceOf: expressionNoInstanceOf INSTANCE_OF stype;
typeCast: START_GROUP stype END_GROUP (parenExpression | expressionNoInstanceOf);

inlineIncrementVariableFirst: (INLINE_PLUS | INLINE_MINUS) variable;
inlineIncrementVariableLast: variable (INLINE_PLUS | INLINE_MINUS);
variable: VARIABLE;
name: IDENT;

typeHint: typeHintSingle | typeHintArray;
typeHintSingle: (IS_PROBABLY_A | IS_PROBABLY_AN) stypeNoArray;
typeHintArray: ARE_PROBABLY ptype;
stype: (ARRAY_OF ptype) | stypeNoArray;
stypeNoArray: typeName;
ptype: ARRAYS_OF* typeName;
typeName: typeNameReserved | typeNameNested;
typeNames: typeName (COMMA typeName)*;
typeNameSimple: IDENT (DOT IDENT)*;
typeNameNested: typeNameSimple (STATIC_ACCESS IDENT)*;
typeNameReserved: NULL | NOTHING | UNDEFINED | NADA | EMPTY;
memberName: IDENT;
