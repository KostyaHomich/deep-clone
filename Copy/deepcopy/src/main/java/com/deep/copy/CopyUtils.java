package com.deep.copy;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CopyUtils {

    public static <T> T deepCopy(T original) {
        return deepCopyRecursive(original, new IdentityHashMap<Object, Object>());
    }

    private static <T> T deepCopyRecursive(T original, Map<Object, Object> copies) {
        if (original == null) {
            return null;
        }

        // Check if already copied
        if (copies.containsKey(original)) {
            return (T) copies.get(original);
        }

        Class<?> clazz = original.getClass();

        // Handle immutable types
        if (isImmutable(clazz)) {
            copies.put(original, original);
            return original;
        }

        // Handle arrays
        if (clazz.isArray()) {
            int length = Array.getLength(original);
            Object arrayCopy = Array.newInstance(clazz.getComponentType(), length);
            copies.put(original, arrayCopy);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(original, i);
                Object copiedElement = deepCopyRecursive(element, copies);
                Array.set(arrayCopy, i, copiedElement);
            }
            return (T) arrayCopy;
        }

        // Handle collections
        if (original instanceof Collection) {
            Collection<?> originalCollection = (Collection<?>) original;
            Collection<Object> collectionCopy = createCollectionInstance(originalCollection.getClass());
            copies.put(original, collectionCopy);
            for (Object element : originalCollection) {
                Object copiedElement = deepCopyRecursive(element, copies);
                collectionCopy.add(copiedElement);
            }
            return (T) collectionCopy;
        }

        // Handle maps
        if (original instanceof Map) {
            Map<?, ?> originalMap = (Map<?, ?>) original;
            Map<Object, Object> mapCopy = createMapInstance(originalMap.getClass());
            copies.put(original, mapCopy);
            for (Map.Entry<?, ?> entry : originalMap.entrySet()) {
                Object keyCopy = deepCopyRecursive(entry.getKey(), copies);
                Object valueCopy = deepCopyRecursive(entry.getValue(), copies);
                mapCopy.put(keyCopy, valueCopy);
            }
            return (T) mapCopy;
        }

        // Handle other objects
        Object copy = createInstance(clazz);
        copies.put(original, copy);

        for (Field field : getAllFields(clazz)) {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(original);
                Object copiedValue = deepCopyRecursive(fieldValue, copies);
                field.set(copy, copiedValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error accessing field: " + field.getName(), e);
            }
        }

        return (T) copy;
    }

    private static boolean isImmutable(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == String.class || isWrapperType(clazz);
    }

    private static boolean isWrapperType(Class<?> clazz) {
        return clazz == Integer.class || clazz == Long.class ||
                clazz == Double.class || clazz == Float.class ||
                clazz == Boolean.class || clazz == Character.class ||
                clazz == Byte.class || clazz == Short.class;
    }

    private static <C extends Collection> C createCollectionInstance(Class<C> clazz) {
        try {
            Constructor<C> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            try {
                // Try to create using a constructor that takes a Collection
                Constructor<C> constructor = clazz.getDeclaredConstructor(Collection.class);
                constructor.setAccessible(true);
                return constructor.newInstance(Collections.emptyList());
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("No suitable constructor found for collection " + clazz.getName(), ex);
            } catch (Exception ex) {
                throw new RuntimeException("Error creating collection instance", ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating collection instance", e);
        }
    }

    private static <M extends Map> M createMapInstance(Class<M> clazz) {
        try {
            Constructor<M> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            try {
                // Try to create using a constructor that takes a Map
                Constructor<M> constructor = clazz.getDeclaredConstructor(Map.class);
                constructor.setAccessible(true);
                return constructor.newInstance(Collections.emptyMap());
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("No suitable constructor found for map " + clazz.getName(), ex);
            } catch (Exception ex) {
                throw new RuntimeException("Error creating map instance", ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating map instance", e);
        }
    }

    private static Object createInstance(Class<?> clazz) {
        try {
            // Try no-arg constructor first
            Constructor<?> noArgConstructor = clazz.getDeclaredConstructor();
            noArgConstructor.setAccessible(true);
            return noArgConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            // Try other constructors with default values
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                try {
                    constructor.setAccessible(true);
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    Object[] params = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        params[i] = getDefaultValue(paramTypes[i]);
                    }
                    return constructor.newInstance(params);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                    // Try next constructor
                }
            }
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error creating instance of " + clazz.getName(), e);
        }
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) return false;
            else if (type == char.class) return '\0';
            else if (type == byte.class) return (byte) 0;
            else if (type == short.class) return (short) 0;
            else if (type == int.class) return 0;
            else if (type == long.class) return 0L;
            else if (type == float.class) return 0.0f;
            else if (type == double.class) return 0.0d;
        }
        return null;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    // Example class and main method for demonstration
    static class Man {
        private String name;
        private int age;
        private List<String> favoriteBooks;

        public Man(String name, int age, List<String> favoriteBooks) {
            this.name = name;
            this.age = age;
            this.favoriteBooks = favoriteBooks;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public List<String> getFavoriteBooks() {
            return favoriteBooks;
        }

        public void setFavoriteBooks(List<String> favoriteBooks) {
            this.favoriteBooks = favoriteBooks;
        }
    }

    public static void main(String[] args) {
        // Test with Man class
        List<String> books = new ArrayList<>(Arrays.asList("Book1", "Book2"));
        Man originalMan = new Man("Alice", 30, books);
        Man copiedMan = CopyUtils.deepCopy(originalMan);

        System.out.println("Original and copy are different objects: " + (originalMan != copiedMan));
        System.out.println("Same name: " + originalMan.getName().equals(copiedMan.getName()));
        System.out.println("Same age: " + (originalMan.getAge() == copiedMan.getAge()));
        System.out.println("Different favoriteBooks list: " + (originalMan.getFavoriteBooks() != copiedMan.getFavoriteBooks()));
        System.out.println("Same books content: " + originalMan.getFavoriteBooks().equals(copiedMan.getFavoriteBooks()));

        // Modify original's list
        originalMan.getFavoriteBooks().add("Book3");
        System.out.println("Original's book count after modification: " + originalMan.getFavoriteBooks().size());
        System.out.println("Copy's book count remains: " + copiedMan.getFavoriteBooks().size());

        // Test recursive structure
        class Node {
            Node next;
        }
        Node node = new Node();
        node.next = node;
        Node copiedNode = CopyUtils.deepCopy(node);
        System.out.println("Node copy is recursive: " + (copiedNode.next == copiedNode));
    }
}

