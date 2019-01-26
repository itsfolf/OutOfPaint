package me.checkium.outofpaint;

import me.checkium.outofpaint.frame.OutOfPaintGUI;
import me.checkium.outofpaint.processors.DeobfuscateStringsProcessor;
import me.checkium.outofpaint.processors.UnprotectProcessor;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
                process(input, output);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }).run();

    }

    private void process(File input, File output) throws Throwable {
        progressBar.setValue(0);
        log("Loading file...");

        long current = System.currentTimeMillis();
        ZipFile zipFile = new ZipFile(input);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        Map<String, ClassNode> classes = new HashMap<>();
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output));

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.getName().endsWith(".class")) {
                ClassReader cr = new ClassReader(zipFile.getInputStream(entry));
                ClassNode classNode = new ClassNode();
                cr.accept(classNode, 0);
                classes.put(classNode.name, classNode);
            } else {
                ZipEntry newEntry = new ZipEntry(entry);
                newEntry.setTime(current);
                zos.putNextEntry(newEntry);
                zos.write(IOUtils.toByteArray(zipFile.getInputStream(entry)));
            }
        }
        log("File loaded in " + (System.currentTimeMillis() - current) + "ms");
        progressBar.setValue(1);

        current = System.currentTimeMillis();
        if (deobfuscateStrings) {
            log("Deobfuscating strings...");
            DeobfuscateStringsProcessor processor = new DeobfuscateStringsProcessor();
            processor.proccess(classes.values());
            log("Strings deobfuscated in " + (System.currentTimeMillis() - current) + "ms");
            progressBar.setValue(2);
        } else {
            progressBar.setValue(2);
        }

        current = System.currentTimeMillis();
        if (unprotect) {
            log("Unprotecting...");
            UnprotectProcessor processor = new UnprotectProcessor();
            processor.proccess(classes.values());
            log("Unprotected in " + (System.currentTimeMillis() - current) + "ms");
            progressBar.setValue(3);
        } else {
            progressBar.setValue(3);
        }

        current = System.currentTimeMillis();
        log("Writing file...");
        for (ClassNode cn : classes.values()) {
            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);

            ZipEntry newEntry = new ZipEntry(cn.name + ".class");
            newEntry.setTime(current);

            zos.putNextEntry(newEntry);
            zos.write(cw.toByteArray());
        }

        zos.close();
        log("Filed written in " + (System.currentTimeMillis() - current) + "ms");
        log("Finished processing.");
        progressBar.setValue(4);
    }

}
