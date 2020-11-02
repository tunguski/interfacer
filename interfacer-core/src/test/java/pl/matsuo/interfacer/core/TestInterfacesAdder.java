package pl.matsuo.interfacer.core;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import pl.matsuo.interfacer.model.ifc.IfcResolve;
import pl.matsuo.interfacer.showcase.GenericInterface;
import pl.matsuo.interfacer.showcase.HasName;
import pl.matsuo.interfacer.showcase.MutableOwner;
import pl.matsuo.interfacer.util.Pair;

import java.io.File;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class TestInterfacesAdder {

  @Test
  public void testHasName() {
    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        doTestInterface("/classes/test/SampleClass.java", HasName.class);
    assertEquals(1, modifications.size());
  }

  @Test
  @Ignore
  public void testGenericInterface() {
    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        doTestInterface("/classes/test/SampleClass.java", GenericInterface.class);
    assertEquals(1, modifications.size());
  }

  @Test
  public void testMutableOwner() {
    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        doTestInterface("/classes/test/SampleMutableClass.java", MutableOwner.class);
    assertEquals(1, modifications.size());
  }

  public static File fileForResource(String resourcePath) {
    return new File(TestInterfacesAdder.class.getResource(resourcePath).getFile());
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> doTestInterface(
      String className, Class<?> ifc) {
    InterfacesAdder interfacesAdder = new InterfacesAdder();

    File sampleClassFile = fileForResource(className);
    File scanDir = sampleClassFile.getParentFile();

    CombinedTypeSolver typeSolver =
        interfacesAdder.createTypeSolver(scanDir, scanDir, getClass().getClassLoader());

    ClasspathInterfacesScanner interfacesScanner = new ClasspathInterfacesScanner();
    IfcResolve genericInterface = interfacesScanner.processClassFromClasspath(ifc, typeSolver);

    assertNotNull(genericInterface);

    ParseResult<CompilationUnit> compilationUnitParseResult =
        interfacesAdder.parseFile(typeSolver, sampleClassFile);

    if (!compilationUnitParseResult.isSuccessful()) {
      log.info("" + compilationUnitParseResult.getProblems());
    }

    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        interfacesAdder.processAllFiles(
            typeSolver, asList(compilationUnitParseResult), asList(genericInterface));

    modifications.forEach(mod -> log.info(mod.toString()));

    return modifications;
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> doTestInterface(
      String className, File interfacesDir) {
    SourceInterfacesScanner interfacesScanner = new SourceInterfacesScanner();

    List<IfcResolve> ifcResolves = interfacesScanner.scanInterfacesFromSrc(null, interfacesDir);
    assertFalse(ifcResolves.isEmpty());

    InterfacesAdder interfacesAdder = new InterfacesAdder();

    File sampleClassFile = fileForResource(className);
    File scanDir = sampleClassFile.getParentFile();

    CombinedTypeSolver typeSolver =
        interfacesAdder.createTypeSolver(scanDir, interfacesDir, getClass().getClassLoader());

    ParseResult<CompilationUnit> compilationUnitParseResult =
        interfacesAdder.parseFile(typeSolver, sampleClassFile);

    if (!compilationUnitParseResult.isSuccessful()) {
      log.info("" + compilationUnitParseResult.getProblems());
    }

    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        interfacesAdder.processAllFiles(
            typeSolver, asList(compilationUnitParseResult), ifcResolves);

    modifications.forEach(mod -> log.info(mod.toString()));

    return modifications;
  }
}
