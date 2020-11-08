package pl.matsuo.interfacer.core;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.utils.SourceRoot;
import lombok.extern.slf4j.Slf4j;
import pl.matsuo.interfacer.model.ifc.IfcResolve;
import pl.matsuo.interfacer.model.ifc.TypeDeclarationIfcResolve;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Implements scanning sources for interfaces that should be used during interface adding. */
@Slf4j
public class SourceInterfacesScanner {

  /** Parse source classes and create {@link IfcResolve} for interfaces. */
  public List<IfcResolve> scanInterfacesFromSrc(
      ParserConfiguration parserConfiguration, File interfacesDirectory) {
    List<IfcResolve> ifcs = new ArrayList<>();

    // do not scan anything if source directory was not specified
    if (interfacesDirectory == null) {
      return ifcs;
    }

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
          log.warn("Parse failure for " + parseResult.getProblems());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error reading from source directory", e);
    }

    return ifcs;
  }

  /** Create {@link IfcResolve} for interface represented by <code>compilationUnit</code>. */
  public IfcResolve getIfcResolve(CompilationUnit compilationUnit) {
    return compilationUnit
        .getPrimaryType()
        .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
        .map(type -> (ClassOrInterfaceDeclaration) type)
        .filter(ClassOrInterfaceDeclaration::isInterface)
        .map(type -> new TypeDeclarationIfcResolve(compilationUnit, type))
        .orElse(null);
  }
}
