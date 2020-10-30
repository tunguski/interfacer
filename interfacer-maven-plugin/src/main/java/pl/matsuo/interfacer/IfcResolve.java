package pl.matsuo.interfacer;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;

import java.util.ArrayList;
import java.util.List;

public class IfcResolve {

  final String name;

  final ResolvedReferenceTypeDeclaration resolve;
  final Class<?> clazz;

  final List<TypeWithName> methods = new ArrayList<>();

  public IfcResolve(String name, ResolvedReferenceTypeDeclaration resolve, Class<?> clazz) {
    this.name = name;
    this.resolve = resolve;
    this.clazz = clazz;
  }
}
