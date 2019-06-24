package com.jr.test.algorithms.trees;

public class AVLTree<T extends Comparable<? super T>> {

    private AVLNode<T> insert(T value, AVLNode<T> root) {
        if (null == root) return new AVLNode<>(value, null, null);

        int i = value.compareTo(root.data);
        if (i < 0)
            root.left = insert(value, root.left);
        else if (i > 0)
            root.right = insert(value, root.right);

        return balance(root);
    }

    private AVLNode<T> balance(AVLNode<T> root) {
        if (null == root) return null;

        if (height(root.left) - height(root.right) > 1) {
            if (height(root.left.left) >= height(root.left.right))
                root = rotateWithLeftChild(root);
            else
                root = doubleWithLeftChild(root);
        } else if (height(root.right) - height(root.left) > 1) {
            if (height(root.right.right) >= height(root.right.left))
                root = rotateWithRightChild(root);
            else
                root = doubleWithRightChild(root);
        }

        root.height = Math.max(height(root.left), height(root.right)) + 1;
        return root;
    }

    private AVLNode<T> doubleWithRightChild(AVLNode<T> root) {
        return null;
    }

    private AVLNode<T> rotateWithRightChild(AVLNode<T> root) {
        return null;
    }

    private AVLNode<T> doubleWithLeftChild(AVLNode<T> root) {
        root.left = rotateWithRightChild(root.left);
        return rotateWithLeftChild(root);
    }

    private AVLNode<T> rotateWithLeftChild(AVLNode<T> root) {
        AVLNode<T> left = root.left;
        root.left = left.right;
        left.right = root;
        root.height = Math.max(height(root.left), height(root.right)) + 1;
        left.height = Math.max(height(left.left), root.height) + 1;
        return left;
    }

    private int height(AVLNode root) {
        return null == root ? -1 : root.height;
    }

    private static class AVLNode<T> {

        int height;
        T data;
        AVLNode<T> left, right;

        public AVLNode() {
        }

        public AVLNode(T data, AVLNode<T> left, AVLNode<T> right) {
            this.data = data;
            this.left = left;
            this.right = right;
        }
    }

}
