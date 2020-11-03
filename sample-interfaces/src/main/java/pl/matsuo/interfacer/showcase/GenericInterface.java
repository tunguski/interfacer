package pl.matsuo.interfacer.showcase;

import java.util.List;

public interface GenericInterface<E> {

  E getSingleton();

  void setSingleton(E value);

  List<E> getList();

  void setList(List<E> value);
}
