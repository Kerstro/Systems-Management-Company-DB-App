import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import javax.swing.*;

public class AzaniApp extends JFrame {

    JComboBox<String> payInstitution = new JComboBox<>();
    JComboBox<String> upgradeInstitution = new JComboBox<>();
    JTable reportTable = new JTable();

    public AzaniApp() {
        setTitle("Azani Internet Service Provider System");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Register Institution", registerPanel());
        tabs.add("Payments", paymentPanel());
        tabs.add("Billing and Upgrades", billingPanel());
        tabs.add("Reports", reportPanel());
        add(tabs);

        loadInstitutions();
    }

    void loadInstitutions() {
        try {
            payInstitution.removeAllItems();
            upgradeInstitution.removeAllItems();
            javax.swing.table.DefaultTableModel m = DB.getTable("SELECT id, name FROM institution ORDER BY name");
            for (int i = 0; i < m.getRowCount(); i++) {
                String item = m.getValueAt(i, 0) + " - " + m.getValueAt(i, 1);
                payInstitution.addItem(item);
                upgradeInstitution.addItem(item);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot connect to database: " + ex.getMessage());
        }
    }

    int getId(JComboBox<String> box) {
        String text = (String) box.getSelectedItem();
        return Integer.parseInt(text.split(" - ")[0]);
    }

    JPanel registerPanel() {
        JTextField name = new JTextField();
        JComboBox<String> category = new JComboBox<>(new String[] { "Primary", "Junior", "Senior", "College" });
        JTextField location = new JTextField();
        JTextField contactName = new JTextField();
        JTextField phone = new JTextField();
        JTextField email = new JTextField();
        JComboBox<Integer> mbps = new JComboBox<>(new Integer[] { 4, 10, 20, 25, 50 });
        JTextField users = new JTextField("0");
        JTextField pcs = new JTextField("0");
        JTextField nodes = new JTextField("0");
        JCheckBox ready = new JCheckBox("Already has all requirements");
        JButton save = new JButton("Register");

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 8));
        form.add(new JLabel("Institution name:"));
        form.add(name);
        form.add(new JLabel("Category:"));
        form.add(category);
        form.add(new JLabel("Location:"));
        form.add(location);
        form.add(new JLabel("Contact person name:"));
        form.add(contactName);
        form.add(new JLabel("Contact phone:"));
        form.add(phone);
        form.add(new JLabel("Contact email:"));
        form.add(email);
        form.add(new JLabel("Bandwidth (MBPS):"));
        form.add(mbps);
        form.add(new JLabel("Number of users:"));
        form.add(users);
        form.add(new JLabel("PCs to buy from Azani:"));
        form.add(pcs);
        form.add(new JLabel("LAN nodes to buy (0 or 2-100):"));
        form.add(nodes);
        form.add(new JLabel(""));
        form.add(ready);
        form.add(new JLabel(""));
        form.add(save);

        save.addActionListener(e -> {
            try {
                if (name.getText().trim().isEmpty() || contactName.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Institution name and contact name are required.");
                    return;
                }
                int numUsers = Integer.parseInt(users.getText().trim());
                int numPcs = Integer.parseInt(pcs.getText().trim());
                int numNodes = Integer.parseInt(nodes.getText().trim());
                if (numNodes == 1 || numNodes > 100) {
                    JOptionPane.showMessageDialog(this, "LAN nodes must be 0 or between 2 and 100.");
                    return;
                }

                int id = DB.registerInstitution(name.getText().trim(), (String) category.getSelectedItem(),
                        location.getText().trim(), contactName.getText().trim(), phone.getText().trim(),
                        email.getText().trim(), (Integer) mbps.getSelectedItem(), numUsers, numPcs,
                        numNodes, ready.isSelected());

                JOptionPane.showMessageDialog(this, "Institution registered. ID = " + id
                        + "\nNow record the registration fee in the Payments tab.");
                name.setText("");
                location.setText("");
                contactName.setText("");
                phone.setText("");
                email.setText("");
                loadInstitutions();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Users, PCs and nodes must be whole numbers.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    JPanel paymentPanel() {
        JComboBox<String> type = new JComboBox<>(
                new String[] { "Registration fee", "Installation fee", "Monthly payment" });
        JTextField month = new JTextField(YearMonth.now().toString());
        JTextField date = new JTextField(LocalDate.now().toString());
        JButton pay = new JButton("Record Payment");

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 8));
        form.add(new JLabel("Institution:"));
        form.add(payInstitution);
        form.add(new JLabel("Payment type:"));
        form.add(type);
        form.add(new JLabel("Bill month (yyyy-MM, monthly only):"));
        form.add(month);
        form.add(new JLabel("Payment date (yyyy-MM-dd):"));
        form.add(date);
        form.add(new JLabel(""));
        form.add(pay);

        pay.addActionListener(e -> {
            try {
                if (payInstitution.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(this, "Register an institution first.");
                    return;
                }
                int id = getId(payInstitution);
                String payDate = LocalDate.parse(date.getText().trim()).toString();
                String message;

                if (type.getSelectedIndex() == 0) {
                    message = DB.payRegistration(id, payDate);
                } else if (type.getSelectedIndex() == 1) {
                    message = DB.payInstallation(id, payDate);
                } else {
                    message = DB.payMonthlyBill(id, month.getText().trim(), payDate);
                }
                JOptionPane.showMessageDialog(this, message);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    JPanel billingPanel() {
        JTextField month = new JTextField(YearMonth.now().toString());
        JButton generate = new JButton("Generate Bills");

        JPanel billForm = new JPanel(new GridLayout(0, 2, 5, 8));
        billForm.setBorder(BorderFactory.createTitledBorder("Monthly bills"));
        billForm.add(new JLabel("Month (yyyy-MM):"));
        billForm.add(month);
        billForm.add(new JLabel(""));
        billForm.add(generate);

        generate.addActionListener(e -> {
            try {
                int count = DB.generateBills(month.getText().trim());
                JOptionPane.showMessageDialog(this, count + " bill(s) created.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        JComboBox<Integer> newMbps = new JComboBox<>(new Integer[] { 4, 10, 20, 25, 50 });
        JButton upgrade = new JButton("Upgrade");

        JPanel upgradeForm = new JPanel(new GridLayout(0, 2, 5, 8));
        upgradeForm.setBorder(BorderFactory.createTitledBorder("Upgrade bandwidth (10% discount)"));
        upgradeForm.add(new JLabel("Institution:"));
        upgradeForm.add(upgradeInstitution);
        upgradeForm.add(new JLabel("New bandwidth (MBPS):"));
        upgradeForm.add(newMbps);
        upgradeForm.add(new JLabel(""));
        upgradeForm.add(upgrade);

        upgrade.addActionListener(e -> {
            try {
                if (upgradeInstitution.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(this, "Register an institution first.");
                    return;
                }
                DB.upgrade(getId(upgradeInstitution), (Integer) newMbps.getSelectedItem());
                JOptionPane.showMessageDialog(this, "Bandwidth upgraded.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        JPanel both = new JPanel(new GridLayout(2, 1, 10, 10));
        both.add(billForm);
        both.add(upgradeForm);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(both, BorderLayout.NORTH);
        return panel;
    }

    JPanel reportPanel() {
        JComboBox<String> reports = new JComboBox<>(DB.reportNames);
        JButton show = new JButton("Show Report");

        show.addActionListener(e -> {
            try {
                int index = reports.getSelectedIndex();
                reportTable.setModel(DB.getTable(DB.reportSql[index]));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Choose report:"));
        top.add(reports);
        top.add(show);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportTable), BorderLayout.CENTER);
        return panel;
    }

    public static void main(String[] args) {
        new AzaniApp().setVisible(true);
    }
}
