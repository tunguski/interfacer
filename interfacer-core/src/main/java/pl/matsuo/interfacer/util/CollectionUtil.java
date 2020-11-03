package pl.matsuo.interfacer.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public static <F, T, R> Map<T, R> toMap(
      Collection<? extends F> collection, Function<F, T> keyMapper, Function<F, R> valueMapper) {
    Map<T, R> result = new HashMap<>();

    for (F element : collection) {
      result.put(keyMapper.apply(element), valueMapper.apply(element));
    }

    return result;
  }

  public static <F, T> List<T> flatMap(
      Collection<? extends F> collection, Function<F, Collection<T>> mapper) {
    List<T> resultList = new ArrayList<>(collection.size());

    for (F element : collection) {
      resultList.addAll(mapper.apply(element));
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

  public static <E, F> F firstNotNull(Collection<E> collection, Function<E, F> mapper) {
    for (E element : collection) {
      F mapped = mapper.apply(element);
      if (mapped != null) {
        return mapped;
      }
    }

    return null;
  }

  public static <E> boolean anyMatch(Collection<E> collection, Predicate<E> condition) {
    for (E element : collection) {
      if (condition.test(element)) {
        return true;
      }
    }

    return false;
  }

  public static <E> boolean allMatch(Collection<E> collection, Predicate<E> condition) {
    for (E element : collection) {
      if (!condition.test(element)) {
        return false;
      }
    }

    return true;
  }
}
