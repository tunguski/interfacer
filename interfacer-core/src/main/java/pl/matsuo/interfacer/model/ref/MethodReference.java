package pl.matsuo.interfacer.model.ref;

import com.github.javaparser.ast.body.MethodDeclaration;
import java.util.Map;
import pl.matsuo.interfacer.model.tv.TypeVariableReference;

public interface MethodReference {

  String getName();

  Map<String, String> matches(
      MethodDeclaration methodDeclaration, Map<String, TypeVariableReference> typeVariables);
}
