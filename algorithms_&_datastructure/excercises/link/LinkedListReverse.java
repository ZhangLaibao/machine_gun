package com.jr.test.algorithms.link;

public class LinkedListReverse {

    public static void main(String[] args) {
        Node next4 = new Node(5, null);
        Node next3 = new Node(4, next4);
        Node next2 = new Node(3, next3);
        Node next1 = new Node(2, next2);
        Node head = new Node(1, next1);

        Node node = reverse(head);
        System.out.println(node);
    }

    static Node reverse(Node current) {

        Node nextNode, previousNode = null;

        while (current != null) {
            //save the next node
            nextNode = current.next;
            //update the value of "next"
            current.next = previousNode;
            //shift the pointers
            previousNode = current;
            current = nextNode;
        }
        return previousNode;

    }
}
