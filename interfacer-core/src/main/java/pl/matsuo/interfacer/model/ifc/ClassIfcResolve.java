package pl.matsuo.interfacer.model.ifc;

import static com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFactory.typeDeclarationFor;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static pl.matsuo.core.util.collection.CollectionUtil.map;
import static pl.matsuo.core.util.collection.CollectionUtil.toMap;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import pl.matsuo.interfacer.model.ref.MethodReference;
import pl.matsuo.interfacer.model.ref.ReflectionMethodReference;
import pl.matsuo.interfacer.model.tv.TypeVariableReference;

@ToString
@RequiredArgsConstructor
@Slf4j
public class ClassIfcResolve extends AbstractIfcResolve {

  final Class<?> clazz;
  final TypeSolver typeSolver;

  @Override
  public String getName() {
    return clazz.getName();
  }

  @Override
  public String getGenericName(Map<String, String> typeParams) {
    if (clazz.getTypeParameters().length == 0) {
      return getName();
    } else {
      return getName()
          + "<"
          + join(", ", map(asList(clazz.getTypeParameters()), tp -> typeParams.get(tp.getName())))
          + ">";
    }
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

  @Override
  protected Map<String, TypeVariableReference> typeVariables() {
    return toMap(
        asList(clazz.getTypeParameters()),
        TypeVariable::getName,
        tp -> new TypeVariableReference(tp, null));
  }
}
