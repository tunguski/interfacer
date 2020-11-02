package pl.matsuo.interfacer.model.ifc;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import pl.matsuo.interfacer.model.ref.MethodReference;
import pl.matsuo.interfacer.model.ref.MethodUsageReference;

import java.util.List;

import static pl.matsuo.interfacer.util.CollectionUtil.filterMap;

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
}
