package pl.matsuo.interfacer.model.ifc;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import pl.matsuo.interfacer.model.ref.MethodReference;
import pl.matsuo.interfacer.model.ref.ReflectionMethodReference;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFactory.typeDeclarationFor;

@ToString
@RequiredArgsConstructor
public class ClassIfcResolve extends AbstractIfcResolve {

  final Class<?> clazz;
  final TypeSolver typeSolver;

  @Override
  public String getName() {
    return clazz.getName();
  }

  @Override
  public List<MethodReference> getMethods() {
    List<MethodReference> result = new ArrayList<>();
    for (Method method : clazz.getMethods()) {
      result.add(new ReflectionMethodReference(method, typeSolver));
    }

    return result;
  }

  @Override
  public ResolvedReferenceTypeDeclaration getResolvedTypeDeclaration() {
    return typeDeclarationFor(clazz, typeSolver);
  }
}
