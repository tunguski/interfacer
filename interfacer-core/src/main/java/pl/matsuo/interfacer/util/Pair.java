package pl.matsuo.interfacer.util;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
public class Pair<E, F> {

  public final E key;
  public final F value;

  public static <E, F> Pair<E, F> pair(E key, F value) {
    return new Pair<>(key, value);
  }
}
