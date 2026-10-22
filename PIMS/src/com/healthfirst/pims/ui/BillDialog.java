package com.healthfirst.pims.ui;

import com.healthfirst.pims.model.Sale;
import com.healthfirst.pims.model.SaleItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modal window that shows the customer's bill after checkout.
 * The bill can be printed or saved as a text file.
 */
public class BillDialog extends JDialog {

    private static final Color PRIMARY = new Color(30, 90, 160);
    private static final int WIDTH_CHARS = 43;

    private final Sale sale;
    private final JTextArea billArea = new JTextArea();

    public BillDialog(Window owner, Sale sale, List<SaleItem> items) {
        super(owner, "Bill #" + sale.getSaleId(), Dialog.ModalityType.APPLICATION_MODAL);
        this.sale = sale;

        billArea.setEditable(false);
        billArea.setCaretColor(billArea.getBackground()); // hide the text cursor in the read-only bill
        billArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        billArea.setMargin(new Insets(12, 12, 12, 12));
        billArea.setText(buildBill(sale, items));
        billArea.setCaretPosition(0);

        JButton printBtn = styledButton("Print");
        JButton saveBtn = styledButton("Save as Text File");
        JButton closeBtn = styledButton("Close");
        printBtn.addActionListener(e -> onPrint());
        saveBtn.addActionListener(e -> onSave());
        closeBtn.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        buttons.add(printBtn);
        buttons.add(saveBtn);
        buttons.add(closeBtn);

        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBorder(new EmptyBorder(10, 10, 4, 10));
        root.add(new JScrollPane(billArea), BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(480, 560);
        setLocationRelativeTo(owner);
    }

    // ------------------------------------------------------------- bill text

    private String buildBill(Sale sale, List<SaleItem> items) {
        String line = repeat('-', WIDTH_CHARS);
        String dateText = sale.getSaleDate() == null
                ? ""
                : sale.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append(center("HEALTHFIRST PHARMACY", WIDTH_CHARS)).append('\n');
        sb.append(center("Inventory Management System", WIDTH_CHARS)).append('\n');
        sb.append(line).append('\n');
        sb.append("Bill No : ").append(sale.getSaleId()).append('\n');
        sb.append("Date    : ").append(dateText).append('\n');
        sb.append("Cashier : ").append(sale.getCashierName() == null ? "" : sale.getCashierName()).append('\n');
        sb.append(line).append('\n');
        sb.append(String.format("%-20s %3s %8s %9s%n", "Item", "Qty", "Price", "Total"));
        sb.append(line).append('\n');

        for (SaleItem item : items) {
            String name = item.getMedicineName();
            if (name.length() > 20) {
                name = name.substring(0, 20);
            }
            sb.append(String.format("%-20s %3d %8s %9s%n",
                    name,
                    item.getQuantitySold(),
                    money(item.getPriceAtSale()),
                    money(item.getLineTotal())));
        }

        sb.append(line).append('\n');
        sb.append(String.format("%-20s %22s%n", "TOTAL", money(sale.getTotalAmount())));
        sb.append(line).append('\n');
        sb.append(center("Thank you - get well soon!", WIDTH_CHARS)).append('\n');
        return sb.toString();
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private String center(String text, int width) {
        int pad = Math.max(0, (width - text.length()) / 2);
        return repeat(' ', pad) + text;
    }

    // --------------------------------------------------------------- actions

    private void onPrint() {
        try {
            billArea.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Could not print the bill:\n" + ex.getMessage(),
                    "Print error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSave() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Bill_" + sale.getSaleId() + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getParentFile(), file.getName() + ".txt");
        }
        try {
            Files.write(file.toPath(), billArea.getText().getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this, "Bill saved to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the bill:\n" + ex.getMessage(),
                    "Save error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }
}
