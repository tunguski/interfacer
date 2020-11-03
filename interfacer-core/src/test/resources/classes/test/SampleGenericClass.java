package test;

import java.util.ArrayList;
import java.util.List;

public class SampleGenericClass {

  public String getSingleton() {
    return "singleton";
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
