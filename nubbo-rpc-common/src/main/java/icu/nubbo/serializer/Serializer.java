package icu.nubbo.serializer;

public interface Serializer {

    <T> byte[] serialize(T obj);

    <T> T deserializer (byte[] bytes, Class<T> clazz);
}
