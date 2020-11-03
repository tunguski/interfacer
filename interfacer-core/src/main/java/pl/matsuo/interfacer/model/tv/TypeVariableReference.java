package pl.matsuo.interfacer.model.tv;

import com.github.javaparser.ast.type.TypeParameter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.TypeVariable;

@RequiredArgsConstructor
@Getter
public class TypeVariableReference {

  // if from class
  final TypeVariable typeVariable;
  // if from declaration
  final TypeParameter typeParameter;
}
