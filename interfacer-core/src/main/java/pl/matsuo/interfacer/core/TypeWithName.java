package pl.matsuo.interfacer.core;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TypeWithName {

  final Method method;
  final MethodUsage methodUsage;

  public TypeWithName(Method method) {
    this(method, null);
  }

  public TypeWithName(MethodUsage methodUsage) {
    this(null, methodUsage);
  }

  public String getName() {
    if (method != null) {
      return method.getName();
    } else {
      return methodUsage.getName();
    }
  }

  public boolean matches(
      MethodDeclaration methodDeclaration, CombinedTypeSolver combinedTypeSolver) {
    if (methodUsage != null) {
      return methodUsage.returnType().isAssignableBy(methodDeclaration.getType().resolve());
    } else {
      ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration =
          method.getReturnType().isInterface()
              ? new ReflectionInterfaceDeclaration(method.getReturnType(), combinedTypeSolver)
              : new ReflectionClassDeclaration(method.getReturnType(), combinedTypeSolver);

      return resolvedReferenceTypeDeclaration.isAssignableBy(methodDeclaration.getType().resolve());
    }
  }
}
