package com.spread.utils;

public class PrintUtils {

    public static <E> void printArray(E[] array) {
        for (E element : array) {
            System.out.printf("%s ", element);
        }
        System.out.println();
    }

}
