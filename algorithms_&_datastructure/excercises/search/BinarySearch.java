package com.jr.test.algorithms.search;

public class BinarySearch {

    public static int search(int[] arr, int n) {
        int low = 0;
        int high = arr.length;

        while (low <= high) {
            int mid = (low + high) / 2;
            if (arr[mid] == n) {
                return mid;
            } else if (arr[mid] > n) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return -1;
    }

}
