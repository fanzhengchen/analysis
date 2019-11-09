package com.github.fzc;

/**
 * @author shengxun
 * @date 12/25/18 12:25 PM
 */

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * @author shengxun
 * @date 12/24/18 10:37 PM
 */
public class CallGraph {

    private static final int SIZE = 16;

    private HashMap<String, Integer> mHash = new HashMap<>();
    private Integer count = 0;
    private Node[] edges = new Node[SIZE];
    private static final Node NULL = new Node("", 0);
    private PrintStream mPrintStream;
    private int numEdges = 0;

    private static class Node {
        private String pattern;
        private Integer vertex;
        private Node next = null;

        Node(String pattern, Integer vertex) {
            this.pattern = pattern;
            this.vertex = vertex;
            this.next = this;
        }

        Node(String pattern, Integer vertex, Node next) {
            this.pattern = pattern;
            this.vertex = vertex;
            this.next = next;
        }

        @Override
        public int hashCode() {
            return pattern.hashCode();
        }
    }

    private Node start = NULL;

    public CallGraph() {
        this(System.out);
    }

    public CallGraph(PrintStream printStream) {
        mPrintStream = printStream;
    }

    public void init(String initialPattern) {
        start = new Node(initialPattern, getId(initialPattern), NULL);
    }


    public void add(String from, String to) {
        Integer u = getId(from);
        Integer v = getId(to);
        Node node = new Node(to, v, edges[u].next);
        ++numEdges;
        edges[u].next = node;
    }

    public Integer getId(String pattern) {
        if (mHash.containsKey(pattern)) {
            return mHash.get(pattern);
        }
        ++count;
        int size = edges.length;
        if (size <= count) {
            Node[] newEdges = new Node[size << 1];
            System.arraycopy(edges, 0, newEdges, 0, size);
            edges = newEdges;
        }
        edges[count] = new Node(pattern, count, NULL);
        mHash.put(pattern, count);
        return count;
    }

    public void dfs(int u, int dep) {
        if (dep > 10) {
            return;
        }
        printSpace(dep);
        mPrintStream.println(dep + " -- " + edges[u].pattern);
        for (Node v = edges[u]; v != NULL; v = v.next) {
            if (u != v.vertex) {
                dfs(v.vertex, dep + 1);
            }
        }

    }

    private void printSpace(int n) {
        for (int i = 1; i <= n; ++i) {
            mPrintStream.print("  ");
        }
    }

    public int getNumEdges() {
        return numEdges;
    }
}

