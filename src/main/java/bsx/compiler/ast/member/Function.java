package bsx.compiler.ast.member;

import bsx.compiler.ast.Member;
import bsx.compiler.ast.Statement;
import bsx.compiler.ast.types.TypeHint;

import javax.annotation.Nullable;
import java.util.List;

public record Function(List<MemberModifier> modifiers, String name, List<Parameter> args, @Nullable TypeHint returnTypeHint, List<Statement> statements) implements Member {
    
}
