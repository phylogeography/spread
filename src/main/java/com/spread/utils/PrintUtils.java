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

    public static <E> void print2DArray(E[][] array) {
        int nRow = array.length;
        int nCol = array[0].length;
        for (int row = 0; row < nRow; row++) {
            for (int col = 0; col < nCol; col++) {
                System.out.print(array[row][col] + " ");
            }
            System.out.print("\n");
        }
    }// END: print2DArray

    public static void printMap(Map<?, ?> map) {
        Iterator<?> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<?, ?> pairs = (Entry<?, ?>) it.next();
            System.out.println(pairs.getKey() + " = " + pairs.getValue());
        }
    }

}
