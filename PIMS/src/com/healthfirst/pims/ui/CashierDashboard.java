package com.healthfirst.pims.ui;

import com.healthfirst.pims.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * Landing screen for Cashier users: Point of Sale + read-only Stock Check.
 */
public class CashierDashboard extends JFrame {

    private final User currentUser;

    public CashierDashboard(User currentUser) {
        this.currentUser = currentUser;
        setTitle("HealthFirst PIMS - Cashier (" + currentUser.getFullName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();

        PosPanel posPanel = new PosPanel(currentUser);
        StockCheckPanel stockPanel = new StockCheckPanel();
        tabs.addTab("Point of Sale", posPanel);
        tabs.addTab("Stock Check", stockPanel);

        // Reload stock figures whenever a tab is re-opened
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) {
                posPanel.refreshResults();
            } else {
                stockPanel.refresh();
            }
        });

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel welcome = new JLabel("  Welcome, " + currentUser.getFullName() + " (Cashier)");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 14));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        topBar.add(welcome, BorderLayout.WEST);
        topBar.add(logoutBtn, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(topBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
