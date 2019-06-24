package com.jr.test.algorithms.trees;

import java.util.Stack;

public class ExpressionTreeBuilder {

    public static void main(String[] args) {
        TreeNode tree = build(new String[]{"a", "b", "+", "c", "d", "e", "+", "*", "*"});
        TreeTraveller.preOrder(tree);
        System.out.println();
        TreeTraveller.inOrder(tree);
        System.out.println();
        TreeTraveller.postOrder(tree);
    }

    public static TreeNode build(String[] postfixExpression) {
        Stack<TreeNode> stack = new Stack<>();
        for (String s : postfixExpression) {
            if (isOperator(s)) {
                TreeNode right = stack.pop(), left = stack.pop();
                TreeNode node = new TreeNode(s, left, right);
                stack.push(node);
            } else {
                stack.push(new TreeNode(s, null, null));
            }
        }
        return stack.pop();
    }

    private static boolean isOperator(String oper) {
        return oper.equals("+") || oper.equals("-") || oper.equals("*") || oper.equals("/");
    }

}
