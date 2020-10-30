package pl.matsuo.interfacer.showcase;

import org.junit.Assert;
import org.junit.Test;
import pl.matsuo.interfacer.avro.BasicKeyValue;
import pl.matsuo.interfacer.avro.KeyValueReference;

public class TestGeneratedResources {

  @Test
  public void checkImplementedTypes() {
    Assert.assertTrue(IKeyValue.class.isAssignableFrom(BasicKeyValue.class));
    Assert.assertTrue(IKeyValueProvider.class.isAssignableFrom(KeyValueReference.class));
  }
}
