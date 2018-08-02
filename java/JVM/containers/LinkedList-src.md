```java
/**
 * Doubly-linked list implementation of the List and Deque interfaces. Implements all optional list operations, 
 * and permits all elements (including nul).
 *
 * All of the operations perform as could be expected for a doubly-linked list. Operations that index into the 
 * list will traverse the list from the beginning or the end, whichever is closer to the specified index.
 *
 * Note that this implementation is not synchronized. If multiple threads access a linked list concurrently, 
 * and at least one of the threads modifies the list structurally, it must be synchronized externally. (A 
 * structural modification is any operation that adds or deletes one or more elements; merely setting the value 
 * of an element is not a structural modification.) This is typically accomplished by synchronizing on some object 
 * that naturally encapsulates the list.
 *
 * If no such object exists, the list should be "wrapped" using the Collections.synchronizedList() method. 
 * This is best done at creation time, to prevent accidental unsynchronized access to the list:
 *   List list = Collections.synchronizedList(new LinkedList(...));
 *
 * The iterators returned by this class's iterator and listIterator methods are fail-fast: if the list is
 * structurally modified at any time after the iterator is created, in any way except through the Iterator's 
 * own remove() or add() methods, the iterator will throw a ConcurrentModificationException. Thus, in the face 
 * of concurrent modification, the iterator fails quickly and cleanly, rather than risking arbitrary, 
 * non-deterministic behavior at an undetermined time in the future.
 *
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it is, generally speaking, impossible 
 * to make any hard guarantees in the presence of unsynchronized concurrent modification. Fail-fast iterators
 * throw ConcurrentModificationException on a best-effort basis. Therefore, it would be wrong to write a program 
 * that depended on this exception for its correctness: the fail-fast behavior of iterators should be used only 
 * to detect bugs.
 */

public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
    transient int size = 0;

    /** 
     * Pointer to first node.
     * Invariant: (first == null && last == null) || (first.prev == null && first.item != null)
     */
    transient Node<E> first;

    /**
     * Pointer to last node.
     * Invariant: (first == null && last == null) || (last.next == null && last.item != null)
     */
    transient Node<E> last;

    // constructor omitted

    /** Links e as first element */
    private void linkFirst(E e) {
        final Node<E> f = first;
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        if (f == null)
            last = newNode;
        else
            f.prev = newNode;
        size++;
        modCount++;
    }

    /** Links e as last element */
    void linkLast(E e) {
        final Node<E> l = last;
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;
        if (l == null)
            first = newNode;
        else
            l.next = newNode;
        size++;
        modCount++;
    }

    /** Inserts element e before non-null Node succ */
    void linkBefore(E e, Node<E> succ) {
        // assert succ != null;
        final Node<E> pred = succ.prev;
        final Node<E> newNode = new Node<>(pred, e, succ);
        succ.prev = newNode;
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        size++;
        modCount++;
    }

    /** Unlinks non-null first node f */
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        final E element = f.item;
        final Node<E> next = f.next;
        f.item = null;
        f.next = null; // help GC
        first = next;
        if (next == null)
            last = null;
        else
            next.prev = null;
        size--;
        modCount++;
        return element;
    }

    /** Unlinks non-null last node l */
    private E unlinkLast(Node<E> l) {
        // assert l == last && l != null;
        final E element = l.item;
        final Node<E> prev = l.prev;
        l.item = null;
        l.prev = null; // help GC
        last = prev;
        if (prev == null)
            first = null;
        else
            prev.next = null;
        size--;
        modCount++;
        return element;
    }

    /** Unlinks non-null node x */
    E unlink(Node<E> x) {
        // assert x != null;
        final E element = x.item;
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;

        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }

        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }

        x.item = null;
        size--;
        modCount++;
        return element;
    }

    /** Returns the first element in this list */
    public E getFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return f.item;
    }

    /** Returns the last element in this list */
    public E getLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return l.item;
    }

    /** Removes and returns the first element from this list */
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);
    }

    /** Removes and returns the last element from this list */
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return unlinkLast(l);
    }

    /** Inserts the specified element at the beginning of this list */
    public void addFirst(E e) {
        linkFirst(e);
    }

    /** Appends the specified element to the end of this list */
    public void addLast(E e) {
        linkLast(e);
    }

    /** Returns true if this list contains the specified element */
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    /** Returns the number of elements in this list */
    public int size() {
        return size;
    }

    /** Appends the specified element to the end of this list. This method is equivalent to addLast() */
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    /** Removes the first occurrence of the specified element from this list, if it is present. If this list 
     * does not contain the element, it is unchanged. More formally, removes the element with the lowest index
     * i such that (o == null ? get(i) == null : o.equals(get(i))) (if such an element exists). Returns true if 
     * this list contained the specified element (or equivalently, if this list changed as a result of the call) */
    public boolean remove(Object o) {
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    /** Appends all of the elements in the specified collection to the end of this list, in the order that 
     * they are returned by the specified collection's iterator. The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in progress. (Note that this will occur if the 
     * specified collection is this list, and it's nonempty.) */
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    /** Inserts all of the elements in the specified collection into this list, starting at the specified position. 
     * Shifts the element currently at that position (if any) and any subsequent elements to the right (increases 
     * their indices). The new elements will appear in the list in the order that they are returned by the
     * specified collection's iterator */
    public boolean addAll(int index, Collection<? extends E> c) {
        checkPositionIndex(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew == 0)
            return false;

        Node<E> pred, succ;
        if (index == size) {
            succ = null;
            pred = last;
        } else {
            succ = node(index);
            pred = succ.prev;
        }

        for (Object o : a) {
            @SuppressWarnings("unchecked") E e = (E) o;
            Node<E> newNode = new Node<>(pred, e, null);
            if (pred == null)
                first = newNode;
            else
                pred.next = newNode;
            pred = newNode;
        }

        if (succ == null) {
            last = pred;
        } else {
            pred.next = succ;
            succ.prev = pred;
        }

        size += numNew;
        modCount++;
        return true;
    }

    /** Removes all of the elements from this list. The list will be empty after this call returns */
    public void clear() {
        // Clearing all of the links between nodes is "unnecessary", but:
        // - helps a generational GC if the discarded nodes inhabit more than one generation
        // - is sure to free memory even if there is a reachable Iterator
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }


    // Positional Access Operations

    /** Returns the element at the specified position in this list */
    public E get(int index) {
        checkElementIndex(index);
        return node(index).item;
    }

    /** Replaces the element at the specified position in this list with the specified element */
    public E set(int index, E element) {
        checkElementIndex(index);
        Node<E> x = node(index);
        E oldVal = x.item;
        x.item = element;
        return oldVal;
    }

    /** Inserts the specified element at the specified position in this list. Shifts the element currently 
     * at that position (if any) and any subsequent elements to the right (adds one to their indices) */
    public void add(int index, E element) {
        checkPositionIndex(index);

        if (index == size)
            linkLast(element);
        else
            linkBefore(element, node(index));
    }

    /** Removes the element at the specified position in this list. Shifts any subsequent elements to the left 
     * (subtracts one from their indices). Returns the element that was removed from the list */
    public E remove(int index) {
        checkElementIndex(index);
        return unlink(node(index));
    }

    /** Tells if the argument is the index of an existing element */
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }
    private void checkElementIndex(int index) {
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /** Tells if the argument is the index of a valid position for an iterator or an add operation */
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }
    private void checkPositionIndex(int index) {
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /** Returns the (non-null) Node at the specified element index */
    Node<E> node(int index) {
        // assert isElementIndex(index);

        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }

    // Search Operations

    /** Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element */
    public int indexOf(Object o) {
        int index = 0;
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)
                    return index;
                index++;
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))
                    return index;
                index++;
            }
        }
        return -1;
    }

    /** Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element */
    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null)
                    return index;
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;
            }
        }
        return -1;
    }

    // Queue operations.

    /** Retrieves, but does not remove, the head (first element) of this list */
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    /** Retrieves, but does not remove, the head (first element) of this list */
    public E element() {
        return getFirst();
    }

    /** Retrieves and removes the head (first element) of this list */
    public E poll() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    /** Retrieves and removes the head (first element) of this list */
    public E remove() {
        return removeFirst();
    }

    /** Adds the specified element as the tail (last element) of this list */
    public boolean offer(E e) {
        return add(e);
    }

    // Deque operations
    /**
     * Inserts the specified element at the front of this list.
     *
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @since 1.6
     */
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this list.
     *
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @since 1.6
     */
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * Retrieves, but does not remove, the first element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the first element of this list, or {@code null}
     *         if this list is empty
     * @since 1.6
     */
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
     }

    /**
     * Retrieves, but does not remove, the last element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the last element of this list, or {@code null}
     *         if this list is empty
     * @since 1.6
     */
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }

    /**
     * Retrieves and removes the first element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the first element of this list, or {@code null} if
     *     this list is empty
     * @since 1.6
     */
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    /**
     * Retrieves and removes the last element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the last element of this list, or {@code null} if
     *     this list is empty
     * @since 1.6
     */
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }

    /**
     * Pushes an element onto the stack represented by this list.  In other
     * words, inserts the element at the front of this list.
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e the element to push
     * @since 1.6
     */
    public void push(E e) {
        addFirst(e);
    }

    /** Pops an element from the stack represented by this list. In other words, removes and returns the 
     * first element of this list. This method is equivalent to {@link #removeFirst() */
    public E pop() {
        return removeFirst();
    }

    /** Removes the first occurrence of the specified element in this list (when traversing the list from 
     * head to tail). If the list does not contain the element, it is unchanged */
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    /** Removes the last occurrence of the specified element in this list (when traversing the list from 
     * head to tail). If the list does not contain the element, it is unchanged */
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns a list-iterator of the elements in this list (in proper sequence), starting at the specified 
     * position in the list. Obeys the general contract of List.listIterator(int)
     *
     * The list-iterator is fail-fast: if the list is structurally modified at any time after the Iterator is 
     * created, in any way except through the list-iterator's own remove() or add() methods, the list-iterator 
     * will throw a ConcurrentModificationException. Thus, in the face of concurrent modification, the iterator 
     * fails quickly and cleanly, rather than risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future */
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);
        return new ListItr(index);
    }

    private class ListItr implements ListIterator<E> {
        private Node<E> lastReturned;
        private Node<E> next;
        private int nextIndex;
        private int expectedModCount = modCount;

        ListItr(int index) {
            // assert isPositionIndex(index);
            next = (index == size) ? null : node(index);
            nextIndex = index;
        }

        public boolean hasNext() {
            return nextIndex < size;
        }

        public E next() {
            checkForComodification();
            if (!hasNext())
                throw new NoSuchElementException();

            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }

        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        public E previous() {
            checkForComodification();
            if (!hasPrevious())
                throw new NoSuchElementException();

            lastReturned = next = (next == null) ? last : next.prev;
            nextIndex--;
            return lastReturned.item;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex - 1;
        }

        public void remove() {
            checkForComodification();
            if (lastReturned == null)
                throw new IllegalStateException();

            Node<E> lastNext = lastReturned.next;
            unlink(lastReturned);
            if (next == lastReturned)
                next = lastNext;
            else
                nextIndex--;
            lastReturned = null;
            expectedModCount++;
        }

        public void set(E e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            checkForComodification();
            lastReturned.item = e;
        }

        public void add(E e) {
            checkForComodification();
            lastReturned = null;
            if (next == null)
                linkLast(e);
            else
                linkBefore(e, next);
            nextIndex++;
            expectedModCount++;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            while (modCount == expectedModCount && nextIndex < size) {
                action.accept(next.item);
                lastReturned = next;
                next = next.next;
                nextIndex++;
            }
            checkForComodification();
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    /** Adapter to provide descending iterators via ListItr.previous */
    private class DescendingIterator implements Iterator<E> {
        private final ListItr itr = new ListItr(size());
        public boolean hasNext() {
            return itr.hasPrevious();
        }
        public E next() {
            return itr.previous();
        }
        public void remove() {
            itr.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedList<E> superClone() {
        try {
            return (LinkedList<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /** Returns a shallow copy of this LinkedList. (The elements themselves are not cloned.) */
    public Object clone() {
        LinkedList<E> clone = superClone();

        // Put clone into "virgin" state
        clone.first = clone.last = null;
        clone.size = 0;
        clone.modCount = 0;

        // Initialize clone with our elements
        for (Node<E> x = first; x != null; x = x.next)
            clone.add(x.item);

        return clone;
    }

    /** Returns an array containing all of the elements in this list in proper sequence (from first to last element).
     *
     * The returned array will be "safe" in that no references to it are maintained by this list.  (In other words, 
     * this method must allocate a new array). The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based */
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        return result;
    }

    /** Returns an array containing all of the elements in this list in proper sequence (from first to last element); 
     * the runtime type of the returned array is that of the specified array. If the list fits in the specified 
     * array, it is returned therein. Otherwise, a new array is allocated with the runtime type of the specified 
     * array and the size of this list.
     *
     * If the list fits in the specified array with room to spare (i.e., the array has more elements than the list), 
     * the element in the array immediately following the end of the list is set to null. (This is useful in 
     * determining the length of the list only if the caller knows that the list does not contain any null elements.)
     *
     * Like the toArray() method, this method acts as bridge between array-based and collection-based APIs. Further, 
     * this method allows precise control over the runtime type of the output array, and may, under certain 
     * circumstances, be used to save allocation costs.
     *
     * Suppose x is a list known to contain only strings. The following code can be used to dump the list into a 
     * newly allocated array of String:
     *     String[] y = x.toArray(new String[0]);
     *
     * Note that toArray(new Object[0]) is identical in function to toArray() */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            a = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;

        if (a.length > size)
            a[size] = null;

        return a;
    }

    /** Creates a late-binding and fail-fast Spliterator over the elements in this list.
     * The Spliterator reports Spliterator.SIZED and Spliterator.ORDERED. */
    @Override
    public Spliterator<E> spliterator() {
        return new LLSpliterator<E>(this, -1, 0);
    }

    /** A customized variant of Spliterators.IteratorSpliterator */
    static final class LLSpliterator<E> implements Spliterator<E> {
        static final int BATCH_UNIT = 1 << 10;  // batch array size increment
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedList<E> list; // null OK unless traversed
        Node<E> current;      // current node; null until initialized
        int est;              // size estimate; -1 until first needed
        int expectedModCount; // initialized when est set
        int batch;            // batch size for splits

        LLSpliterator(LinkedList<E> list, int est, int expectedModCount) {
            this.list = list;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEst() {
            int s; // force initialization
            final LinkedList<E> lst;
            if ((s = est) < 0) {
                if ((lst = list) == null)
                    s = est = 0;
                else {
                    expectedModCount = lst.modCount;
                    current = lst.first;
                    s = est = lst.size;
                }
            }
            return s;
        }

        public long estimateSize() { return (long) getEst(); }

        public Spliterator<E> trySplit() {
            Node<E> p;
            int s = getEst();
            if (s > 1 && (p = current) != null) {
                int n = batch + BATCH_UNIT;
                if (n > s)
                    n = s;
                if (n > MAX_BATCH)
                    n = MAX_BATCH;
                Object[] a = new Object[n];
                int j = 0;
                do { a[j++] = p.item; } while ((p = p.next) != null && j < n);
                current = p;
                batch = j;
                est = s - j;
                return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p; int n;
            if (action == null) throw new NullPointerException();
            if ((n = getEst()) > 0 && (p = current) != null) {
                current = null;
                est = 0;
                do {
                    E e = p.item;
                    p = p.next;
                    action.accept(e);
                } while (p != null && --n > 0);
            }
            if (list.modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            if (getEst() > 0 && (p = current) != null) {
                --est;
                E e = p.item;
                current = p.next;
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }
}
```