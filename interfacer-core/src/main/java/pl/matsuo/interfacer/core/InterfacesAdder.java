package pl.matsuo.interfacer.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFactory.typeDeclarationFor;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static pl.matsuo.interfacer.core.CollectionUtil.anyMatch;
import static pl.matsuo.interfacer.core.CollectionUtil.filterMap;
import static pl.matsuo.interfacer.core.CollectionUtil.findFirst;
import static pl.matsuo.interfacer.core.CollectionUtil.flatMap;
import static pl.matsuo.interfacer.core.CollectionUtil.map;
import static pl.matsuo.interfacer.core.Pair.pair;

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
            scanInterfaces(interfacesDirectory, interfacePackage, classLoader, parserConfiguration);

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
      ParserConfiguration parserConfiguration) {
    List<IfcResolve> ifcs = new ArrayList<>();
    ifcs.addAll(
        new ClasspathInterfacesScanner()
            .scanInterfacesFromClasspath(classLoader, interfacePackage));
    ifcs.addAll(
        new SourceInterfacesScanner()
            .scanInterfacesFromSrc(parserConfiguration, interfacesDirectory));
    Comparator<IfcResolve> comparator = comparing(i -> -i.methods.size());
    ifcs.sort(comparator);
    return ifcs;
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> processAllFiles(
      CombinedTypeSolver combinedTypeSolver,
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
                      return addInterfaces(cu, ifcs, combinedTypeSolver);
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
    combinedTypeSolver.add(new ClassLoaderTypeSolver(classLoader) {});
    combinedTypeSolver.add(new JavaParserTypeSolver(scanDirectory.toPath()));
    combinedTypeSolver.add(new JavaParserTypeSolver(interfacesDirectory.toPath()));
    return combinedTypeSolver;
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> addInterfaces(
      CompilationUnit compilationUnit,
      List<IfcResolve> ifcs,
      CombinedTypeSolver combinedTypeSolver) {
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
                    ifcs,
                    ifc -> processDeclarationWithInterface(combinedTypeSolver, declaration, ifc)))
        .orElse(emptyList());
  }

  public Pair<IfcResolve, ClassOrInterfaceDeclaration> processDeclarationWithInterface(
      CombinedTypeSolver combinedTypeSolver,
      ClassOrInterfaceDeclaration declaration,
      IfcResolve ifc) {

    List<MethodDeclaration> declarations =
        findMethodDeclarationsForInterface(combinedTypeSolver, declaration, ifc);

    ResolvedReferenceTypeDeclaration other =
        ifc.resolve != null ? ifc.resolve : typeDeclarationFor(ifc.clazz, combinedTypeSolver);

    boolean canBeAssignedTo =
        anyMatch(
            declaration.resolve().getAncestors(),
            ancestor -> {
              try {
                return other.isAssignableBy(ancestor);
              } catch (RuntimeException e) {
                // e.printStackTrace();
                return false;
              }
            });
    if (declarations.size() == ifc.methods.size() && !canBeAssignedTo) {
      log.info(
          "Modifying the class: " + declaration.getFullyQualifiedName() + " with ifc " + ifc.name);
      // ClassOrInterfaceType type = getInterfaceType(ifc, declarations);
      declaration.addImplementedType(ifc.name);
      return pair(ifc, declaration);
    }

    return null;
  }

  public ClassOrInterfaceType getInterfaceType(
      IfcResolve ifc, List<MethodDeclaration> declarations) {
    ClassOrInterfaceType classOrInterfaceType = new ClassOrInterfaceType(ifc.name);

    if (ifc.clazz != null && ifc.clazz.getTypeParameters().length > 0) {
      for (TypeVariable<? extends Class<?>> typeParameter : ifc.clazz.getTypeParameters()) {
        typeParameter.getName();
      }

    } else if (ifc.resolve != null && ifc.resolve.getTypeParameters().size() > 0) {
    }

    return classOrInterfaceType;
  }

  public List<MethodDeclaration> findMethodDeclarationsForInterface(
      CombinedTypeSolver combinedTypeSolver,
      ClassOrInterfaceDeclaration declaration,
      IfcResolve ifc) {
    log.info("Check methods for " + ifc.name);
    List<Optional<MethodDeclaration>> methodDeclarations =
        map(
            ifc.methods,
            methodDecl -> {
              String[] paramStringTypes = methodDecl.getParamStringTypes();
              List<MethodDeclaration> methodsBySignature =
                  declaration.getMethodsBySignature(methodDecl.getName(), paramStringTypes);
              log.info(
                  "Check method "
                      + methodDecl.getName()
                      + " with params "
                      + asList(methodDecl.getParamStringTypes())
                      + " similar methods "
                      + methodsBySignature);
              return findFirst(
                  methodsBySignature, method -> methodDecl.matches(method, combinedTypeSolver));
            });
    return filterMap(methodDeclarations, Optional::isPresent, Optional::get);
  }
}
