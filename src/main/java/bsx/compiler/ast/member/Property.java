package bsx.compiler.ast.member;

import bsx.compiler.ast.Expression;
import bsx.compiler.ast.Member;

import javax.annotation.Nullable;
import java.util.List;

public record Property(int lineNumber, List<MemberModifier> modifiers, String name, @Nullable Expression initialValue) implements Member {
    
}
