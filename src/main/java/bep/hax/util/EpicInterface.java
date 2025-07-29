package bep.hax.util;

@FunctionalInterface
public interface EpicInterface<T, E> {
    E get(T t);
}
