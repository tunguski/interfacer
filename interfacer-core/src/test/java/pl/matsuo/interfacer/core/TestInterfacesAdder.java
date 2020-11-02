package pl.matsuo.interfacer.core;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import pl.matsuo.interfacer.showcase.HasName;

import java.io.File;
import java.net.URL;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@Slf4j
public class TestInterfacesAdder {

  @Test
  public void doTest() {
    ClasspathInterfacesScanner classpathInterfacesScanner = new ClasspathInterfacesScanner();

    IfcResolve genericInterface =
        classpathInterfacesScanner.processClassFromClasspath(HasName.class);

    Assert.assertNotNull(genericInterface);

    InterfacesAdder interfacesAdder = new InterfacesAdder();

    URL sampleClassResource = getClass().getResource("/classes/test/SampleClass.java");
    File scanDir = new File(sampleClassResource.getFile()).getParentFile();
    File interfacesDir =
        new File(getClass().getResource("/interfaces/test/SampleInterface.java").getFile())
            .getParentFile();

    CombinedTypeSolver typeSolver =
        interfacesAdder.createTypeSolver(scanDir, interfacesDir, getClass().getClassLoader());

    ParseResult<CompilationUnit> compilationUnitParseResult =
        interfacesAdder.parseFile(typeSolver, new File(sampleClassResource.getFile()));

    if (!compilationUnitParseResult.isSuccessful()) {
      log.info("" + compilationUnitParseResult.getProblems());
    }

    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        interfacesAdder.processAllFiles(
            typeSolver, asList(compilationUnitParseResult), asList(genericInterface));

    modifications.forEach(mod -> log.info(mod.toString()));

    assertEquals(1, modifications.size());
  }
}
