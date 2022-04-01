package pl.matsuo.interfacer.showcase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import pl.matsuo.interfacer.avro.BasicKeyValue;
import pl.matsuo.interfacer.avro.GenericDate;
import pl.matsuo.interfacer.avro.GenericShape;
import pl.matsuo.interfacer.avro.GenericString;
import pl.matsuo.interfacer.avro.KeyValueReference;
import pl.matsuo.interfacer.avro.NamedRecord;
import pl.matsuo.interfacer.avro.NoInterfacesObject;

public class TestGeneratedResources {

  @Test
  public void checkImplementedTypes() {
    assertTrue(IKeyValue.class.isAssignableFrom(BasicKeyValue.class));
    assertTrue(IKeyValueProvider.class.isAssignableFrom(KeyValueReference.class));

    assertFalse(HasKey.class.isAssignableFrom(NoInterfacesObject.class));
    assertFalse(HasValue.class.isAssignableFrom(NoInterfacesObject.class));
    assertFalse(KeyValueReference.class.isAssignableFrom(NoInterfacesObject.class));
    assertFalse(IKeyValueProvider.class.isAssignableFrom(NoInterfacesObject.class));

    assertTrue(HasName.class.isAssignableFrom(NamedRecord.class));
    assertTrue(MutableOwner.class.isAssignableFrom(NamedRecord.class));
    assertTrue(MutableOwner2.class.isAssignableFrom(NamedRecord.class));
    assertFalse(MutableOwner3.class.isAssignableFrom(NamedRecord.class));

    assertTrue(GenericInterface.class.isAssignableFrom(GenericString.class));
    assertTrue(GenericInterface.class.isAssignableFrom(GenericShape.class));
    assertTrue(GenericInterface.class.isAssignableFrom(GenericDate.class));
  }
}
