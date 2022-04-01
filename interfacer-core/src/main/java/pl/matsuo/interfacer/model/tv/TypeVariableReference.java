package pl.matsuo.interfacer.model.tv;

import com.github.javaparser.ast.type.TypeParameter;
import java.lang.reflect.TypeVariable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TypeVariableReference {

  // if from class
  final TypeVariable typeVariable;
  // if from declaration
  final TypeParameter typeParameter;
}
