package com.jr.test.algorithms.trees;

public class BinarySearchTree<T extends Comparable<? super T>> {

    private BinaryNode<T> root;

    public BinarySearchTree() {
        root = null;
    }

    public void makeEmpty() {
        root = null;
    }

    public boolean isEmpty() {
        return root == null;
    }

    // =====================================================
    public void insert(T value) {
        root = insert(value, root);
    }

    private BinaryNode<T> insert(T value, BinaryNode<T> root) {
        if (null == root) return new BinaryNode<>(value, null, null);

        int i = value.compareTo(root.data);
        if (i < 0)
            root.left = insert(value, root.left);
        else if (i > 0)
            root.right = insert(value, root.right);

        return root;
    }
    // =====================================================

    // =====================================================
    public void remove(T value) {
        root = remove(value, root);
    }

    private BinaryNode<T> remove(T value, BinaryNode<T> root) {
        if (null == root) return null;

        int i = value.compareTo(root.data);
        if (i < 0)
            root.left = remove(value, root.left);
        else if (i > 0)
            root.right = remove(value, root.right);
        else {
            if (null != root.left && null != root.right) {
                root.data = findMin(root.right).data;
                root.right = remove(root.data, root.right);
            } else {
                root = root.left != null ? root.left : root.right;
            }
        }
        return root;
    }
    // =====================================================

    // =====================================================
    public void print() {
        print(root);
    }

    private void print(BinaryNode<T> root) {
        if (null != root) {
            print(root.left);
            System.out.println(root.data);
            print(root.right);
        }
    }
    // =====================================================

    // =====================================================
    public int height() {
        return height(root);
    }

    private int height(BinaryNode<T> root) {
        if (null == root)
            return 0;
        else
            return Math.max(height(root.left), height(root.right)) + 1;
    }
    // =====================================================

    // =====================================================
    public boolean contains(T nodeData) {
        return contains(nodeData, root);
    }

    private boolean contains(T nodeData, BinaryNode<T> root) {
        if (null == root) return false;

        int i = nodeData.compareTo(root.data);
        if (i == 0) return true;
        else if (i < 0) return contains(nodeData, root.left);
        else return contains(nodeData, root.right);
    }
    // =====================================================

    // =====================================================
    public T findMax() {
        if (isEmpty()) return null;
        return findMax(root).data;
    }

    private BinaryNode<T> findMax(BinaryNode<T> root) {
        if (null == root) return null;
        else if (null == root.right) return root;
        return findMax(root.right);
    }
    // =====================================================

    // =====================================================
    public T findMin() {
        if (isEmpty()) return null;
        return findMin(root).data;
    }

    private BinaryNode<T> findMin(BinaryNode<T> root) {
        if (null == root) return null;
        while (null != root.left)
            root = root.left;

        return root;
    }
    // =====================================================

    private static class BinaryNode<T> {

        T data;
        BinaryNode<T> left, right;

        public BinaryNode() {
        }

        public BinaryNode(T data, BinaryNode<T> left, BinaryNode<T> right) {
            this.data = data;
            this.left = left;
            this.right = right;
        }
    }

}
