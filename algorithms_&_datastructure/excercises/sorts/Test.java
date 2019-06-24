package com.jr.test.algorithms.sorts;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by PengXianglong on 2018/8/7.
 */
public class Test {

    private static final int BOUND = 10;

    public static void main(String[] args) {
        int[] test = randomArr(100);
        System.out.println("Before: " + Arrays.toString(test));
//        SortAllInOne.bubbleSort(test);
//        SortAllInOne.quickSort(test, 0, test.length - 1);
//        SortAllInOne.insertionSort(test);
//        SortAllInOne.shellSort(test);
//        SortAllInOne.selectionSort(test);
//        SortAllInOne.heapSort(test);
//        SortAllInOne.mergeSort(test);
//        SortAllInOne.countSort(test);
//        SortAllInOne.bucketSort(test);
        System.out.println("After: " + Arrays.toString(test));
    }

    private static int[] randomArr(int len) {
        if (len < 1) throw new IllegalArgumentException();

        Random random = new Random();
        int[] arr = new int[len];
        for (int i = 0; i < len; i++)
            arr[i] = random.nextInt(BOUND);

        return arr;
    }

}
