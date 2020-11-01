package pl.matsuo.interfacer.core;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

import static com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFactory.typeDeclarationFor;

@Slf4j
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
      log.info(
          "methodDeclaration "
              + methodDeclaration.getType()
              + " methodUsage "
              + methodUsage.returnType().describe());
      if (methodUsage.returnType().isPrimitive() || methodUsage.returnType().isVoid()) {
        log.info(
            "methodUsage.returnType() "
                + methodUsage.returnType()
                + " methodDeclaration.getType().resolve() "
                + methodDeclaration.getType().resolve());
        return methodUsage
            .returnType()
            .toString()
            .equals(methodDeclaration.getType().resolve().toString());
      } else {
        return methodUsage.returnType().isAssignableBy(methodDeclaration.getType().resolve());
      }
    } else {
      if (method.getReturnType().isPrimitive()) {
        return methodDeclaration.getType().toString().equals(method.getReturnType().getName());
      } else {
        ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration =
            typeDeclarationFor(method.getReturnType(), combinedTypeSolver);

        return resolvedReferenceTypeDeclaration.isAssignableBy(
            methodDeclaration.getType().resolve());
      }
    }
  }

  public String[] getParamStringTypes() {
    if (method != null) {
      String[] paramStringTypes = new String[method.getParameterCount()];
      for (int i = 0; i < method.getParameterCount(); i++) {
        paramStringTypes[i] = method.getParameterTypes()[i].getCanonicalName();
      }
      return paramStringTypes;
    } else {
      String[] paramStringTypes = new String[methodUsage.getNoParams()];
      for (int i = 0; i < methodUsage.getNoParams(); i++) {
        paramStringTypes[i] = methodUsage.getParamType(i).describe();
      }
      return paramStringTypes;
    }
  }
}
