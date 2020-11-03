package pl.matsuo.interfacer.model.ref;

import com.github.javaparser.ast.body.MethodDeclaration;
import pl.matsuo.interfacer.model.tv.TypeVariableReference;

import java.util.Map;

public interface MethodReference {

  String getName();

  Map<String, String> matches(
      MethodDeclaration methodDeclaration, Map<String, TypeVariableReference> typeVariables);
}
