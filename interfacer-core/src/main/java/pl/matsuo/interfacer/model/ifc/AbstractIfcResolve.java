package pl.matsuo.interfacer.model.ifc;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import pl.matsuo.interfacer.model.ref.MethodReference;

import java.util.List;

import static java.util.Collections.emptyList;
import static pl.matsuo.interfacer.util.CollectionUtil.allMatch;
import static pl.matsuo.interfacer.util.CollectionUtil.anyMatch;

public abstract class AbstractIfcResolve implements IfcResolve {

  @Override
  public List<String> matches(ClassOrInterfaceDeclaration declaration) {
    List<MethodReference> methods = getMethods();
    if (methods.isEmpty()) {
      return null;
    }

    if (!allMatch(
        methods,
        method -> anyMatch(declaration.getMethodsByName(method.getName()), method::matches))) {
      return null;
    }

    // todo: collect type variables specifications required to fulfill interface

    return emptyList();
  }
}
