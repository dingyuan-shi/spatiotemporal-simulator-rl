package Solvers.ACP;

public class Node {
    private int r;
    private int c;
    private int t;
    private int g;
    private int h;
    private Node pre;

    public Node(int r, int c, int t, int g, int h, Node pre) {
        this.r = r;
        this.c = c;
        this.t = t;
        this.g = g;
        this.h = h;
        this.pre = pre;
    }

    public int getR() {
        return r;
    }

    public int getC() {
        return c;
    }

    public int getT() {
        return t;
    }

    public int getG() {
        return g;
    }

    public int getH() {
        return h;
    }

    public Node getPre() {
        return pre;
    }

    public void setR(int r) {
        this.r = r;
    }

    public void setC(int c) {
        this.c = c;
    }

    public void setT(int t) {
        this.t = t;
    }

    public void setG(int g) {
        this.g = g;
    }

    public void setH(int h) {
        this.h = h;
    }

    public void setPre(Node pre) {
        this.pre = pre;
    }
}
