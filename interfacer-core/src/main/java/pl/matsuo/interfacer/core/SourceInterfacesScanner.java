package pl.matsuo.interfacer.core;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SourceInterfacesScanner {

  private final Consumer<String> log;

  public SourceInterfacesScanner(Consumer<String> log) {
    this.log = log;
  }

  public List<IfcResolve> scanInterfacesFromSrc(
      ParserConfiguration parserConfiguration, File interfacesDirectory) {
    List<IfcResolve> ifcs = new ArrayList<>();

    final SourceRoot source = new SourceRoot(interfacesDirectory.toPath(), parserConfiguration);
    try {
      for (ParseResult<CompilationUnit> parseResult : source.tryToParse()) {
        // Only deal with files without parse errors
        if (parseResult.isSuccessful()) {
          parseResult
              .getResult()
              .ifPresent(
                  cu -> {
                    // Do the actual logic
                    IfcResolve ifcResolve = getIfcResolve(cu);
                    if (ifcResolve != null) {
                      ifcs.add(ifcResolve);
                    }
                  });
        } else {
          log.accept("Parse failure for " + parseResult.getProblems());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error reading from source directory", e);
    }

    return ifcs;
  }

  public IfcResolve getIfcResolve(CompilationUnit cu) {
    return cu.getPrimaryType()
        .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
        .map(type -> (ClassOrInterfaceDeclaration) type)
        .filter(ClassOrInterfaceDeclaration::isInterface)
        .map(
            type -> {
              log.accept("Adding interface: " + type.getNameAsString());
              IfcResolve ifcResolve =
                  new IfcResolve(
                      cu.getPackageDeclaration()
                              .map(packageDeclaration -> packageDeclaration.getNameAsString() + ".")
                              .orElse("")
                          + type.getNameAsString(),
                      type.resolve(),
                      null);

              type.resolve()
                  .getAllMethods()
                  .forEach(
                      method -> {
                        ResolvedMethodDeclaration methodDeclaration = method.getDeclaration();
                        if (methodDeclaration.getNumberOfParams() == 0
                            && methodDeclaration.getName().startsWith("get")
                            && !methodDeclaration.getName().equals("getClass")) {
                          log.accept("Adding method: " + methodDeclaration);
                          ifcResolve.methods.add(
                              new TypeWithName(
                                  methodDeclaration.getName(),
                                  methodDeclaration.getReturnType(),
                                  null));
                        }
                      });

              type.getMethods().forEach(methodDeclaration -> {});

              return ifcResolve;
            })
        .orElse(null);
  }
}
