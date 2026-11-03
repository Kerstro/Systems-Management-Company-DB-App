import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import javax.swing.table.DefaultTableModel;

public class DB {

    static String url = "jdbc:mysql://localhost:3306/azani_db?sslMode=DISABLED&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    static String user = "root";
    static String password = "Castro2006%";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL Connector/J was not found. " +
                            "Check that mysql-connector-j-26.7.0.jar is under Referenced Libraries.",
                    e);
        }
    }

    public static int monthlyCost(int mbps) {
        switch (mbps) {
            case 4:
                return 1200;
            case 10:
                return 2000;
            case 20:
                return 3500;
            case 25:
                return 4000;
            case 50:
                return 7000;
            default:
                return 0;
        }
    }

    public static int lanCost(int nodes) {
        if (nodes >= 2 && nodes <= 10)
            return 10000;
        if (nodes >= 11 && nodes <= 20)
            return 20000;
        if (nodes >= 21 && nodes <= 40)
            return 30000;
        if (nodes >= 41 && nodes <= 100)
            return 40000;
        return 0;
    }

    public static int registerInstitution(String name, String category, String location,
            String contactName, String phone, String email,
            int mbps, int users, int pcs, int nodes, boolean ready) throws SQLException {

        int pcCost = pcs * 40000;
        int lan = lanCost(nodes);
        int installCost = 10000 + pcCost + lan;
        int monthly = monthlyCost(mbps);

        String sql = "INSERT INTO institution (name, category, location, contact_name, contact_phone, "
                + "contact_email, reg_date, mbps, upgraded, num_users, pcs, nodes, site_ready, "
                + "pc_cost, lan_cost, install_cost, monthly_charge) "
                + "VALUES (?, ?, ?, ?, ?, ?, CURDATE(), ?, 0, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection con = getConnection();
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, name);
        ps.setString(2, category);
        ps.setString(3, location);
        ps.setString(4, contactName);
        ps.setString(5, phone);
        ps.setString(6, email);
        ps.setInt(7, mbps);
        ps.setInt(8, users);
        ps.setInt(9, pcs);
        ps.setInt(10, nodes);
        ps.setInt(11, ready ? 1 : 0);
        ps.setInt(12, pcCost);
        ps.setInt(13, lan);
        ps.setInt(14, installCost);
        ps.setInt(15, monthly);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        rs.next();
        int id = rs.getInt(1);
        con.close();
        return id;
    }

    private static void addPayment(Connection con, int id, String type, double amount, String date)
            throws SQLException {
        String sql = "INSERT INTO payment (institution_id, type, amount, pay_date) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.setString(2, type);
        ps.setDouble(3, amount);
        ps.setString(4, date);
        ps.executeUpdate();
    }

    private static boolean alreadyPaid(Connection con, int id, String type) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM payment WHERE institution_id = ? AND type = ?");
        ps.setInt(1, id);
        ps.setString(2, type);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    public static String payRegistration(int id, String date) throws SQLException {
        Connection con = getConnection();
        if (alreadyPaid(con, id, "REGISTRATION")) {
            con.close();
            throw new SQLException("Registration fee has already been paid.");
        }
        addPayment(con, id, "REGISTRATION", 8500, date);
        con.close();
        return "Registration fee of KSh 8,500 recorded.";
    }

    public static String payInstallation(int id, String date) throws SQLException {
        Connection con = getConnection();
        if (alreadyPaid(con, id, "INSTALLATION")) {
            con.close();
            throw new SQLException("Installation fee has already been paid.");
        }
        PreparedStatement ps = con.prepareStatement("SELECT install_cost FROM institution WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        rs.next();
        double cost = rs.getDouble(1);
        addPayment(con, id, "INSTALLATION", cost, date);
        con.close();
        return "Installation payment of KSh " + cost + " recorded.";
    }

    public static String payMonthlyBill(int id, String month, String payDate) throws SQLException {
        Connection con = getConnection();
        PreparedStatement ps = con.prepareStatement(
                "SELECT bill_id, amount, due_date, paid FROM bill WHERE institution_id = ? AND bill_month = ?");
        ps.setInt(1, id);
        ps.setString(2, month);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            con.close();
            throw new SQLException("No bill found for that month. Generate the bills first.");
        }
        if (rs.getInt("paid") == 1) {
            con.close();
            throw new SQLException("This bill has already been paid.");
        }

        int billId = rs.getInt("bill_id");
        double amount = rs.getDouble("amount");
        LocalDate dueDate = rs.getDate("due_date").toLocalDate();
        LocalDate paidOn = LocalDate.parse(payDate);

        double fine = 0;
        double reconnection = 0;
        if (paidOn.isAfter(dueDate)) {
            fine = Math.round(amount * 0.15 * 100) / 100.0;
        }
        if (paidOn.isAfter(dueDate.plusDays(10))) {
            reconnection = 1000;
        }

        PreparedStatement up = con.prepareStatement(
                "UPDATE bill SET paid = 1, paid_date = ?, fine = ?, reconnection_fee = ? WHERE bill_id = ?");
        up.setString(1, payDate);
        up.setDouble(2, fine);
        up.setDouble(3, reconnection);
        up.setInt(4, billId);
        up.executeUpdate();

        addPayment(con, id, "MONTHLY", amount, payDate);
        if (fine > 0) {
            addPayment(con, id, "FINE", fine, payDate);
        }
        if (reconnection > 0) {
            addPayment(con, id, "RECONNECTION", reconnection, payDate);
        }
        con.close();

        double total = amount + fine + reconnection;
        return "Bill: " + amount + "\nFine: " + fine + "\nReconnection fee: " + reconnection
                + "\nTotal paid: KSh " + total;
    }

    public static int generateBills(String month) throws SQLException {
        String dueDate = YearMonth.parse(month).atEndOfMonth().toString();
        Connection con = getConnection();
        PreparedStatement ps = con.prepareStatement(
                "INSERT IGNORE INTO bill (institution_id, bill_month, amount, due_date) "
                        + "SELECT id, ?, monthly_charge, ? FROM institution");
        ps.setString(1, month);
        ps.setString(2, dueDate);
        int rows = ps.executeUpdate();
        con.close();
        return rows;
    }

    public static void upgrade(int id, int newMbps) throws SQLException {
        double newCharge = monthlyCost(newMbps) * 0.9;
        Connection con = getConnection();
        PreparedStatement ps = con.prepareStatement(
                "UPDATE institution SET mbps = ?, upgraded = 1, monthly_charge = ? WHERE id = ? AND mbps < ?");
        ps.setInt(1, newMbps);
        ps.setDouble(2, newCharge);
        ps.setInt(3, id);
        ps.setInt(4, newMbps);
        int rows = ps.executeUpdate();
        con.close();
        if (rows == 0) {
            throw new SQLException("The new bandwidth must be higher than the current one.");
        }
    }

    public static String[] reportNames = {
            "Registered institutions",
            "Defaulters",
            "Institutions with disconnection issues",
            "Infrastructure requirements",
            "Total installation cost per institution",
            "PC and LAN cost (assorted services)",
            "Monthly charges for upgraded services",
            "Charges by institution category",
            "Aggregate amount per service by institution"
    };

    public static String[] reportSql = {
            "SELECT id, name, category, location, contact_name, contact_phone, contact_email, reg_date "
                    + "FROM institution ORDER BY name",

            "SELECT i.name, b.bill_month, b.amount, b.amount * 0.15 AS fine, b.amount * 1.15 AS total_due "
                    + "FROM bill b JOIN institution i ON i.id = b.institution_id "
                    + "WHERE b.paid = 0 AND b.due_date < CURDATE() ORDER BY i.name",

            "SELECT i.name, b.bill_month, b.amount * 1.15 AS bill_plus_fine, 1000 AS reconnection_fee, "
                    + "b.amount * 1.15 + 1000 AS total_to_reconnect "
                    + "FROM bill b JOIN institution i ON i.id = b.institution_id "
                    + "WHERE b.paid = 0 AND DATE_ADD(b.due_date, INTERVAL 10 DAY) < CURDATE() ORDER BY i.name",

            "SELECT name, num_users, pcs, nodes, IF(site_ready = 1, 'Yes', 'No') AS already_ready "
                    + "FROM institution ORDER BY name",

            "SELECT name, 10000 AS installation_fee, pc_cost, lan_cost, install_cost AS total_installation_cost "
                    + "FROM institution ORDER BY name",

            "SELECT name, pcs, pc_cost, nodes, lan_cost, pc_cost + lan_cost AS total "
                    + "FROM institution WHERE pcs > 0 OR nodes > 0 ORDER BY name",

            "SELECT name, mbps AS new_mbps, monthly_charge FROM institution WHERE upgraded = 1 ORDER BY name",

            "SELECT i.category, "
                    + "SUM(IF(p.type = 'MONTHLY', p.amount, 0)) AS monthly_charges, "
                    + "SUM(IF(p.type = 'FINE', p.amount, 0)) AS overdue_fines, "
                    + "SUM(IF(p.type = 'RECONNECTION', p.amount, 0)) AS reconnection_fees "
                    + "FROM institution i LEFT JOIN payment p ON p.institution_id = i.id "
                    + "GROUP BY i.category",

            "SELECT i.name, "
                    + "SUM(IF(p.type = 'REGISTRATION', p.amount, 0)) AS registration, "
                    + "SUM(IF(p.type = 'INSTALLATION', p.amount, 0)) AS installation, "
                    + "SUM(IF(p.type = 'MONTHLY', p.amount, 0)) AS monthly, "
                    + "SUM(IF(p.type = 'FINE', p.amount, 0)) AS fines, "
                    + "SUM(IF(p.type = 'RECONNECTION', p.amount, 0)) AS reconnection, "
                    + "SUM(IF(p.type IS NULL, 0, p.amount)) AS total "
                    + "FROM institution i LEFT JOIN payment p ON p.institution_id = i.id "
                    + "GROUP BY i.id, i.name ORDER BY i.name"
    };

    public static DefaultTableModel getTable(String sql) throws SQLException {
        Connection con = getConnection();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();

        DefaultTableModel model = new DefaultTableModel();
        for (int i = 1; i <= columns; i++) {
            model.addColumn(md.getColumnLabel(i));
        }
        while (rs.next()) {
            Object[] row = new Object[columns];
            for (int i = 0; i < columns; i++) {
                row[i] = rs.getObject(i + 1);
            }
            model.addRow(row);
        }
        con.close();
        return model;
    }
}
