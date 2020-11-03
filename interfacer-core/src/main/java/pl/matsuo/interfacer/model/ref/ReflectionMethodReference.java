package pl.matsuo.interfacer.model.ref;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.matsuo.interfacer.model.tv.TypeVariableReference;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFactory.typeDeclarationFor;
import static pl.matsuo.interfacer.util.CollectionUtil.map;

@Slf4j
@RequiredArgsConstructor
public class ReflectionMethodReference implements MethodReference {

  final Method method;
  final TypeSolver typeSolver;

  public String getName() {
    return method.getName();
  }

  public Map<String, String> matches(
      MethodDeclaration methodDeclaration, Map<String, TypeVariableReference> typeVariables) {
    if (!returnTypeMatches(methodDeclaration)) {
      return null;
    }

    if (!parametersMatch(methodDeclaration)) {
      return null;
    }

    // com.github.javaparser.ast.type.Type type = methodDeclaration.getParameter(0).getType();
    // TypeParameter
    // IntersectionType
    // WildcardType

    // Type parameterizedType = method.getParameters()[0].getParameterizedType();
    // ParameterizedType
    // TypeVariable
    // WildcardType

    Map<String, String> result = new HashMap<>();

    Map<String, String> resultConstraints =
        typeConstraints(methodDeclaration.getType(), method.getGenericReturnType());
    Map<String, String> parameterConstraints =
        typeConstraints(
            map(methodDeclaration.getParameters(), Parameter::getType),
            method.getGenericParameterTypes());

    // todo: this is not a correct way of merging type constraints
    result.putAll(resultConstraints);
    result.putAll(parameterConstraints);

    return result;
  }

  public static Map<String, String> typeConstraints(
      List<com.github.javaparser.ast.type.Type> params, Type[] generics) {
    Map<String, String> result = new HashMap<>();

    for (int i = 0; i < params.size(); i++) {
      Map<String, String> partialConstraints = typeConstraints(params.get(i), generics[i]);
      result.putAll(partialConstraints);
    }

    return result;
  }

  public static Map<String, String> typeConstraints(
      com.github.javaparser.ast.type.Type param, Type generic) {
    Map<String, String> result = new HashMap<>();

    if (generic instanceof ParameterizedType) {
      log.info("ParameterizedType " + generic);
      if (param instanceof ClassOrInterfaceType) {
        List<com.github.javaparser.ast.type.Type> typeArguments =
            ((ClassOrInterfaceType) param)
                .getTypeArguments()
                .map(types -> new ArrayList<>(types))
                .orElse(new ArrayList<>());
        result.putAll(
            typeConstraints(typeArguments, ((ParameterizedType) generic).getActualTypeArguments()));
      } else {
        throw new RuntimeException("Not implemented yet");
      }
    } else if (generic instanceof TypeVariable) {
      log.info("TypeVariable " + generic);
      result.put(((TypeVariable<?>) generic).getName(), param.resolve().describe());
    } else {
      log.info("Not a generic: " + generic + " for concrete: " + param);
    }

    return result;
  }

  private boolean parametersMatch(MethodDeclaration methodDeclaration) {
    for (int i = 0; i < method.getParameterCount(); i++) {
      Class<?> paramType = method.getParameters()[i].getType();
      ResolvedType resolvedType = methodDeclaration.getParameter(i).getType().resolve();

      if (!typeDeclarationFor(paramType, typeSolver).isAssignableBy(resolvedType)) {
        return false;
      }
    }
    return true;
  }

  private boolean returnTypeMatches(MethodDeclaration methodDeclaration) {
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
    return true;
  }
}
