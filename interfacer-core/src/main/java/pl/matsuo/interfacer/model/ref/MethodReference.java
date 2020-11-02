package pl.matsuo.interfacer.model.ref;

import com.github.javaparser.ast.body.MethodDeclaration;

public interface MethodReference {

  String getName();

  boolean matches(MethodDeclaration methodDeclaration);
}
