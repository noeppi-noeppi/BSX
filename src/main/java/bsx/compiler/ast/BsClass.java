package bsx.compiler.ast;

import bsx.compiler.ast.member.MemberModifier;

import javax.annotation.Nullable;
import java.util.List;

public record BsClass(List<MemberModifier> modifiers, String name, @Nullable String superName, List<String> interfaces, List<Member> members) implements BsTypeNode<Member> {
    
}
