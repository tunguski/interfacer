package test;

import java.util.ArrayList;
import java.util.List;

public class SampleNotGenericClass {

  // return type of this method breaks compatibility with GenericInterface
  public Integer getSingleton() {
    return 1;
  }

  public void setSingleton(String value) {
    // do nothing
  }

  public List<String> getList() {
    return new ArrayList<>();
  }

  public void setList(List<String> value) {
    // do nothing
  }
}
