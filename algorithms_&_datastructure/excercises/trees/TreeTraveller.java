package com.jr.test.algorithms.trees;

import java.util.LinkedList;
import java.util.Stack;

/**
 * Created by PengXianglong on 2019/4/15.
 */
public class TreeTraveller {

    public static void preOrder(TreeNode root) {
        if (null != root) {
            print(root);
            preOrder(root.getLeftChild());
            preOrder(root.getRightChild());
        }
    }

    public static void preOrderStack(TreeNode root) {
        Stack<TreeNode> stack = new Stack<>();
        if (root != null) {
            stack.push(root);
        }

        while (!stack.empty()) {

            TreeNode node = stack.pop();
            print(node);

            //右结点先入栈，左结点后入栈
            if (node.getRightChild() != null) stack.push(node.getRightChild());
            if (node.getLeftChild() != null) stack.push(node.getLeftChild());
        }
    }

    public static void inOrder(TreeNode root) {
        if (null != root) {
            inOrder(root.getLeftChild());
            print(root);
            inOrder(root.getRightChild());
        }
    }

    public static void inOrderStack(TreeNode root) {
        Stack<TreeNode> stack = new Stack<>();
        TreeNode node = root;
        while (node != null || !stack.isEmpty()) {
            if (node != null) {
                stack.push(node);
                node = node.getLeftChild();
            } else {
                node = stack.pop();
                print(node);
                node = node.getRightChild();
            }
        }
    }

    public static void postOrder(TreeNode root) {
        if (null != root) {
            postOrder(root.getLeftChild());
            postOrder(root.getRightChild());
            print(root);
        }
    }

    public static void postOrderStack(TreeNode root) {
        Stack<TreeNode> stack1 = new Stack<>();//第一次入栈
        Stack<TreeNode> stack2 = new Stack<>();//第二次入栈
        TreeNode node = root;
        while (!stack1.isEmpty() || node != null) {
            if (node != null) {
                stack1.push(node);
                stack2.push(node);
                node = node.getRightChild();
            } else {
                node = stack1.pop();
                node = node.getLeftChild();
            }
        }

        while (!stack2.isEmpty()) {
            print(stack2.pop());
        }
    }

    public static void layer(TreeNode root) {
        if (root == null) return;
        LinkedList<TreeNode> list = new LinkedList<>();
        list.add(root);

        TreeNode currentNode;
        while (!list.isEmpty()) {
            currentNode = list.poll();
            print(currentNode);
            if (currentNode.getLeftChild() != null) {
                list.add(currentNode.getLeftChild());
            }
            if (currentNode.getRightChild() != null) {
                list.add(currentNode.getRightChild());
            }
        }
    }

    private static void print(TreeNode node) {
        System.out.print(node.getValue() + " ");
    }

}
