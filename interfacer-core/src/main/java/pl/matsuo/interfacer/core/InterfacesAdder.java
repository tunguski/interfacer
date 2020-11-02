package pl.matsuo.interfacer.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
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
        ClassLoader classLoader =
            ClasspathInterfacesScanner.getCompileClassLoader(compileClasspathElements);

        CombinedTypeSolver combinedTypeSolver =
            createTypeSolver(scanDirectory, interfacesDirectory, classLoader);
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
        final SourceRoot source = new SourceRoot(scanDirectory.toPath(), parserConfiguration);

        List<IfcResolve> ifcs =
            scanInterfaces(
                interfacesDirectory,
                interfacePackage,
                classLoader,
                parserConfiguration,
                combinedTypeSolver);

        List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
            processAllFiles(combinedTypeSolver, source.tryToParse(), ifcs);
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

  public ParseResult<CompilationUnit> parseString(
      CombinedTypeSolver combinedTypeSolver, String content) {
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    parserConfiguration.setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    return new JavaParser(parserConfiguration).parse(content);
  }

  public ParseResult<CompilationUnit> parseFile(CombinedTypeSolver combinedTypeSolver, File file) {
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    parserConfiguration.setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    try {
      return new JavaParser(parserConfiguration).parse(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public List<IfcResolve> scanInterfaces(
      File interfacesDirectory,
      String interfacePackage,
      ClassLoader classLoader,
      ParserConfiguration parserConfiguration,
      TypeSolver typeSolver) {
    List<IfcResolve> ifcs = new ArrayList<>();
    ifcs.addAll(
        new ClasspathInterfacesScanner()
            .scanInterfacesFromClasspath(classLoader, interfacePackage, typeSolver));
    ifcs.addAll(
        new SourceInterfacesScanner()
            .scanInterfacesFromSrc(parserConfiguration, interfacesDirectory));
    Comparator<IfcResolve> comparator = comparing(i -> -i.getMethods().size());
    ifcs.sort(comparator);
    return ifcs;
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> processAllFiles(
      TypeSolver typeSolver,
      List<ParseResult<CompilationUnit>> parseResults,
      List<IfcResolve> ifcs) {

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
                      return addInterfaces(cu, ifcs, typeSolver);
                    })
                .orElse(emptyList());
          } else {
            log.warn("Parse failure for " + parseResult.getProblems());
            return emptyList();
          }
        });
  }

  public CombinedTypeSolver createTypeSolver(
      File scanDirectory, File interfacesDirectory, ClassLoader classLoader) {
    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(new ClassLoaderTypeSolver(classLoader));
    combinedTypeSolver.add(new JavaParserTypeSolver(scanDirectory.toPath()));
    combinedTypeSolver.add(new JavaParserTypeSolver(interfacesDirectory.toPath()));
    return combinedTypeSolver;
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> addInterfaces(
      CompilationUnit compilationUnit, List<IfcResolve> ifcs, TypeSolver typeSolver) {
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
                filterMap(ifcs, ifc -> processDeclarationWithInterface(declaration, ifc)))
        .orElse(emptyList());
  }

  public Pair<IfcResolve, ClassOrInterfaceDeclaration> processDeclarationWithInterface(
      ClassOrInterfaceDeclaration declaration, IfcResolve ifc) {

    List<String> matches = ifc.matches(declaration);

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
    if (matches != null && !canBeAssignedTo) {
      log.info(
          "Modifying the class: "
              + declaration.getFullyQualifiedName()
              + " with ifc "
              + ifc.getName());
      // ClassOrInterfaceType type = getInterfaceType(ifc, declarations);
      declaration.addImplementedType(ifc.getName());
      return pair(ifc, declaration);
    }

    return null;
  }
}
