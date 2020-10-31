package pl.matsuo.interfacer.core;

import com.github.javaparser.resolution.types.ResolvedType;

public class TypeWithName {

  final String name;

  final ResolvedType resolvedType;
  final Class<?> clazz;

  public TypeWithName(String name, ResolvedType resolvedType, Class<?> clazz) {
    this.name = name;
    this.resolvedType = resolvedType;
    this.clazz = clazz;
  }
}
