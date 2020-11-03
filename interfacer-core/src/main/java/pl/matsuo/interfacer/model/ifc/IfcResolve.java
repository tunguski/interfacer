package pl.matsuo.interfacer.model.ifc;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import pl.matsuo.interfacer.model.ref.MethodReference;

import java.util.List;
import java.util.Map;

public interface IfcResolve {

  String getName();

  String getGenericName(Map<String, String> typeParams);

  List<MethodReference> getMethods();

  ResolvedReferenceTypeDeclaration getResolvedTypeDeclaration();

  /**
   * Return list of interface's type params specifications required to match this interface by the
   * declaration. If class is not generic, returns empty list if declaration fulfills interface.
   *
   * <p>Returns null if declaration does not match with interface.
   */
  Map<String, String> matches(ClassOrInterfaceDeclaration declaration);
}
