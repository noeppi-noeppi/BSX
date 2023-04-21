package bsx.compiler.parser.ast;

import bsx.BsType;
import bsx.compiler.ast.*;
import bsx.compiler.ast.lang.*;
import bsx.compiler.ast.literal.*;
import bsx.compiler.ast.member.Function;
import bsx.compiler.ast.member.MemberModifier;
import bsx.compiler.ast.member.Parameter;
import bsx.compiler.ast.member.Property;
import bsx.compiler.ast.name.*;
import bsx.compiler.ast.operator.BinaryOperator;
import bsx.compiler.ast.operator.UnaryOperator;
import bsx.compiler.ast.types.TypeCast;
import bsx.compiler.ast.types.TypeHint;
import bsx.compiler.parser.antlr.BsParser;
import bsx.type.*;
import bsx.util.string.StringEscapeHelper;
import bsx.value.NoValue;
import bsx.value.NullValue;
import org.antlr.v4.runtime.RuleContext;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AstConverter {
    
    private long nextLabel;
    
    public AstConverter() {
        this.nextLabel= 42;
    }
    
    public Program program(BsParser.ProgramContext ctx) {
        List<Program.Entry> entries = ctx.code().stream().map(this::programEntry).toList();
        return new Program(entries);
    }
    
    private Program.Entry programEntry(BsParser.CodeContext ctx) {
        if (ctx.class_() != null) return new Program.ClassEntry(this.bsClass(ctx.class_()));
        if (ctx.interface_() != null) return new Program.InterfaceEntry(this.bsInterface(ctx.interface_()));
        if (ctx.function() != null) return new Program.FunctionEntry(this.topLevelFunction(ctx.function()));
        if (ctx.statement() != null) return new Program.LineEntry(this.line(ctx.statement()));
        throw new IncompatibleClassChangeError();
    }

    private BsClass bsClass(BsParser.ClassContext ctx) {
        List<MemberModifier> modifiers = this.modifiers(ctx.modifiers(), MemberModifier.STATIC, MemberModifier.READONLY);
        String name = this.typeName(ctx.typeName());
        String superName = ctx.super_() == null ? null : this.typeName(ctx.super_().typeName());
        List<String> interfaces = ctx.implements_() == null ? List.of() : this.typeNames(ctx.implements_().typeNames());
        List<Member> members = ctx.member().stream().map(this::member).toList();
        return new BsClass(modifiers, name, superName, interfaces, members);
    }
    
    private BsInterface bsInterface(BsParser.InterfaceContext ctx) {
        List<MemberModifier> modifiers = this.modifiers(ctx.modifiers(), MemberModifier.STATIC, MemberModifier.READONLY);
        String name = this.typeName(ctx.typeName());
        List<String> interfaces = ctx.interfaceSuper() == null ? List.of() : this.typeNames(ctx.interfaceSuper().typeNames());
        List<InterfaceMember> members = ctx.interfaceMember().stream().map(this::member).toList();
        return new BsInterface(modifiers, name, interfaces, members);
    }

    private List<MemberModifier> modifiers(BsParser.ModifiersContext ctx, MemberModifier... disallowed) {
        List<MemberModifier> modifiers = ctx.modifier().stream().map(this::modifier).distinct().toList();
        int numAccessModifiers = 0;
        if (modifiers.contains(MemberModifier.PUBLIC)) numAccessModifiers += 1;
        if (modifiers.contains(MemberModifier.PROTECTED)) numAccessModifiers += 1;
        if (modifiers.contains(MemberModifier.PRIVATE)) numAccessModifiers += 1;
        if (numAccessModifiers > 1) {
            throw new IllegalStateException("Element can't have more than one access modifier.");
        }
        for (MemberModifier mod : disallowed) {
            if (modifiers.contains(mod)) {
                throw new IllegalStateException("Modifier " + mod.name().toLowerCase(Locale.ROOT) + " is disallowed here.");
            }
        }
        return modifiers;
    }
    
    private MemberModifier modifier(BsParser.ModifierContext ctx) {
        if (ctx.modifierPublic() != null) return MemberModifier.PUBLIC;
        if (ctx.modifierProtected() != null) return MemberModifier.PROTECTED;
        if (ctx.modifierPrivate() != null) return MemberModifier.PRIVATE;
        if (ctx.modifierStatic() != null) return MemberModifier.STATIC;
        if (ctx.modifierFinal() != null) return MemberModifier.FINAL;
        if (ctx.modifierReadonly() != null) return MemberModifier.READONLY;
        throw new IncompatibleClassChangeError();
    }
    
    private Member member(BsParser.MemberContext ctx) {
        if (ctx.property() != null) return this.property(ctx.property());
        if (ctx.function() != null) return this.function(ctx.function());
        throw new IncompatibleClassChangeError();
    }
    
    private InterfaceMember member(BsParser.InterfaceMemberContext ctx) {
        if (ctx.abstractFunction() != null) return this.interfaceFunction(ctx.abstractFunction());
        if (ctx.function() != null) return this.interfaceFunction(ctx.function());
        throw new IncompatibleClassChangeError();
    }
    
    private Property property(BsParser.PropertyContext ctx) {
        List<MemberModifier> modifiers = this.modifiers(ctx.modifiers(), MemberModifier.FINAL);
        String name = this.variable(ctx.variable()).name();
        Expression expr = ctx.expression() == null ? null : this.expression(ctx.expression());
        int startLine = ctx.expression() == null ? ctx.getStart().getLine() : ctx.expression().getStart().getLine();
        return new Property(startLine, modifiers, name, expr);
    }
    
    private Function topLevelFunction(BsParser.FunctionContext ctx) {
        List<MemberModifier> modifiers = this.modifiers(ctx.modifiers(), MemberModifier.STATIC, MemberModifier.FINAL, MemberModifier.READONLY);
        return this.anyFunction(Stream.concat(modifiers.stream(), Stream.of(MemberModifier.STATIC)).toList(), ctx);
    }
    
    private Function function(BsParser.FunctionContext ctx) {
        List<MemberModifier> modifiers = this.modifiers(ctx.modifiers(), MemberModifier.READONLY);
        return this.anyFunction(modifiers, ctx);
    }
    
    private Function interfaceFunction(BsParser.FunctionContext ctx) {
        List<MemberModifier> modifiers = this.modifiers(ctx.modifiers(), MemberModifier.READONLY);
        if (!modifiers.contains(MemberModifier.STATIC)) throw new IllegalStateException("Interface method can't contain any code");
        return this.anyFunction(modifiers, ctx);
    }
    
    private Function interfaceFunction(BsParser.AbstractFunctionContext ctx) {
        List<MemberModifier> modifiers = this.modifiers(ctx.modifiers(), MemberModifier.READONLY);
        if (modifiers.contains(MemberModifier.STATIC)) throw new IllegalStateException("Static interface method without any code");
        String name = this.memberName(ctx.memberName());
        List<Parameter> parameters = ctx.functionParamList().functionParam().stream().map(this::param).toList();
        TypeHint returnTypeHint = ctx.typeHint() == null ? null : this.typeHint(ctx.typeHint());
        return new Function(ctx.getStart().getLine(), modifiers, name, parameters, returnTypeHint, List.of());
    }
    
    private Function anyFunction(List<MemberModifier> modifiers, BsParser.FunctionContext ctx) {
        String name = this.memberName(ctx.memberName());
        List<Parameter> parameters = ctx.functionParamList().functionParam().stream().map(this::param).toList();
        TypeHint returnTypeHint = ctx.typeHint() == null ? null : this.typeHint(ctx.typeHint());
        List<Line> lines = this.lines(ctx.statement());
        return new Function(ctx.getStart().getLine(), modifiers, name, parameters, returnTypeHint, lines);
    }
    
    private Parameter param(BsParser.FunctionParamContext ctx) {
        VariableName var = this.variable(ctx.variable());
        TypeHint hint = ctx.typeHint() == null ? null : this.typeHint(ctx.typeHint());
        return new Parameter(var, hint);
    }
    
    private TypeHint typeHint(BsParser.TypeHintContext ctx) {
        if (ctx.typeHintSingle() != null) {
            char chr = ctx.typeHintSingle().stypeNoArray().getText().toLowerCase(Locale.ROOT).charAt(0);
            boolean hasVowel = chr == 'a' || chr == 'e' || chr == 'i' || chr == 'o' || chr =='u' || chr == 'h';
            boolean needsVowel = ctx.typeHintSingle().IS_PROBABLY_AN() != null;
            if (hasVowel != needsVowel) throw new IllegalStateException("Invalid constant"); // See https://www.youtube.com/watch?v=vcFBwt1nu2U&t=2043s
            return new TypeHint(this.type(ctx.typeHintSingle().stypeNoArray()));
        }
        if (ctx.typeHintArray() != null) return new TypeHint(new ArrayType(this.type(ctx.typeHintArray().ptype())));
        throw new IncompatibleClassChangeError();
    }
    
    private BsType type(BsParser.StypeContext ctx) {
        if (ctx.stypeNoArray() != null) return this.type(ctx.stypeNoArray());
        if (ctx.ptype() != null) return new ArrayType(this.type(ctx.ptype()));
        throw new IncompatibleClassChangeError();
    }
    
    private BsType type(BsParser.StypeNoArrayContext ctx) {
        return this.namedTypeSingular(this.typeName(ctx.typeName()));
    }

    private BsType type(BsParser.PtypeContext ctx) {
        BsType baseType = this.namedTypePlural(this.typeName(ctx.typeName()));
        for (int i = 0; i < ctx.ARRAYS_OF().size(); i++) {
            baseType = new ArrayType(baseType);
        }
        return baseType;
    }
    
    private BsType namedTypeSingular(String name) {
        return switch (name) {
            case "void" -> NoValue.INSTANCE;
            case "null" -> NullValue.NULL;
            case "Nothing" -> NullValue.NOTHING;
            case "undefined" -> NullValue.UNDEFINED;
            case "nada" -> NullValue.NADA;
            case "Empty" -> NullValue.EMPTY;
            case "Boolean" -> BoolType.INSTANCE;
            case "Integer" -> IntegerType.INSTANCE;
            case "Float" -> FloatType.INSTANCE;
            case "array" -> new ArrayType(AnyType.INSTANCE);
            case "ASCII" -> StringType.ASCII;
            case "ANSI" -> StringType.ANSI;
            case "DBCS" -> StringType.DBCS;
            case "EBCDIC" -> StringType.EBCDIC;
            case "String" -> StringType.UTF256;
            default -> ClassType.makeCompileStatic(name);
        };
    }

    private BsType namedTypePlural(String name) {
        return switch (name) {
            case "voids" -> NoValue.INSTANCE;
            case "nulls" -> NullValue.NULL;
            case "Nothings" -> NullValue.NOTHING;
            case "undefined" -> NullValue.UNDEFINED;
            case "nadas" -> NullValue.NADA;
            case "Empties" -> NullValue.EMPTY;
            case "Booleans" -> BoolType.INSTANCE;
            case "Integers" -> IntegerType.INSTANCE;
            case "Floats" -> FloatType.INSTANCE;
            case "arrays" -> new ArrayType(AnyType.INSTANCE);
            case "ASCIIs" -> StringType.ASCII;
            case "ANSIs" -> StringType.ANSI;
            case "DBCS" -> StringType.DBCS;
            case "EBCDICs" -> StringType.EBCDIC;
            case "Strings" -> StringType.UTF256;
            default -> ClassType.makeCompileStatic(name);
        };
    }
    
    private String typeName(BsParser.TypeNameContext ctx) {
        if (ctx.typeNameReserved() != null) return ctx.typeNameReserved().getText();
        if (ctx.typeNameNested() != null) {
            String base = this.typeNameSimple(ctx.typeNameNested().typeNameSimple());
            String nested = ctx.typeNameNested().IDENT().stream().map(part -> part.getSymbol().getText()).collect(Collectors.joining("$"));
            if (nested.isEmpty()) {
                return base;
            } else {
                return base + "$" + nested;
            }
        }
        throw new IncompatibleClassChangeError();
    }
    
    private List<String> typeNames(BsParser.TypeNamesContext ctx) {
        return ctx.typeName().stream().map(this::typeName).toList();
    }
    
    private String typeNameSimple(BsParser.TypeNameSimpleContext ctx) {
        return ctx.IDENT().stream().map(part -> part.getSymbol().getText()).collect(Collectors.joining("."));
    }
    
    private String memberName(BsParser.MemberNameContext ctx) {
        return ctx.IDENT().getSymbol().getText();
    }
    
    private VariableName variable(BsParser.VariableContext ctx) {
        String text = ctx.VARIABLE().getText();
        if (text.startsWith("€")) text = text.substring(1);
        return new VariableName(text);
    }

    private List<Line> lines(List<BsParser.StatementContext> list) {
        return list.stream().map(this::line).toList();
    }
    
    private Line line(BsParser.StatementContext ctx) {
        Statement stmt = this.statement(ctx);
        int line = ctx.getStart().getLine();
        return new Line(line, stmt);
    }
    
    private Statement statement(BsParser.StatementContext ctx) {
        if (ctx.labelledStatement() != null) {
            long label = Long.parseLong(ctx.labelledStatement().INTEGER().getText());
            if (label != this.nextLabel) throw new IllegalStateException("Invalid label, expected " + this.nextLabel + ", got " + label);
            this.nextLabel += 42;
            return new LabelledStatement(label, this.statement(ctx.labelledStatement().statementContent()));
        } else if (ctx.statementContent() != null) {
            return this.statement(ctx.statementContent());
        } else {
            throw new IncompatibleClassChangeError();
        }
    }

    private Statement statement(BsParser.StatementContentContext ctx) {
        if (ctx.echoStatement() != null) return new Echo(this.expression(ctx.echoStatement().expression()));
        if (ctx.gotoStatement() != null) return new Goto(Long.parseLong(ctx.gotoStatement().INTEGER().getText()));
        if (ctx.passStatement() != null) return NoOp.INSTANCE;
        if (ctx.deleteStatement() != null) return new Deletion(ctx.deleteStatement().variable().stream().map(this::variable).toList());
        if (ctx.returnStatementValued() != null) return new Return(this.expression(ctx.returnStatementValued().expression()));
        if (ctx.returnStatement() != null) return new Return(null);
        if (ctx.doAndStatement() != null) return this.doAnd(ctx.doAndStatement());
        if (ctx.unlessElseStatement() != null) return this.unlessElse(ctx.unlessElseStatement());
        if (ctx.unlessStatement() != null) return this.unless(ctx.unlessStatement());
        if (ctx.assignStatement() != null) return new Assignment(this.expression(ctx.assignStatement().target), this.expression(ctx.assignStatement().expression()));
        if (ctx.updateStatement() != null) return new UpdateCall(this.expression(ctx.updateStatement().target), this.paramList(ctx.updateStatement().paramList()), this.expression(ctx.updateStatement().expression()));
        if (ctx.chainedOperatorStatement() != null) return this.chainedOperator(ctx.chainedOperatorStatement());
        if (ctx.expressionStatement() != null) return this.expression(ctx.expressionStatement().expression());
        throw new IncompatibleClassChangeError();
    }
    
    private DoAnd doAnd(BsParser.DoAndStatementContext ctx) {
        return new DoAnd(
                Stream.concat(
                        Stream.of(ctx.doStatement()).map(BsParser.DoStatementContext::statement),
                        ctx.andStatement().stream().map(BsParser.AndStatementContext::statement)
                ).map(this::lines).map(DoAnd.Block::new).toList()
        );
    }
    
    private BranchCondition unlessElse(BsParser.UnlessElseStatementContext ctx) {
        // else part appears before unless part in source, so load it first to respect label order
        List<Line> ifTrue = this.lines(ctx.statement());
        BranchCondition unless = this.unless(ctx.unlessStatement());
        return new BranchCondition(unless.conditionLineNumber(), unless.condition(), ifTrue, unless.ifFalse());
    }
    
    private BranchCondition unless(BsParser.UnlessStatementContext ctx) {
        List<Line> ifFalse = this.lines(ctx.statement());
        Expression condition = this.expression(ctx.expression());
        return new BranchCondition(ctx.expression().getStart().getLine(), condition, List.of(), ifFalse);
    }
    
    private Assignment chainedOperator(BsParser.ChainedOperatorStatementContext ctx) {
        VariableName var = this.variable(ctx.variable());
        String opStr = ctx.CHAINED_OPERATOR().getSymbol().getText();
        BinaryOperator.Type type = null;
        if (opStr.endsWith("=")) type = OperatorResolver.getType(opStr.substring(0, opStr.length() - 1));
        if (type == null) throw new IllegalArgumentException("Invalid chained operator: " + opStr);
        Expression expr = this.expression(ctx.expression());
        return new Assignment(var, new BinaryOperator(type, var, expr));
    }
    
    private List<Expression> paramList(BsParser.ParamListContext ctx) {
        return ctx.expressionNoComma().stream().map(this::expression).toList();
    }
    
    private Expression expression(BsParser.ExpressionContext ctx) {
        List<Expression> expressions = ctx.expressionNoOperator().stream().map(this::expression).toList();
        List<String> operators = ctx.operatorLiteralInfix().stream().map(RuleContext::getText).toList();
        return OperatorResolver.applyOperators(expressions, operators);
    }
    
    private Expression expression(BsParser.ExpressionNoCommaContext ctx) {
        List<Expression> expressions = ctx.expressionNoOperator().stream().map(this::expression).toList();
        List<String> operators = ctx.operatorLiteralInfixNoComma().stream().map(RuleContext::getText).toList();
        return OperatorResolver.applyOperators(expressions, operators);
    }
    
    private Expression expression(BsParser.ExpressionNoOperatorContext ctx) {
        if (ctx.expressionNoInstanceOf() != null) return this.expression(ctx.expressionNoInstanceOf());
        if (ctx.instanceOf() != null) return new InstanceCheck(this.expression(ctx.expressionNoInstanceOf()), this.type(ctx.instanceOf().stype()));
        throw new IncompatibleClassChangeError();
    }
    
    private Expression expression(BsParser.ExpressionNoInstanceOfContext ctx) {
        if (ctx.instancePropertyOrApplyCall() != null) return this.instancePropertyOrApplyCall(ctx.instancePropertyOrApplyCall());
        if (ctx.typeCast() != null) return this.typeCast(ctx.typeCast());
        if (ctx.prefixOperator() != null) return new UnaryOperator(this.unaryOperator(ctx.prefixOperator().operatorLiteralPrefix()), this.expression(ctx.prefixOperator().expressionNoInstanceOf()));
        if (ctx.expressionNoProperty() != null) return this.expression(ctx.expressionNoProperty());
        throw new IncompatibleClassChangeError();
    }
    
    private Expression instancePropertyOrApplyCall(BsParser.InstancePropertyOrApplyCallContext ctx) {
        Expression base = null;
        if (ctx.instancePropertyOrApplyCall() != null) base = this.instancePropertyOrApplyCall(ctx.instancePropertyOrApplyCall());
        if (ctx.expressionNoProperty() != null) base = this.expression(ctx.expressionNoProperty());
        if (base == null) throw new IncompatibleClassChangeError();
        
        BsParser.InstancePropertyOrApplyCallPartContext part = ctx.instancePropertyOrApplyCallPart();
        if (part.paramList() != null) return new ApplyCall(base, this.paramList(part.paramList()));
        if (part.IDENT() != null) return new InstanceProperty(base, part.IDENT().getSymbol().getText());
        throw new IncompatibleClassChangeError();
    }
    
    private Expression expression(BsParser.ExpressionNoPropertyContext ctx) {
        if (ctx.parenExpression() != null) return new Parens(this.expression(ctx.parenExpression().expression()));
        if (ctx.staticProperty() != null) return new StaticProperty(this.type(ctx.staticProperty().stype()), ctx.staticProperty().IDENT().getText());
        if (ctx.parentProperty() != null) return new ParentProperty(ctx.parentProperty().IDENT().getText());
        if (ctx.literal() != null) return this.literal(ctx.literal());
        if (ctx.objectCreation() != null) return new ObjectCreation(this.type(ctx.objectCreation().stype()), this.paramList(ctx.objectCreation().paramList()));
        if (ctx.inlineIncrementVariableFirst() != null) return this.inlineIncrement(ctx.inlineIncrementVariableFirst());
        if (ctx.inlineIncrementVariableLast() != null) return this.inlineIncrement(ctx.inlineIncrementVariableLast());
        if (ctx.variable() != null) return this.variable(ctx.variable());
        if (ctx.name() != null) return new Name(ctx.name().IDENT().getText());
        throw new IncompatibleClassChangeError();
    }
    
    private InlineIncrement inlineIncrement(BsParser.InlineIncrementVariableFirstContext ctx) {
        return new InlineIncrement(this.variable(ctx.variable()), ctx.INLINE_PLUS() != null, true);
    }
    
    private InlineIncrement inlineIncrement(BsParser.InlineIncrementVariableLastContext ctx) {
        return new InlineIncrement(this.variable(ctx.variable()), ctx.INLINE_PLUS() != null, false);
    }
    
    private TypeCast typeCast(BsParser.TypeCastContext ctx) {
        if (ctx.parenExpression() != null) return new TypeCast(this.type(ctx.stype()), this.expression(ctx.parenExpression().expression()));
        if (ctx.expressionNoInstanceOf() != null) return new TypeCast(this.type(ctx.stype()), this.expression(ctx.expressionNoInstanceOf()));
        throw new IncompatibleClassChangeError();
    }
    
    private UnaryOperator.Type unaryOperator(BsParser.OperatorLiteralPrefixContext ctx) {
        if (ctx.BANG_OPERATOR() != null) return UnaryOperator.Type.INVERT;
        if (ctx.MINUS_OPERATOR() != null) return UnaryOperator.Type.NEGATE;
        throw new IncompatibleClassChangeError();
    }
    
    private Literal literal(BsParser.LiteralContext ctx) {
        if (ctx.literalThis() != null) return ThisLiteral.INSTANCE;
        if (ctx.literalTrue() != null) return new BoolLiteral(true);
        if (ctx.literalFalse() != null) return new BoolLiteral(false);
        if (ctx.literalNull() != null) return new NullLiteral(NullValue.NULL);
        if (ctx.literalNothing() != null) return new NullLiteral(NullValue.NOTHING);
        if (ctx.literalUndefined() != null) return new NullLiteral(NullValue.UNDEFINED);
        if (ctx.literalNada() != null) return new NullLiteral(NullValue.NADA);
        if (ctx.literalEmpty() != null) return new NullLiteral(NullValue.EMPTY);
        if (ctx.literalNaN() != null) return new FloatLiteral(Double.NaN);
        if (ctx.literalInfinity() != null) return new FloatLiteral(Double.POSITIVE_INFINITY);
        if (ctx.literalPie() != null) return PieLiteral.INSTANCE;
        if (ctx.literalInt() != null) return IntLiteral.create(ctx.literalInt().INTEGER().getText());
        if (ctx.literalFloat() != null) return FloatLiteral.create(ctx.literalFloat().FLOAT().getText());
        if (ctx.literalAscii() != null) return new StringLiteral(StringType.ASCII, this.makeStringLiteral(ctx.literalAscii().ASCII().getText(), 1, false));
        if (ctx.literalAnsi() != null) return new StringLiteral(StringType.ANSI, this.makeStringLiteral(ctx.literalAnsi().ANSI().getText(), 2, false));
        if (ctx.literalDbcs() != null) return new StringLiteral(StringType.DBCS, this.makeStringLiteral(ctx.literalDbcs().DBCS().getText(), 1, false));
        if (ctx.literalEbcdic() != null) return new StringLiteral(StringType.EBCDIC, this.makeStringLiteral(ctx.literalEbcdic().EBCDIC().getText(), 2, false));
        if (ctx.literalUtf256() != null) return new UtfLiteral(this.makeStringLiteral(ctx.literalUtf256().UTF256().getText(), 1, true));
        if (ctx.literalInterpolatedUtf256() != null) return this.interpolatedUtf256(ctx.literalInterpolatedUtf256());
        throw new IncompatibleClassChangeError();
    }
    
    private InterpolatedUtfLiteral interpolatedUtf256(BsParser.LiteralInterpolatedUtf256Context ctx) {
        return new InterpolatedUtfLiteral(ctx.interpolatedPart().stream().map(this::interpolatedUtf256Entry).toList());
    }
    
    private InterpolatedUtfLiteral.Entry interpolatedUtf256Entry(BsParser.InterpolatedPartContext ctx) {
        if (ctx.interpolatedText() != null) return new InterpolatedUtfLiteral.ConstantEntry(ctx.interpolatedText().INTERPOLATED_STRING_TEXT().getText());
        if (ctx.interpolatedExpression() != null) return new InterpolatedUtfLiteral.ExpressionEntry(this.expression(ctx.interpolatedExpression().expression()));
        throw new IncompatibleClassChangeError();
    }
    
    private String makeStringLiteral(String text, int clip, boolean utf) {
        String content = text.substring(clip, text.length() - clip);
        if (utf) {
            StringEscapeHelper.unescapeUtf256(content); // To throw errors
            return content;
        } else {
            return StringEscapeHelper.unescape(content);
        }
    }
}
