package bsx.compiler.ast.member;

import bsx.compiler.ast.InterfaceMember;
import bsx.compiler.ast.Line;
import bsx.compiler.ast.Member;
import bsx.compiler.ast.types.TypeHint;

import javax.annotation.Nullable;
import java.util.List;

public record Function(int lineNumber, List<MemberModifier> modifiers, String name, List<Parameter> args, @Nullable TypeHint returnTypeHint, List<Line> lines) implements Member, InterfaceMember {
    
}
