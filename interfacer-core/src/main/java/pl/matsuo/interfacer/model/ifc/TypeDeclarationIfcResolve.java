package pl.matsuo.interfacer.model.ifc;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import pl.matsuo.interfacer.model.ref.MethodReference;
import pl.matsuo.interfacer.model.ref.MethodUsageReference;
import pl.matsuo.interfacer.model.tv.TypeVariableReference;

import java.util.List;
import java.util.Map;

import static java.lang.String.join;
import static pl.matsuo.core.util.collection.CollectionUtil.filterMap;
import static pl.matsuo.core.util.collection.CollectionUtil.map;
import static pl.matsuo.core.util.collection.CollectionUtil.toMap;

@ToString
@RequiredArgsConstructor
public class TypeDeclarationIfcResolve extends AbstractIfcResolve {

  final CompilationUnit compileUnit;
  final ClassOrInterfaceDeclaration declaration;

  @Override
  public String getName() {
    return compileUnit
            .getPackageDeclaration()
            .map(packageDeclaration -> packageDeclaration.getNameAsString() + ".")
            .orElse("")
        + declaration.getNameAsString();
  }

  @Override
  public String getGenericName(Map<String, String> typeParams) {
    if (declaration.getTypeParameters().isEmpty()) {
      return getName();
    } else {
      return getName()
          + "<"
          + join(", ", map(declaration.getTypeParameters(), tp -> typeParams.get(tp.getName())))
          + ">";
    }
  }

  @Override
  public List<MethodReference> getMethods() {
    return filterMap(
        declaration.resolve().getAllMethods(),
        method ->
            !method.declaringType().getPackageName().equals("java.lang")
                || !method.declaringType().getClassName().equals("Object"),
        MethodUsageReference::new);
  }

  @Override
  public ResolvedReferenceTypeDeclaration getResolvedTypeDeclaration() {
    return declaration.resolve();
  }

  @Override
  protected Map<String, TypeVariableReference> typeVariables() {
    return toMap(
        declaration.getTypeParameters(),
        TypeParameter::asString,
        tp -> new TypeVariableReference(null, tp));
  }
}
