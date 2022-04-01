package pl.matsuo.interfacer.model.ref;

import static java.util.Collections.emptyMap;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.matsuo.interfacer.model.tv.TypeVariableReference;

@Slf4j
@RequiredArgsConstructor
public class MethodUsageReference implements MethodReference {

  final MethodUsage methodUsage;

  public String getName() {
    return methodUsage.getName();
  }

  public Map<String, String> matches(
      MethodDeclaration methodDeclaration, Map<String, TypeVariableReference> typeVariables) {
    if (!returnTypeMatches(methodDeclaration)) {
      return null;
    }

    if (!parametersMatch(methodDeclaration)) {
      return null;
    }

    // Type type = methodDeclaration.getParameter(0).getType();
    // TypeParameter
    // IntersectionType
    // WildcardType

    // ResolvedType paramType = methodUsage.getParamType(0);
    // ResolvedIntersectionType
    // ResolvedWildcard
    // ResolvedTypeVariable

    return emptyMap();
  }

  private boolean parametersMatch(MethodDeclaration methodDeclaration) {
    for (int i = 0; i < methodUsage.getNoParams(); i++) {
      ResolvedType paramType = methodUsage.getParamType(i);
      ResolvedType resolvedType = methodDeclaration.getParameter(i).getType().resolve();

      if (!paramType.isAssignableBy(resolvedType)) {
        return false;
      }
    }
    return true;
  }

  private boolean returnTypeMatches(MethodDeclaration methodDeclaration) {
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
    return true;
  }
}
