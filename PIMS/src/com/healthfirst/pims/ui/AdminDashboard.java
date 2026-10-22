package com.healthfirst.pims.ui;

import com.healthfirst.pims.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * Landing screen for Admin users: Medicines, Suppliers, Users and Reports tabs.
 */
public class AdminDashboard extends JFrame {

    private final User currentUser;

    public AdminDashboard(User currentUser) {
        this.currentUser = currentUser;
        setTitle("HealthFirst PIMS - Admin Dashboard (" + currentUser.getFullName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();

        MedicinePanel medicinePanel = new MedicinePanel();
        tabs.addTab("Manage Medicines", medicinePanel);
        tabs.addTab("Manage Suppliers", new SupplierPanel());
        tabs.addTab("Manage Users", new UsersPanel());

        ReportsPanel reportsPanel = new ReportsPanel();
        tabs.addTab("Reports", reportsPanel);

        // Re-opening a tab picks up changes made elsewhere (suppliers, new sales, stock)
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) {
                medicinePanel.refresh();
            } else if (tabs.getSelectedIndex() == 3) {
                reportsPanel.refresh();
            }
        });

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel welcome = new JLabel("  Welcome, " + currentUser.getFullName() + " (Admin)");
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
