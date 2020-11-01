package pl.matsuo.interfacer.core;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@RequiredArgsConstructor
public class IfcResolve {

  final String name;

  final ResolvedReferenceTypeDeclaration resolve;
  final Class<?> clazz;

  final List<TypeWithName> methods = new ArrayList<>();
}
