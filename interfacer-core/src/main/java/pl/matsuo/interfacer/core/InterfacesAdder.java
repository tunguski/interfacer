package pl.matsuo.interfacer.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.utils.SourceRoot;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import pl.matsuo.interfacer.model.ifc.IfcResolve;
import pl.matsuo.interfacer.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static pl.matsuo.interfacer.util.CollectionUtil.anyMatch;
import static pl.matsuo.interfacer.util.CollectionUtil.filterMap;
import static pl.matsuo.interfacer.util.CollectionUtil.flatMap;
import static pl.matsuo.interfacer.util.Pair.pair;

@Slf4j
public class InterfacesAdder {

  public void addInterfacesAllFiles(
      @NonNull File scanDirectory,
      File interfacesDirectory,
      String interfacePackage,
      List<String> compileClasspathElements) {

    if (interfacesDirectory == null
        && (interfacePackage == null || compileClasspathElements == null)) {
      throw new RuntimeException(
          "No interface source defined: interfacesDirectory "
              + interfacesDirectory
              + " interfacePackage "
              + interfacePackage
              + " compileClasspathElements "
              + compileClasspathElements);
    }

    log.info("Start processing");

    try {
      List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> allModifications = new ArrayList<>();
      while (true) {
        ParsingContext parsingContext =
            new ParsingContext(compileClasspathElements, scanDirectory, interfacesDirectory);

        final SourceRoot source =
            new SourceRoot(scanDirectory.toPath(), parsingContext.parserConfiguration);

        List<IfcResolve> ifcs =
            scanInterfaces(interfacesDirectory, interfacePackage, parsingContext);

        List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
            processAllFiles(source.tryToParse(), ifcs, parsingContext.javaParser);
        allModifications.addAll(modifications);

        // save changes on disk
        source.saveAll();

        if (modifications.isEmpty()) {
          break;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error reading from source directory", e);
    }
  }

  public ParseResult<CompilationUnit> parseFile(JavaParser javaParser, File file) {
    try {
      return javaParser.parse(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public List<IfcResolve> scanInterfaces(
      File interfacesDirectory, String interfacePackage, ParsingContext parsingContext) {
    List<IfcResolve> ifcs = new ArrayList<>();
    ifcs.addAll(
        new ClasspathInterfacesScanner()
            .scanInterfacesFromClasspath(
                parsingContext.classLoader, interfacePackage, parsingContext.typeSolver));
    ifcs.addAll(
        new SourceInterfacesScanner()
            .scanInterfacesFromSrc(parsingContext.parserConfiguration, interfacesDirectory));
    Comparator<IfcResolve> comparator = comparing(i -> -i.getMethods().size());
    ifcs.sort(comparator);
    return ifcs;
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> processAllFiles(
      List<ParseResult<CompilationUnit>> parseResults,
      List<IfcResolve> ifcs,
      JavaParser javaParser) {

    return flatMap(
        parseResults,
        parseResult -> {
          // Only deal with files without parse errors
          if (parseResult.isSuccessful()) {
            return parseResult
                .getResult()
                .map(
                    cu -> {
                      // Do the actual logic
                      return addInterfaces(cu, ifcs, javaParser);
                    })
                .orElse(emptyList());
          } else {
            log.warn("Parse failure for " + parseResult.getProblems());
            return emptyList();
          }
        });
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> addInterfaces(
      CompilationUnit compilationUnit, List<IfcResolve> ifcs, JavaParser javaParser) {
    return compilationUnit
        .getPrimaryType()
        .map(
            primaryType ->
                primaryType.isClassOrInterfaceDeclaration()
                    ? (ClassOrInterfaceDeclaration) primaryType
                    : null)
        .filter(declaration -> !declaration.isInterface())
        .map(
            declaration ->
                filterMap(
                    ifcs, ifc -> processDeclarationWithInterface(declaration, ifc, javaParser)))
        .orElse(emptyList());
  }

  public Pair<IfcResolve, ClassOrInterfaceDeclaration> processDeclarationWithInterface(
      ClassOrInterfaceDeclaration declaration, IfcResolve ifc, JavaParser javaParser) {

    Map<String, String> resolvedTypeVariables = ifc.matches(declaration);

    // if any of the declaration's ancestors is already assignable to ifc
    boolean canBeAssignedTo =
        anyMatch(
            declaration.resolve().getAncestors(),
            ancestor -> {
              try {
                return ifc.getResolvedTypeDeclaration().isAssignableBy(ancestor);
              } catch (RuntimeException e) {
                // e.printStackTrace();
                return false;
              }
            });
    if (resolvedTypeVariables != null && !canBeAssignedTo) {
      log.info(
          "Modifying the class: "
              + declaration.getFullyQualifiedName()
              + " with ifc "
              + ifc.getName());

      ClassOrInterfaceType type = // new ClassOrInterfaceType(ifc.getName());
          javaParser
              .parseClassOrInterfaceType(ifc.getGenericName(resolvedTypeVariables))
              .getResult()
              .orElseThrow(() -> new RuntimeException(""));
      // type.setTypeArguments(ifc.getTypeArguments(resolvedTypeVariables));

      declaration.addImplementedType(type);
      return pair(ifc, declaration);
    }

    return null;
  }
}
