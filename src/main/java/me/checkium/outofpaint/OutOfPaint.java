package me.checkium.outofpaint;

import me.checkium.outofpaint.frame.OutOfPaintGUI;
import me.checkium.outofpaint.processors.DeobfuscateStringsProcessor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OutOfPaint {
    public static JTextArea logger;
    public static boolean deobfuscateStrings, unprotect;
    public static JProgressBar progressBar;
    double progress;

    public static void main(String[] args) {
        JFrame frame = new OutOfPaintGUI();
        frame.setVisible(true);
    }

    public static void log(String msg) {
        logger.append("\n" + msg);
        logger.setCaretPosition(logger.getDocument().getLength());
    }

    public void start(String inputPath, String outputPath) {
        if (inputPath.isEmpty()) {
            log("Error: Input field is empty.");
            return;
        }
        if (outputPath.isEmpty()) {
            log("Error: Output field is empty.");
            return;
        }
        File input = new File(inputPath);
        File output = new File(outputPath);
        if (!input.exists()) {
            log("Error: Input file is non-existant");
            return;
        }
        if (output.exists()) {
            log("Warning: Output file already exists, it will be overwritten.");
        }
        new Thread(() -> {
            try {
                process(input);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }).run();

    }

    private void process(File jarFile) throws Throwable {
        progress = 0;
        progressBar.setValue((int) progress);
        log("Loading file...");
        ZipFile zipFile = new ZipFile(jarFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        List<ClassNode> nodes = new ArrayList<>();
        double taskProgress = 100 / (unprotect && deobfuscateStrings ? 3 : 2);
        double perEntry = Math.ceil(countRegularFiles(zipFile) / taskProgress);
        ;
        try {
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        ClassReader cr = new ClassReader(in);
                        ClassNode classNode = new ClassNode();
                        cr.accept(classNode, 0);
                        nodes.add(classNode);
                        progress += perEntry;
                        progressBar.setValue((int) progress);
                    }
                }
            }
        } finally {
            zipFile.close();
        }
        double perClass = Math.ceil(nodes.size() / taskProgress);
        if (deobfuscateStrings) {
            log("Deobfuscating strings...");
            DeobfuscateStringsProcessor processor = new DeobfuscateStringsProcessor();
            processor.proccess(nodes);
        }
    }

    private int countRegularFiles(final ZipFile zipFile) {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        int numRegularFiles = 0;
        while (entries.hasMoreElements()) {
            if (!entries.nextElement().isDirectory()) {
                ++numRegularFiles;
            }
        }
        return numRegularFiles;
    }
}
