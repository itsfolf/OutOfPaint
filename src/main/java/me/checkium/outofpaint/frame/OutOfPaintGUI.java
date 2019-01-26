package me.checkium.outofpaint.frame;

import jdk.jshell.execution.Util;
import me.checkium.outofpaint.OutOfPaint;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.File;

public class OutOfPaintGUI extends JFrame {
    private JPanel panel1;
    private JTextArea inputTextArea;
    private JCheckBox debofuscateStringBox;
    private JCheckBox unprotectBox;
    private JButton startButton;
    private JTextArea logTextArea;
    private JButton inputSelectBtn;
    private JTextArea outputTextArea;
    private JButton outputSelectBtn;
    private JProgressBar progressBar;
    private JScrollPane scrollPane;

    public OutOfPaintGUI() {
        super("OutOfPaint v1.0.0");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        setContentPane(panel1);
        pack();
        setSize(690, 400);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(false);

        logTextArea.setText("Waiting for start...");
        inputSelectBtn.addActionListener(e -> {
            String file = chooseFile(null, this, new JarFileFilter());
            if (file != null) {
                inputTextArea.setText(file);
            }
        });
        outputSelectBtn.addActionListener(e -> {
            String file = chooseFile(null, this, new JarFileFilter());
            if (file != null) {
                outputTextArea.setText(file);
            }
        });
        debofuscateStringBox.addActionListener(e ->  OutOfPaint.deobfuscateStrings = debofuscateStringBox.isSelected());
        unprotectBox.addActionListener(e ->  OutOfPaint.unprotect = unprotectBox.isSelected());
        OutOfPaint.logger = logTextArea;
        OutOfPaint.progressBar = progressBar;
        startButton.addActionListener(e -> new OutOfPaint().start(inputTextArea.getText(), outputTextArea.getText()));
    }

    private String chooseFile(final File currFolder, final Component parent, FileFilter filter) {
        final JFileChooser chooser = new JFileChooser(currFolder);
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile().getAbsolutePath();
        return null;
    }

    class JarFileFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            String name = f.getName();
            return f.isDirectory() || name.endsWith(".jar") || name.endsWith(".zip");
        }

        @Override
        public String getDescription() {
            return "Java Archives (*.jar/*.zip)";
        }
    }

}
