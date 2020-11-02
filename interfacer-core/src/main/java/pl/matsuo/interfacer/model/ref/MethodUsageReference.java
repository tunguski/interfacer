package pl.matsuo.interfacer.model.ref;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.types.ResolvedType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MethodUsageReference implements MethodReference {

  final MethodUsage methodUsage;

  public String getName() {
    return methodUsage.getName();
  }

  public boolean matches(MethodDeclaration methodDeclaration) {
    if (methodUsage.returnType().isPrimitive() || methodUsage.returnType().isVoid()) {
      if (!methodUsage
          .returnType()
          .toString()
          .equals(methodDeclaration.getType().resolve().toString())) {
        return false;
      }
    } else {
      if (!methodUsage.returnType().isAssignableBy(methodDeclaration.getType().resolve())) {
        return false;
      }
    }

    for (int i = 0; i < methodUsage.getNoParams(); i++) {
      ResolvedType paramType = methodUsage.getParamType(i);
      ResolvedType resolvedType = methodDeclaration.getParameter(i).getType().resolve();

      if (!paramType.isAssignableBy(resolvedType)) {
        return false;
      }
    }

    return true;
  }
}
