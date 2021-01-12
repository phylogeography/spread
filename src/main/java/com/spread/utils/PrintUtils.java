package com.spread.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class PrintUtils {

    public static <E> void printArray(E[] array) {
        for (E element : array) {
            System.out.printf("%s ", element);
        }
        System.out.println();
    }

    public static void printMap(Map<?, ?> map) {
        Iterator<?> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<?, ?> pairs = (Entry<?, ?>) it.next();
            System.out.println(pairs.getKey() + " = " + pairs.getValue());
        }
    }

}
