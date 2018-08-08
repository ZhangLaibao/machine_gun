```java
public class Arrays {

    /**
     * The minimum array length below which a parallel sorting algorithm will not further partition the 
     * sorting task. Using smaller sizes typically results in memory contention across tasks that makes 
     * parallel speedups unlikely.
     */
    private static final int MIN_ARRAY_SORT_GRAN = 1 << 13;
    
    /**
     * A comparator that implements the natural ordering of a group of mutually comparable elements. May be 
     * used when a supplied comparator is null. To simplify code-sharing within underlying implementations, 
     * the compare method only declares type Object for its second argument.
     *
     * Arrays class implementor's note: It is an empirical matter whether ComparableTimSort offers any 
     * performance benefit over TimSort used with this comparator. If not, you are better off deleting or 
     * bypassing ComparableTimSort. There is currently no empirical case for separating them for parallel 
     * sorting, so all public Object parallelSort methods use the same comparator based implementation.
     */
    static final class NaturalOrder implements Comparator<Object> {
        static final NaturalOrder INSTANCE = new NaturalOrder();
        @SuppressWarnings("unchecked")
        public int compare(Object first, Object second) {
            return ((Comparable<Object>)first).compareTo(second);
        }
    }
    
    /* Sorting of complex type arrays */
    
    // ==========================================各种排序============================================================
    /*
     * Sorting methods. Note that all public "sort" methods take the same form: Performing argument checks 
     * if necessary, and then expanding arguments into those required for the internal implementation methods 
     * residing in other package-private classes (except for legacyMergeSort, included in this class).
     */    
    
    /**
     * Sorts the specified array into ascending numerical order.
     *
     * The sorting algorithm is a Dual-Pivot Quicksort by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. 
     * This algorithm offers O(n log(n)) performance on many data sets that cause other quicksorts to degrade 
     * to quadratic performance, and is typically faster than traditional (one-pivot) Quicksort implementations.
     */
    public static void sort(int[] a) {
        DualPivotQuicksort.sort(a, 0, a.length - 1, null, 0, 0);
    }
    // overloaded methods omitted
    // ==========================================各种排序============================================================
    // ========================================各种并行排序============================================================
    /**
     * Sorts the specified array into ascending numerical order.
     *
     * The sorting algorithm is a parallel sort-merge that breaks the array into sub-arrays that are themselves 
     * sorted and then merged. When the sub-array length reaches a minimum granularity, the sub-array is sorted 
     * using the appropriate Arrays.sort() method. If the length of the specified array is less than the minimum
     * granularity, then it is sorted using the appropriate Arrays.sort method. The algorithm requires a working 
     * space no greater than the size of the original array. The ForkJoinPool.commonPool() is used to execute 
     * any parallel tasks.
     */
    public static void parallelSort(int[] a) {
        int n = a.length, p, g;
        if (n <= MIN_ARRAY_SORT_GRAN || (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
            DualPivotQuicksort.sort(a, 0, n - 1);
        else
            new ArraysParallelSortHelpers.FJInt.Sorter(null, a, new byte[n], 0, n, 0, 
                ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ? MIN_ARRAY_SORT_GRAN : g).invoke();
    }
    // ========================================各种并行排序============================================================
    

    /**
     * Sorts the specified array of objects into ascending order, according natural ordering of its elements. All 
     * elements in the array must implement the Comparable interface. Furthermore, all elements in the array must 
     * be mutually comparable(that is, e1.compareTo(e2) must not throw a ClassCastException for any elements e1
     * and e2 in the array).
     *
     * This sort is guaranteed to be stable: equal elements will not be reordered as a result of the sort.
     * This implementation is a stable, adaptive, iterative mergesort that requires far fewer than n lg(n) 
     * comparisons when the input array is partially sorted, while offering the performance of a traditional 
     * mergesort when the input array is randomly ordered. If the input array is nearly sorted, the implementation 
     * requires approximately n comparisons. Temporary storage requirements vary from a small constant for 
     * nearly sorted input arrays to n/2 object references for randomly ordered input arrays.
     * The implementation takes equal advantage of ascending and descending order in its input array, and can 
     * take advantage of ascending and descending order in different parts of the the same input array. It is 
     * well-suited to merging two or more sorted arrays: simply concatenate the arrays and sort the resulting array.
     */
    public static void sort(Object[] a) {
        ComparableTimSort.sort(a, 0, a.length, null, 0, 0);
    }
    
    /**
     * This implementation is a stable, adaptive, iterative mergesort that requires far fewer than n lg(n) 
     * comparisons when the input array is partially sorted, while offering the performance of a traditional 
     * mergesort when the input array is randomly ordered. If the input array is nearly sorted, the implementation 
     * requires approximately n comparisons. Temporary storage requirements vary from a small constant for 
     * nearly sorted input arrays to n/2 object references for randomly ordered input arrays.
     *
     * The implementation takes equal advantage of ascending and descending order in its input array, and can 
     * take advantage of ascending and descending order in different parts of the the same input array. It is 
     * well-suited to merging two or more sorted arrays: simply concatenate the arrays and sort the resulting array.
     */
    public static void sort(Object[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        ComparableTimSort.sort(a, fromIndex, toIndex, null, 0, 0);
    }

    // ====================================To be removed in a future release======================================
    /** Tuning parameter: list size at or below which insertion sort will be used in preference to mergesort */
    private static final int INSERTIONSORT_THRESHOLD = 7;

    /**
     * Src is the source array that starts at index 0; 
     * Dest is the (possibly larger) array destination with a possible offset; 
     * low is the index in dest to start sorting; 
     * high is the end index in dest to end sorting;
     * off is the offset to generate corresponding low, high in src;
     * To be removed in a future release.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void mergeSort(Object[] src, Object[] dest, int low, int high, int off) {
        int length = high - low;

        // Insertion sort on smallest arrays
        if (length < INSERTIONSORT_THRESHOLD) {
            for (int i=low; i<high; i++)
                for (int j=i; j>low && ((Comparable) dest[j-1]).compareTo(dest[j])>0; j--)
                    swap(dest, j, j-1);
                    swap(dest, j, j-1);
            return;
        }

        // Recursively sort halves of dest into src
        int destLow  = low;
        int destHigh = high;
        low  += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off);
        mergeSort(dest, src, mid, high, -off);

        // If list is already sorted, just copy from src to dest.  This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (((Comparable)src[mid-1]).compareTo(src[mid]) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        // Merge sorted halves (now in src) into dest
        for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && ((Comparable)src[p]).compareTo(src[q])<=0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }
 
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void mergeSort(Object[] src, Object[] dest, int low, int high, int off, Comparator c) {
        int length = high - low;

        // Insertion sort on smallest arrays
        if (length < INSERTIONSORT_THRESHOLD) {
            for (int i=low; i<high; i++)
                for (int j=i; j>low && c.compare(dest[j-1], dest[j])>0; j--)
                    swap(dest, j, j-1);
            return;
        }

        // Recursively sort halves of dest into src
        int destLow  = low;
        int destHigh = high;
        low  += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off, c);
        mergeSort(dest, src, mid, high, -off, c);

        // If list is already sorted, just copy from src to dest.  This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (c.compare(src[mid-1], src[mid]) <= 0) {
           System.arraycopy(src, low, dest, destLow, length);
           return;
        }

        // Merge sorted halves (now in src) into dest
        for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }
    // ====================================To be removed in a future release======================================

    // Parallel prefix

    /**
     * Cumulates, in parallel, each element of the given array in place, using the supplied function. For example 
     * if the array initially holds [2, 1, 0, 3] and the operation performs addition, then upon return the array 
     * holds [2, 3, 3, 6]. Parallel prefix computation is usually more efficient than sequential loops for large arrays
     */
    public static <T> void parallelPrefix(T[] array, BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        if (array.length > 0)
            new ArrayPrefixHelpers.CumulateTask<>(null, op, array, 0, array.length).invoke();
    }
    
    // Searching
    /**
     * Searches the specified array for the specified object using the binary search algorithm. The array must 
     * be sorted into ascending order according to the natural ordering of its elements (as by the sort(Object[])
     * method) prior to making this call. If it is not sorted, the results are undefined. (If the array contains 
     * elements that are not mutually comparable (for example, strings and integers), it cannot be sorted 
     * according to the natural ordering of its elements, hence results are undefined.) If the array contains 
     * multiple elements equal to the specified object, there is no guarantee which one will be found.
     */
    public static int binarySearch(Object[] a, Object key) {
        return binarySearch0(a, 0, a.length, key);
    }
    public static int binarySearch(Object[] a, int fromIndex, int toIndex, Object key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    // Like public version, but without range checks.
    private static int binarySearch0(Object[] a, int fromIndex, int toIndex, Object key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            @SuppressWarnings("rawtypes")
            Comparable midVal = (Comparable)a[mid];
            @SuppressWarnings("unchecked")
            int cmp = midVal.compareTo(key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Searches the specified array for the specified object using the binary search algorithm. The array must 
     * be sorted into ascending order according to the specified comparator (as by the sort(Object[], Comparator) 
     * method) prior to making this call. If it is not sorted, the results are undefined. If the array contains 
     * multiple elements equal to the specified object, there is no guarantee which one will be found.
     */
    public static <T> int binarySearch(T[] a, T key, Comparator<? super T> c) {
        return binarySearch0(a, 0, a.length, key, c);
    }
    public static <T> int binarySearch(T[] a, int fromIndex, int toIndex, T key, Comparator<? super T> c) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key, c);
    }

    // Like public version, but without range checks.
    private static <T> int binarySearch0(T[] a, int fromIndex, int toIndex, T key, Comparator<? super T> c) {
        if (c == null) 
            return binarySearch0(a, fromIndex, toIndex, key);
        
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = a[mid];
            int cmp = c.compare(midVal, key);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }
    
    // Filling

    /** Assigns the specified int value to each element of the specified array of ints */
    public static void fill(int[] a, int val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    public static void fill(int[] a, int fromIndex, int toIndex, int val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }  
    
    // Cloning

    /**
     * Copies the specified array, truncating or padding with nulls (if necessary) so the copy has the specified 
     * length. For all indices that are valid in both the original array and the copy, the two arrays will
     * contain identical values. For any indices that are valid in the copy but not the original, the copy will 
     * contain null. Such indices will exist if and only if the specified length is greater than that of the 
     * original array. The resulting array is of exactly the same class as the original array.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
    }

    public static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        @SuppressWarnings("unchecked")
        T[] copy = ((Object)newType == (Object)Object[].class) ? (T[]) new Object[newLength] 
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array. The initial index of the range (from) 
     * must lie between zero and original.length, inclusive. The value at original[from] is placed into the initial 
     * element of the copy (unless from == original.length or from == to). Values from subsequent elements in the 
     * original array are placed into subsequent elements in the copy. The final index of the range(to), which 
     * must be greater than or equal to from, may be greater than original.length, in which case null is placed 
     * in all elements of the copy whose index is greater than or equal to original.length - from. The length
     * of the returned array will be to - from.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        return copyOfRange(original, from, to, (Class<? extends T[]>) original.getClass());
    }

    public static <T,U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        @SuppressWarnings("unchecked")
        T[] copy = ((Object)newType == (Object)Object[].class) ? (T[]) new Object[newLength] 
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    // Misc

    /**
     * Returns a fixed-size list backed by the specified array. (Changes to the returned list "write through" 
     * to the array.) This method acts as bridge between array-based and collection-based APIs, in combination 
     * with Collection.toArray(). The returned list is serializable and implements RandomAccess.
     *
     * This method also provides a convenient way to create a fixed-size list initialized to contain several elements:
     *     List<String> stooges = Arrays.asList("Larry", "Moe", "Curly");
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> asList(T... a) {
        return new ArrayList<>(a);
    }
    
    /**
     * Set all elements of the specified array, using the provided generator function to compute each element. 
     * If the generator function throws an exception, it is relayed to the caller and the array is left in an 
     * indeterminate state.
     */
    public static <T> void setAll(T[] array, IntFunction<? extends T> generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++)
            array[i] = generator.apply(i);
    }

    /**
     * Set all elements of the specified array, in parallel, using the provided generator function to compute 
     * each element. If the generator function throws an exception, an unchecked exception is thrown from 
     * parallelSetAll and the array is left in an indeterminate state.
     */
    public static <T> void parallelSetAll(T[] array, IntFunction<? extends T> generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(i -> { array[i] = generator.apply(i); });
    }

    /**
     * Returns a Spliterator covering all of the specified array.
     * The spliterator reports Spliterator.SIZED, Spliterator.SUBSIZED, Spliterator.ORDERED, Spliterator.IMMUTABLE
     */
    public static <T> Spliterator<T> spliterator(T[] array) {
        return Spliterators.spliterator(array, Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    public static <T> Spliterator<T> spliterator(T[] array, int startInclusive, int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive, Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /** Returns a sequential Stream with the specified array as its source */
    public static <T> Stream<T> stream(T[] array) {
        return stream(array, 0, array.length);
    }

    /** Returns a sequential Stream with the specified range of the specified array as its source */
    public static <T> Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
        return StreamSupport.stream(spliterator(array, startInclusive, endExclusive), false);
    }

}
```
