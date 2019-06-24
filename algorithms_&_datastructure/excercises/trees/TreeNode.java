package com.jr.test.algorithms.trees;

public class TreeNode {

    private String value;
    private TreeNode leftChild;
    private TreeNode rightChild;

    public TreeNode(String value, TreeNode leftChild, TreeNode rightChild) {
        this.value = value;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TreeNode getLeftChild() {
        return leftChild;
    }

    public void setLeftChild(TreeNode leftChild) {
        this.leftChild = leftChild;
    }

    public TreeNode getRightChild() {
        return rightChild;
    }

    public void setRightChild(TreeNode rightChild) {
        this.rightChild = rightChild;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append(" value=").append(value);
        sb.append(", leftChild=").append(leftChild);
        sb.append(", rightChild=").append(rightChild);
        sb.append("]");
        return sb.toString();
    }
}
