package com.github.fzc;

import com.google.common.collect.Sets;
import com.sun.tools.javap.JavapTask;
import io.netty.util.internal.StringUtil;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author shengxun
 * @date 12/25/18 12:26 PM
 */
public class TraceAnalyser {

    private static final Logger LOG = Logger.getLogger(TraceAnalyser.class.getName());

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
            "(?<=Compiled[\\s]from[\\s]\")([\\w]+)(?=[.]java)");

    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(?<=class\\s)([\\w.$]+)(?=\\s)");

    private static final Pattern METHOD_CALLED_PATTERN = Pattern.compile(
            "(?<=//\\sMethod\\s)([\\w.:()\\[;/$\"\"<>]+)"
    );

    private static final Pattern METHOD_DESCRIPTOR_PATTERN = Pattern.compile(
            "(?<=descriptor:\\s)([\\w/;\\(\\)\\[]+)");

    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("(?<=\\s)([\\w.$]+)(?=\\()");

    Integer count = 0;

    public TraceAnalyser() {
        count = 0;
    }


    public void trace(String rootDir, String classPattern, String methodPattern) throws Exception {

        String path = rootDir.endsWith("/") ? rootDir + "target/classes" : rootDir + "/target" +
                "/classes";

        CallGraph callGraph = new CallGraph();

        count = 0;

        Set<Integer> startingVertices = Sets.newHashSet();

        Files.walkFileTree(Paths.get(path), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir,
                    final BasicFileAttributes attrs)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                try {
                    String fullPath = file.toString();
                    if (fullPath.endsWith(".class")) {
                        LOG.info("fullPath: " + fullPath);
                        JavapTask task = new JavapTask();
                        String[] args = { "-s", "-private", "-c", fullPath };
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        task.setLog(baos);
                        task.handleOptions(args);
                        task.run();
                        String classCode = baos.toString();
                        String[] codes = classCode.split("\\n");
                        handleVisitFile(codes, callGraph, startingVertices, classPattern,
                                methodPattern);
                        System.out.println("\n");
                        ++count;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        LOG.info("total nodes: " + count);
        LOG.info("called: " + callGraph.getNumEdges());

        if (!startingVertices.isEmpty()) {
            for (Integer vertex : startingVertices) {
                callGraph.dfs(vertex, 0);
            }
        }
    }

    private void handleVisitFile(String[] codes, CallGraph callGraph,
            Set<Integer> startingVertices, String classPattern, String methodPattern) {
        String className = null;
        String classSignature = null;
        int tag = 0;
        String previous = "";
        String penultimate = "";
        String methodSignature = "";
        String methodName = "";
        String signature = "";
        for (String line : codes) {
            if (tag < 2) {
                Matcher matcher = CLASS_PATTERN.matcher(line);
                if (matcher.find()) {
                    className = matcher.group();
                    classSignature = className.replaceAll("[.]", "/");
                }
                ++tag;
            } else {
                if (line.endsWith("Code:")) {
                    Matcher matcher = METHOD_NAME_PATTERN.matcher(penultimate);
                    if (matcher.find()) {
                        methodName = matcher.group();
                        /**
                         * it's class initial method
                         */
                        if (methodName.equals(className)) {
                            methodName = "\"<init>\"";
                        }
                    }
                    matcher = METHOD_DESCRIPTOR_PATTERN.matcher(previous);
                    if (matcher.find()) {
                        methodSignature = matcher.group();
                    }
                    signature = classSignature + "." + methodName + ":" +
                            methodSignature;
                    LOG.info("signature " + signature);
                } else if (!StringUtil.isNullOrEmpty(signature)) {
                    /**
                     * method body
                     */
                    Matcher matcher = METHOD_CALLED_PATTERN.matcher(line);
                    if (matcher.find()) {
                        String calledPattern = matcher.group();
                        String[] temp = calledPattern.split("[:]");
                        if (!temp[0].contains(".")) {
                            calledPattern = classSignature + "." + calledPattern;
                        }
                        if (calledPattern.contains(classPattern) && calledPattern.contains(
                                methodPattern)) {
                            Integer vertex = callGraph.getId(calledPattern);
                            startingVertices.add(vertex);
                            LOG.info("starting: " + vertex + " " + calledPattern);
                        }
                        callGraph.add(calledPattern, signature);

                    }
                } else if (line.matches("^\\s+$")) {
                    /**
                     * method code ends
                     */
                    previous = "";
                    penultimate = "";
                    methodSignature = "";
                    methodName = "";
                    signature = "";
                }
            }

            penultimate = previous;
            previous = line;
        }
    }

}
