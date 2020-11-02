package pl.matsuo.interfacer.core;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Pair<E, F> {

  public final E key;
  public final F value;

  public static <E, F> Pair<E, F> pair(E key, F value) {
    return new Pair<>(key, value);
  }
}
