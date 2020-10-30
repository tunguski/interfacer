package pl.matsuo.interfacer.showcase;

import org.junit.Test;
import pl.matsuo.interfacer.avro.BasicKeyValue;
import pl.matsuo.interfacer.avro.KeyValueReference;
import pl.matsuo.interfacer.avro.NoInterfacesObject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestGeneratedResources {

  @Test
  public void checkImplementedTypes() {
    assertTrue(IKeyValue.class.isAssignableFrom(BasicKeyValue.class));
    assertTrue(IKeyValueProvider.class.isAssignableFrom(KeyValueReference.class));

    assertFalse(NoInterfacesObject.class.isAssignableFrom(HasKey.class));
    assertFalse(NoInterfacesObject.class.isAssignableFrom(HasValue.class));
    assertFalse(NoInterfacesObject.class.isAssignableFrom(KeyValueReference.class));
    assertFalse(NoInterfacesObject.class.isAssignableFrom(IKeyValueProvider.class));
  }
}
