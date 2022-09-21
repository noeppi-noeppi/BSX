package bsx.compiler.ast;

import bsx.compiler.ast.member.MemberModifier;

import javax.annotation.Nullable;
import java.util.List;

public record BsInterface(List<MemberModifier> modifiers, String name, List<String> interfaces, List<InterfaceMember> members) implements BsTypeNode<InterfaceMember> {

    @Nullable
    @Override
    public String superName() {
        return null;
    }
}
