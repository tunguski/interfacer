package pl.matsuo.interfacer.core;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Comparator.comparing;
import static pl.matsuo.interfacer.core.CollectionUtil.anyMatch;
import static pl.matsuo.interfacer.core.CollectionUtil.filterMap;
import static pl.matsuo.interfacer.core.CollectionUtil.findFirst;
import static pl.matsuo.interfacer.core.CollectionUtil.map;

public class InterfacesAdder {

  private final Consumer<String> log;

  public InterfacesAdder(Consumer<String> log) {
    this.log = log;
  }

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

    log.accept("Start processing");

    ClasspathInterfacesScanner classpathScanner = new ClasspathInterfacesScanner(log);
    SourceInterfacesScanner sourceScanner = new SourceInterfacesScanner(log);
    try {
      AtomicBoolean modified = new AtomicBoolean(true);
      while (modified.get()) {
        modified.set(false);

        ClassLoader classLoader = classpathScanner.getCompileClassLoader(compileClasspathElements);

        CombinedTypeSolver combinedTypeSolver =
            createTypeSolver(scanDirectory, interfacesDirectory, classLoader);
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
        final SourceRoot source = new SourceRoot(scanDirectory.toPath(), parserConfiguration);

        List<IfcResolve> ifcs = new ArrayList<>();
        ifcs.addAll(classpathScanner.scanInterfacesFromClasspath(classLoader, interfacePackage));
        ifcs.addAll(sourceScanner.scanInterfacesFromSrc(parserConfiguration, interfacesDirectory));
        Comparator<IfcResolve> comparator = comparing(i -> -i.methods.size());
        ifcs.sort(comparator);

        for (ParseResult<CompilationUnit> parseResult : source.tryToParse()) {
          // Only deal with files without parse errors
          if (parseResult.isSuccessful()) {
            parseResult
                .getResult()
                .ifPresent(
                    cu -> {
                      // Do the actual logic
                      boolean modifiedState = addInterfaces(cu, ifcs, combinedTypeSolver);
                      modified.set(modified.get() || modifiedState);
                    });
          } else {
            log.accept("Parse failure for " + parseResult.getProblems());
          }
        }

        // save changes on disk
        source.saveAll();
      }
    } catch (IOException e) {
      throw new RuntimeException("Error reading from source directory", e);
    }
  }

  public CombinedTypeSolver createTypeSolver(
      File scanDirectory, File interfacesDirectory, ClassLoader classLoader) {
    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(new ClassLoaderTypeSolver(classLoader) {});
    combinedTypeSolver.add(new JavaParserTypeSolver(scanDirectory.toPath()));
    combinedTypeSolver.add(new JavaParserTypeSolver(interfacesDirectory.toPath()));
    return combinedTypeSolver;
  }

  public boolean addInterfaces(
      CompilationUnit compilationUnit,
      List<IfcResolve> ifcs,
      CombinedTypeSolver combinedTypeSolver) {
    AtomicBoolean modified = new AtomicBoolean();

    compilationUnit
        .getPrimaryType()
        .map(
            primaryType ->
                primaryType.isClassOrInterfaceDeclaration()
                    ? (ClassOrInterfaceDeclaration) primaryType
                    : null)
        .filter(declaration -> !declaration.isInterface())
        .ifPresent(
            declaration ->
                ifcs.forEach(
                    ifc ->
                        processDeclarationWithInterface(
                            combinedTypeSolver, modified, declaration, ifc)));

    return modified.get();
  }

  public void processDeclarationWithInterface(
      CombinedTypeSolver combinedTypeSolver,
      AtomicBoolean modified,
      ClassOrInterfaceDeclaration declaration,
      IfcResolve ifc) {

    List<MethodDeclaration> declarations =
        findMethodDeclarationsForInterface(combinedTypeSolver, declaration, ifc);

    ResolvedReferenceTypeDeclaration other =
        ifc.resolve != null
            ? ifc.resolve
            : new ReflectionInterfaceDeclaration(ifc.clazz, combinedTypeSolver);

    boolean canBeAssignedTo =
        anyMatch(
            declaration.resolve().getAncestors(),
            ancestor -> {
              try {
                return other.isAssignableBy(ancestor);
              } catch (RuntimeException e) {
                e.printStackTrace();
                return false;
              }
            });
    if (declarations.size() == ifc.methods.size() && !canBeAssignedTo) {
      log.accept(
          "Modifying the class: " + declaration.getFullyQualifiedName() + " with ifc " + ifc.name);
      // ClassOrInterfaceType type = getInterfaceType(ifc, declarations);
      declaration.addImplementedType(ifc.name);
      modified.set(true);
    }
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
    List<Optional<MethodDeclaration>> methodDeclarations =
        map(
            ifc.methods,
            methodDecl ->
                findFirst(
                    declaration.getMethodsBySignature(methodDecl.getName()),
                    method -> methodDecl.matches(method, combinedTypeSolver)));
    return filterMap(methodDeclarations, Optional::isPresent, Optional::get);
  }
}
