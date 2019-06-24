package com.jr.test.algorithms.trees;

/**
 * Created by PengXianglong on 2019/4/15.
 */
public class Test {

    public static void main(String[] args) {
        TreeNode tree = buildTree();

//        TreeTraveller.preOrder(tree);
//        System.out.println();

//        TreeTraveller.preOrderStack(tree);
//        System.out.println();

//        TreeTraveller.inOrder(tree);
//        System.out.println();

//        TreeTraveller.inOrderStack(tree);
//        System.out.println();

//        TreeTraveller.postOrder(tree);
//        System.out.println();

//        TreeTraveller.postOrderStack(tree);
//        System.out.println();

//        TreeTraveller.layer(tree);
    }

    private static TreeNode buildTree() {
        TreeNode l31 = new TreeNode("31", null, null);
        TreeNode l32 = new TreeNode("32", null, null);
        TreeNode l33 = new TreeNode("33", null, null);
        TreeNode l34 = new TreeNode("34", null, null);
        TreeNode l35 = new TreeNode("35", null, null);
        TreeNode l36 = new TreeNode("36", null, null);
        TreeNode l37 = new TreeNode("37", null, null);
        TreeNode l38 = new TreeNode("38", null, null);

        TreeNode l21 = new TreeNode("21", l31, l32);
        TreeNode l22 = new TreeNode("22", l33, l34);
        TreeNode l23 = new TreeNode("23", l35, l36);
        TreeNode l24 = new TreeNode("24", l37, l38);

        TreeNode l11 = new TreeNode("11", l21, l22);
        TreeNode l12 = new TreeNode("12", l23, l24);

        return new TreeNode("1", l11,l12);
    }

}
