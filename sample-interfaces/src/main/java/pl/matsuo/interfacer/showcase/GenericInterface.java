package pl.matsuo.interfacer.showcase;

import java.util.List;

public interface GenericInterface<E> {

  E getSingleton();

  List<E> getList();
}
