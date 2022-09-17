package bsx.compiler.ast.member;

import bsx.compiler.ast.name.VariableName;
import bsx.compiler.ast.types.TypeHint;

import javax.annotation.Nullable;

public record Parameter(VariableName variable, @Nullable TypeHint hint) {
}
