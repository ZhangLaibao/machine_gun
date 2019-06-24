package com.jr.test.algorithms.link;

public class Node {

    public int value;
    public Node next;

    public Node(int value, Node next) {
        this.value = value;
        this.next = next;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("value=").append(value);
        sb.append(", next=").append(next);
        sb.append("]");
        return sb.toString();
    }
}
