package de.diddiz.LogBlock.blockstate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Stores PDC primitives, not the plugin-specific adapters or server objects. */
final class PersistentDataContainerCodec {
    static final String KEY = "persistentData";
    // Each nested container adds a YAML sequence and mapping. Stay below Bukkit's
    // YAML nesting limit (100), and bound probing of unrecognised nested lists.
    private static final int MAX_DEPTH = 32;
    private static final List<ValueType> PRIMITIVES = List.of(
            new ValueType("BYTE", PersistentDataType.BYTE, null),
            new ValueType("SHORT", PersistentDataType.SHORT, null),
            new ValueType("INTEGER", PersistentDataType.INTEGER, null),
            new ValueType("LONG", PersistentDataType.LONG, null),
            new ValueType("FLOAT", PersistentDataType.FLOAT, null),
            new ValueType("DOUBLE", PersistentDataType.DOUBLE, null),
            new ValueType("STRING", PersistentDataType.STRING, null),
            new ValueType("BYTE_ARRAY", PersistentDataType.BYTE_ARRAY, null),
            new ValueType("INTEGER_ARRAY", PersistentDataType.INTEGER_ARRAY, null),
            new ValueType("LONG_ARRAY", PersistentDataType.LONG_ARRAY, null),
            new ValueType("TAG_CONTAINER", PersistentDataType.TAG_CONTAINER, null));

    private PersistentDataContainerCodec() {
    }

    static List<Map<String, Object>> serialize(PersistentDataContainer container) {
        return serialize(container, 0);
    }

    private static List<Map<String, Object>> serialize(PersistentDataContainer container, int depth) {
        checkDepth(depth);
        List<Map<String, Object>> entries = new ArrayList<>();
        for (NamespacedKey key : container.getKeys().stream().sorted(Comparator.comparing(NamespacedKey::toString)).toList()) {
            Map<String, Object> entry = readEntry(container, key, depth);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static Map<String, Object> readEntry(PersistentDataContainer container, NamespacedKey key, int depth) {
        List<ValueType> candidates = PRIMITIVES;
        for (int listDepth = 0; listDepth + depth <= MAX_DEPTH; listDepth++) {
            for (ValueType type : candidates) {
                try {
                    if (container.has(key, type.apiType())) {
                        Object value = container.get(key, type.apiType());
                        if (value != null) {
                            // has() may only check the outer list tag. get() and
                            // encoding must succeed before accepting this type.
                            return Map.of("key", key.toString(), "type", type.id(), "value", encode(type, value, depth));
                        }
                    }
                } catch (IllegalArgumentException | ClassCastException | UnsupportedOperationException ignored) {
                    // Try another readable primitive representation.
                }
            }
            if (listDepth == 1) {
                try {
                    if (!container.has(key, PRIMITIVES.getFirst().list().list().apiType())) {
                        return null;
                    }
                } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                    return null;
                }
            }
            candidates = candidates.stream().map(ValueType::list).toList();
        }
        return null;
    }

    private static Object encode(ValueType type, Object value, int depth) {
        checkDepth(depth);
        switch (type.name()) {
            case "TAG_CONTAINER":
                return serialize((PersistentDataContainer) value, depth + 1);
            case "LIST": {
                List<Object> encoded = new ArrayList<>();
                for (Object element : (List<?>) value) {
                    encoded.add(encode(type.element(), element, depth + 1));
                }
                return encoded;
            }
            case "BYTE_ARRAY": {
                List<Byte> encoded = new ArrayList<>();
                for (byte element : (byte[]) value) {
                    encoded.add(element);
                }
                return encoded;
            }
            case "INTEGER_ARRAY": {
                List<Integer> encoded = new ArrayList<>();
                for (int element : (int[]) value) {
                    encoded.add(element);
                }
                return encoded;
            }
            case "LONG_ARRAY": {
                List<Long> encoded = new ArrayList<>();
                for (long element : (long[]) value) {
                    encoded.add(element);
                }
                return encoded;
            }
            default:
                return type.apiType().getPrimitiveType().cast(value);
        }
    }

    static void deserialize(PersistentDataContainer container, Object serialized) {
        List<?> entries = requireList(serialized);
        for (NamespacedKey key : new HashSet<>(container.getKeys())) {
            container.remove(key);
        }
        readEntries(container, entries, 0);
    }

    private static void readEntries(PersistentDataContainer container, List<?> entries, int depth) {
        checkDepth(depth);
        for (Object entry : entries) {
            try {
                if (!(entry instanceof Map<?, ?> values) || !(values.get("key") instanceof String keyString)
                        || !(values.get("type") instanceof String typeString)) {
                    continue;
                }
                NamespacedKey key = NamespacedKey.fromString(keyString);
                ValueType type = parseType(typeString);
                if (key != null && type != null) {
                    Object value = decode(type, values.get("value"), container.getAdapterContext(), depth);
                    set(container, key, type.apiType(), value);
                }
            } catch (IllegalArgumentException | ClassCastException | UnsupportedOperationException | ArithmeticException ignored) {
                // One unavailable or malformed value must not abort the block restore.
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void set(PersistentDataContainer container, NamespacedKey key, PersistentDataType type, Object value) {
        container.set(key, type, value);
    }

    private static Object decode(ValueType type, Object value, PersistentDataAdapterContext context, int depth) {
        checkDepth(depth);
        if (value == null) {
            throw new IllegalArgumentException("Missing PDC value");
        }
        return switch (type.name()) {
            case "BYTE" -> (byte) integer(value, Byte.MIN_VALUE, Byte.MAX_VALUE);
            case "SHORT" -> (short) integer(value, Short.MIN_VALUE, Short.MAX_VALUE);
            case "INTEGER" -> (int) integer(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
            case "LONG" -> integer(value, Long.MIN_VALUE, Long.MAX_VALUE);
            case "FLOAT" -> ((Number) value).floatValue();
            case "DOUBLE" -> ((Number) value).doubleValue();
            case "STRING" -> String.class.cast(value);
            case "BYTE_ARRAY" -> {
                List<?> values = requireList(value);
                byte[] result = new byte[values.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (byte) integer(values.get(i), Byte.MIN_VALUE, Byte.MAX_VALUE);
                }
                yield result;
            }
            case "INTEGER_ARRAY" -> {
                List<?> values = requireList(value);
                int[] result = new int[values.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (int) integer(values.get(i), Integer.MIN_VALUE, Integer.MAX_VALUE);
                }
                yield result;
            }
            case "LONG_ARRAY" -> {
                List<?> values = requireList(value);
                long[] result = new long[values.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = integer(values.get(i), Long.MIN_VALUE, Long.MAX_VALUE);
                }
                yield result;
            }
            case "TAG_CONTAINER" -> {
                PersistentDataContainer result = context.newPersistentDataContainer();
                readEntries(result, requireList(value), depth + 1);
                yield result;
            }
            case "LIST" -> {
                List<Object> result = new ArrayList<>();
                for (Object element : requireList(value)) {
                    // Do not compact a list when an element cannot be read.
                    result.add(decode(type.element(), element, context, depth + 1));
                }
                yield result;
            }
            default -> throw new IllegalArgumentException("Unknown PDC type " + type.name());
        };
    }

    private static long integer(Object value, long min, long max) {
        long result = switch (value) {
            case Byte number -> number.longValue();
            case Short number -> number.longValue();
            case Integer number -> number.longValue();
            case Long number -> number.longValue();
            case BigInteger number -> number.longValueExact();
            default -> throw new IllegalArgumentException("Expected an integral PDC value");
        };
        if (result < min || result > max) {
            throw new IllegalArgumentException("PDC value out of range");
        }
        return result;
    }

    private static List<?> requireList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Expected a PDC sequence");
    }

    private static ValueType parseType(String id) {
        int listDepth = 0;
        while (id.startsWith("LIST<") && id.endsWith(">")) {
            if (++listDepth > MAX_DEPTH) {
                return null;
            }
            id = id.substring(5, id.length() - 1);
        }
        for (ValueType primitive : PRIMITIVES) {
            if (primitive.name().equals(id)) {
                ValueType result = primitive;
                for (int i = 0; i < listDepth; i++) {
                    result = result.list();
                }
                return result;
            }
        }
        return null;
    }

    private static void checkDepth(int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("PDC nesting exceeds the supported YAML depth");
        }
    }

    private record ValueType(String name, PersistentDataType<?, ?> apiType, ValueType element) {
        ValueType list() {
            return new ValueType("LIST", PersistentDataType.LIST.listTypeFrom(apiType), this);
        }

        String id() {
            return element == null ? name : "LIST<" + element.id() + ">";
        }
    }
}
