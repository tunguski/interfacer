package pl.matsuo.interfacer.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class CollectionUtil {

  public static <F, T> List<T> map(Collection<? extends F> collection, Function<F, T> mapper) {
    List<T> resultList = new ArrayList<>(collection.size());

    for (F element : collection) {
      resultList.add(mapper.apply(element));
    }

    return resultList;
  }

  public static <E> List<E> filter(Collection<E> collection, Predicate<E> condition) {
    List<E> resultList = new ArrayList<>(collection.size());

    for (E element : collection) {
      if (condition.test(element)) {
        resultList.add(element);
      }
    }

    return resultList;
  }

  public static <E, F> List<F> filterMap(
      Collection<E> collection, Predicate<E> condition, Function<E, F> mapper) {
    List<F> resultList = new ArrayList<>();

    for (E element : collection) {
      if (condition.test(element)) {
        resultList.add(mapper.apply(element));
      }
    }

    return resultList;
  }

  public static <E, F> List<F> filterMap(Collection<E> collection, Function<E, F> mapper) {
    List<F> resultList = new ArrayList<>();

    for (E element : collection) {
      F mapped = mapper.apply(element);
      if (mapped != null) {
        resultList.add(mapped);
      }
    }

    return resultList;
  }

  public static <E> Optional<E> findFirst(Collection<E> collection, Predicate<E> condition) {
    for (E element : collection) {
      if (condition.test(element)) {
        return of(element);
      }
    }

    return empty();
  }

  public static <E> boolean anyMatch(Collection<E> collection, Predicate<E> condition) {
    for (E element : collection) {
      if (condition.test(element)) {
        return true;
      }
    }

    return false;
  }
}
