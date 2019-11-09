package com.github.fzc;

/**
 * @author shengxun
 * @date 12/25/18 12:24 PM
 */
public class Main {

    public static void main(String args[]) {
        TraceAnalyser analyser = new TraceAnalyser();
        try {
            analyser.trace(args[0], args[1], args[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
