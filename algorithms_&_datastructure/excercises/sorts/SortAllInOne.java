package com.jr.test.algorithms.sorts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * +============+==========================+========================+=======================+==================+==========+
 * | method     | time complexity(average) | time complexity(worst) | time complexity(best) | space complexity | stablity |
 * +============+==========================+========================+=======================+==================+==========+
 * | bubble     | o(n^2)                   | o(n^2)                 | o(n)                  | o(1)             | stable   |
 * +------------+--------------------------+------------------------+-----------------------+------------------+----------+
 * | selection  | o(n^2)                   | o(n^2)                 | o(n^2)                | o(1)             | unstable |
 * +------------+--------------------------+------------------------+-----------------------+------------------+----------+
 * | insertion  | o(n^2)                   | o(n^2)                 | o(n)                  | o(1)             | stable   |
 * +------------+--------------------------+------------------------+-----------------------+------------------+----------+
 * | shell      | o(n^1.3)                 | o(n^2)                 | o(n)                  | o(1)             | unstable |
 * +------------+--------------------------+------------------------+-----------------------+------------------+----------+
 * | merge
 * +------------+--------------------------+------------------------+-----------------------+------------------+----------+
 * | quick      | o(nlgn)                  | o(n^2)                 | o(nlgn)               | o(nlgn)          | unstable |
 * +------------+--------------------------+------------------------+-----------------------+------------------+----------+
 * | heap
 * +------------+--------------------------+------------------------+-----------------------+------------------+----------+
 * | counting   |
 * +------------+--------------------------+------------------------+-----------------------+------------------+----------+
 * | bucket
 * +------------+--------------------------+------------------------+-----------------------+------------------+----------+
 * | radix
 * +============+==========================+========================+=======================+==================+==========+
 */
public class SortAllInOne {

    // 冒泡排序
    public static void bubbleSort(int[] arr) {
        int len = arr.length;

        // 外层循环控制比较趟数，并不参与下标比较
        for (int i = 0; i < len - 1; i++) {

            // 内层循环控制比较范围，实现相邻元素比较和交换
            for (int j = 0; j < len - 1 - i; j++)
                if (arr[j] > arr[j + 1])
                    swap(arr, j, j + 1);
        }
    }

    // 快速排序： 冒泡排序的升级版，简单快速排序的实现
    public static void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pivot = partition(arr, low, high);
            quickSort(arr, low, pivot - 1);
            quickSort(arr, pivot + 1, high);
        }
    }

    private static int partition(int[] arr, int low, int high) {
        // 选定基准，暂存
        int base = arr[low];

        while (low < high) {
            // 从右向左，直到找到比基准值小的元素，直接赋值给基准值所在位置
            while (low < high && arr[high] >= base) {
                high--;
            }
            arr[low] = arr[high];

            // 从左向右，直到找到比基准值大的元素，直接赋值给基准值所在位置
            while (low < high && arr[low] <= base) {
                low++;
            }
            arr[high] = arr[low];
        }

        // 最终将基准值还原到原始位置
        arr[low] = base;
        return low;
    }

    public static void insertionSort(int[] arr) {
        int len = arr.length;
        int curr, preInx;
        for (int i = 1; i < len; i++) {
            curr = arr[i];
            preInx = i - 1;
            while (preInx >= 0 && curr < arr[preInx]) {
                arr[preInx + 1] = arr[preInx--];
            }

            arr[preInx + 1] = curr;
        }
    }

    public static void shellSort(int[] arr) {

        int length = arr.length;

        for (int step = length / 2; step > 0; step /= 2) {
            for (int i = step; i < length; i++) {
                int j = i;
                int tmp = arr[j];

                while (j - step >= 0 && arr[j - step] > tmp) {
                    arr[j] = arr[j - step];
                    j -= step;
                }
                arr[j] = tmp;
            }
        }

    }

    public static void selectionSort(int[] arr) {

        int length = arr.length;

        int minIdx;
        for (int i = 0; i < length; i++) {

            minIdx = i;
            for (int j = i + 1; j < length; j++) {
                if (arr[minIdx] > arr[j]) {
                    minIdx = j;
                }
            }

            if (i != minIdx) {
                swap(arr, i, minIdx);
            }
        }
    }

    public static void heapSort(int[] arr) {
        // 1.构建大顶堆
        for (int i = arr.length / 2 - 1; i >= 0; i--) {
            // 从第一个非叶子结点从下至上，从右至左调整结构
            adjustHeap(arr, i, arr.length);
        }
        // 2.调整堆结构+交换堆顶元素与末尾元素
        for (int j = arr.length - 1; j > 0; j--) {
            // 将堆顶元素与末尾元素进行交换
            swap(arr, 0, j);
            // 重新对堆进行调整
            adjustHeap(arr, 0, j);
        }
    }

    private static void adjustHeap(int[] arr, int i, int length) {
        // 先取出当前元素i
        int temp = arr[i];

        // 从i结点的左子结点开始，也就是2i+1处开始
        for (int k = i * 2 + 1; k < length; k = k * 2 + 1) {
            // 如果左子结点小于右子结点，k指向右子结点
            if (k + 1 < length && arr[k] < arr[k + 1]) {
                k = k + 1;
            }

            // 如果子节点大于父节点，将子节点值赋给父节点（不用进行交换）
            if (arr[k] > temp) {
                arr[i] = arr[k];
                i = k;
            } else {
                break;
            }
        }
        // 将temp值放到最终的位置
        arr[i] = temp;
    }

    public static void mergeSort(int[] arr) {
        int[] tmp = new int[arr.length];
        doMergeSort(arr, 0, arr.length - 1, tmp);
    }

    private static void doMergeSort(int[] arr, int left, int right, int[] tmp) {
        if (left < right) {
            int divid = (left + right) / 2;
            doMergeSort(arr, divid, right, tmp);
            doMergeSort(arr, left, divid, tmp);
            merge(arr, left, divid, right, tmp);
        }
    }

    private static void merge(int[] arr, int left, int divid, int right, int[] tmp) {

        int i = left, j = divid + 1, t = 0;
        while (i <= divid && j <= right) {
            if (arr[i] <= arr[j]) {
                tmp[t++] = tmp[i++];
            } else {
                tmp[t++] = tmp[j++];
            }
        }

        while (i <= divid) {
            tmp[t++] = arr[i++];
        }

        while (j <= right) {
            tmp[t++] = arr[j++];
        }

        t = 0;
        while (left <= right) {
            arr[left++] = tmp[t++];
        }
    }

    public static void countSort(int[] arr) {
        int max = Arrays.stream(arr).max().getAsInt();
        int min = Arrays.stream(arr).min().getAsInt();

        int interval = max - min + 1;
        int length = arr.length;

        int[] tmp = new int[interval + 1];
        for (int i = 0; i < length; i++) {
            tmp[arr[i] - min + 1]++;
        }

        for (int i = 0; i < interval; i++) {
            tmp[i + 1] += tmp[i];
        }

        int[] aux = new int[length];
        for (int i = 0; i < length; i++) {
            aux[tmp[arr[i] - min]++] = arr[i];
        }

        for (int i = 0; i < length; i++) {
            arr[i] = aux[i];
        }

    }

    public static void bucketSort(int[] arr) {

        int max = Arrays.stream(arr).max().getAsInt();
        int min = Arrays.stream(arr).min().getAsInt();

        int length = arr.length;

        int bucketNum = (max - min) / length + 1;
        ArrayList[] bucketArr = new ArrayList[bucketNum];

        for (int i = 0; i < bucketNum; i++) {
            bucketArr[i] = new ArrayList<Integer>();
        }

        for (int i = 0; i < length; i++) {
            int num = (arr[i] - min) / length;
            bucketArr[num].add(arr[i]);
        }

        for (int i = 0; i < bucketNum; i++) {
            Collections.sort(bucketArr[i]);
        }

        int j = 0;
        for (int i = 0; i < bucketNum; i++) {
            ArrayList<Integer> bucket = bucketArr[i];
            for (Integer integer : bucket) {
                arr[j++] = integer;
            }
        }
    }

    //========================================
    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
