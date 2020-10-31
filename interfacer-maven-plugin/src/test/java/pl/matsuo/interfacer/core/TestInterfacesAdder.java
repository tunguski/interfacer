package pl.matsuo.interfacer.core;

import lombok.extern.log4j.Log4j;
import org.junit.Assert;
import org.junit.Test;
import pl.matsuo.interfacer.showcase.GenericInterface;

@Log4j
public class TestInterfacesAdder {

  @Test
  public void doTest() {
    ClasspathInterfacesScanner classpathInterfacesScanner =
        new ClasspathInterfacesScanner(log::info);
    InterfacesAdder interfacesAdder = new InterfacesAdder(log::info);

    IfcResolve genericInterface =
        classpathInterfacesScanner.processClassFromClasspath(GenericInterface.class);

    Assert.assertNotNull(genericInterface);

    // interfacesAdder.?
  }
}
