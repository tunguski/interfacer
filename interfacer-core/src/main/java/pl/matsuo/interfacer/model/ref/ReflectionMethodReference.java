package pl.matsuo.interfacer.model.ref;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

import static com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFactory.typeDeclarationFor;

@Slf4j
@RequiredArgsConstructor
public class ReflectionMethodReference implements MethodReference {

  final Method method;
  final TypeSolver typeSolver;

  public String getName() {
    return method.getName();
  }

  public boolean matches(MethodDeclaration methodDeclaration) {

    if (method.getReturnType().isPrimitive()) {
      if (!methodDeclaration.getType().toString().equals(method.getReturnType().getName())) {
        return false;
      }
    } else {
      ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration =
          typeDeclarationFor(method.getReturnType(), typeSolver);

      if (!resolvedReferenceTypeDeclaration.isAssignableBy(methodDeclaration.getType().resolve())) {
        return false;
      }
    }

    for (int i = 0; i < method.getParameterCount(); i++) {
      Class<?> paramType = method.getParameters()[i].getType();
      ResolvedType resolvedType = methodDeclaration.getParameter(i).getType().resolve();

      if (!typeDeclarationFor(paramType, typeSolver).isAssignableBy(resolvedType)) {
        return false;
      }
    }

    return true;
  }
}
