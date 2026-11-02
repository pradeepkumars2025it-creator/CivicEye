import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import javax.swing.JFileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    // Demo-only admin credentials. Change before demonstrating.
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";

    private static final String[] CATEGORIES = {
        "Public Restroom", "Garbage / Dustbin", "Pothole / Road",
        "Streetlight", "Bus Stop", "Other"
    };

    public static void main(String[] args) {
        try (Connection ignored = DBConnection.getConnection()) {
            System.out.println("Database connection successful.");
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL.");
            System.out.println("Check MySQL is running, DBConnection.java password,");
            System.out.println("and that you ran schema.sql.");
            System.out.println("Details: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.println("\n========================================");
            System.out.println("              CIVICEYE");
            System.out.println("     Public Facility Issue Tracker");
            System.out.println("========================================");
            System.out.println("1. Register as Citizen");
            System.out.println("2. Citizen Login");
            System.out.println("3. Admin Login");
            System.out.println("4. Exit");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> register();
                case 2 -> citizenLogin();
                case 3 -> adminLogin();
                case 4 -> {
                    System.out.println("Thank you for using CivicEye!");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void register() {
        System.out.println("\n--- Citizen Registration ---");
        String name = readNonEmpty("Full name: ");
        String email = readNonEmpty("Email: ").toLowerCase();
        String password = readNonEmpty("Password: ");

        String sql = "INSERT INTO users(name, email, password_hash) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hashPassword(password));
            ps.executeUpdate();
            System.out.println("Registration successful. You can now log in.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("That email is already registered.");
        } catch (SQLException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private static void citizenLogin() {
        System.out.println("\n--- Citizen Login ---");
        String email = readNonEmpty("Email: ").toLowerCase();
        String password = readNonEmpty("Password: ");

        String sql = "SELECT user_id, name FROM users WHERE email = ? AND password_hash = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hashPassword(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    citizenMenu(rs.getInt("user_id"), rs.getString("name"));
                } else {
                    System.out.println("Invalid email or password.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void adminLogin() {
        System.out.println("\n--- Admin Login ---");
        String username = readNonEmpty("Username: ");
        String password = readNonEmpty("Password: ");

        if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
            adminMenu();
        } else {
            System.out.println("Invalid admin credentials.");
        }
    }

    private static void citizenMenu(int userId, String name) {
        while (true) {
            System.out.println("\n--- Citizen Dashboard: " + name + " ---");
            System.out.println("1. Report a Public Facility Issue");
            System.out.println("2. View All Reports");
            System.out.println("3. Search Reports by Location");
            System.out.println("4. View My Reports");
            System.out.println("5. Logout");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> submitComplaint(userId);
                case 2 -> listComplaints("SELECT c.complaint_id, c.category, c.location, c.status, c.created_at, u.name FROM complaints c JOIN users u ON c.user_id=u.user_id ORDER BY c.created_at DESC", null);
                case 3 -> searchByLocation();
                case 4 -> listComplaints("SELECT c.complaint_id, c.category, c.location, c.status, c.created_at, u.name FROM complaints c JOIN users u ON c.user_id=u.user_id WHERE c.user_id=? ORDER BY c.created_at DESC", userId);
                case 5 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void submitComplaint(int userId) {
        System.out.println("\n--- Report a Public Facility Issue ---");
        for (int i = 0; i < CATEGORIES.length; i++) {
            System.out.println((i + 1) + ". " + CATEGORIES[i]);
        }
        int categoryChoice = readInt("Choose category: ");
        if (categoryChoice < 1 || categoryChoice > CATEGORIES.length) {
            System.out.println("Invalid category.");
            return;
        }

        String location = readNonEmpty("Location / landmark: ");
        String description = readNonEmpty("Describe the problem: ");
        String photoPath = null;

try {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Select Complaint Photo");

    int result = chooser.showOpenDialog(null);

    if (result == JFileChooser.APPROVE_OPTION) {
        File selectedFile = chooser.getSelectedFile();
        String fileName = selectedFile.getName().toLowerCase();

        if (fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".png")) {

            Path uploadFolder = Path.of("uploads");
            Files.createDirectories(uploadFolder);

            String extension =
                fileName.substring(fileName.lastIndexOf('.'));

            Path destination = uploadFolder.resolve(
                UUID.randomUUID().toString() + extension
            );

            Files.copy(selectedFile.toPath(), destination,
                StandardCopyOption.REPLACE_EXISTING);

            photoPath = destination.toAbsolutePath().toString();
            System.out.println("Photo attached successfully!");

        } else {
            System.out.println("Select a JPG, JPEG, or PNG image.");
        }
    } else {
        System.out.println("No photo selected.");
    }

} catch (Exception e) {
    System.out.println("Photo error: " + e.getMessage());
}

        String sql = "INSERT INTO complaints(user_id, category, location, description, photo_path) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, CATEGORIES[categoryChoice - 1]);
            ps.setString(3, location);
            ps.setString(4, description);
            ps.setString(5, photoPath);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    System.out.println("Report submitted successfully!");
                    System.out.println("Complaint ID: " + keys.getInt(1));
                    if (photoPath != null) {
                        System.out.println("Photo path recorded. Ensure the file exists on this computer.");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Could not submit report: " + e.getMessage());
        }
    }

    private static void searchByLocation() {
        String location = readNonEmpty("Enter location keyword: ");
        String sql = "SELECT c.complaint_id, c.category, c.location, c.status, c.created_at, u.name " +
                     "FROM complaints c JOIN users u ON c.user_id=u.user_id " +
                     "WHERE c.location LIKE ? ORDER BY c.created_at DESC";
        listComplaints(sql, "%" + location + "%");
    }

    private static void listComplaints(String sql, Object parameter) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parameter instanceof Integer id) {
                ps.setInt(1, id);
            } else if (parameter instanceof String value) {
                ps.setString(1, value);
            }

            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%n%-6s | %-22s | %-25s | %-14s | %-20s%n",
                    "ID", "CATEGORY", "LOCATION", "STATUS", "REPORTED BY");
                System.out.println("-".repeat(100));
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%-6d | %-22.22s | %-25.25s | %-14s | %-20.20s%n",
                        rs.getInt("complaint_id"),
                        rs.getString("category"),
                        rs.getString("location"),
                        rs.getString("status"),
                        rs.getString("name"));
                }
                if (!found) System.out.println("No reports found.");
            }
        } catch (SQLException e) {
            System.out.println("Could not retrieve reports: " + e.getMessage());
        }
    }

    private static void adminMenu() {
        while (true) {
            System.out.println("\n--- CivicEye Authority Dashboard ---");
            System.out.println("1. View All Reports");
            System.out.println("2. View Pending / In Progress Reports");
            System.out.println("3. Update Report Status");
            System.out.println("4. Location-wise Statistics");
            System.out.println("5. Logout");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> listComplaints("SELECT c.complaint_id, c.category, c.location, c.status, c.created_at, u.name FROM complaints c JOIN users u ON c.user_id=u.user_id ORDER BY c.created_at DESC", null);
                case 2 -> listComplaints("SELECT c.complaint_id, c.category, c.location, c.status, c.created_at, u.name FROM complaints c JOIN users u ON c.user_id=u.user_id WHERE c.status <> 'RESOLVED' ORDER BY c.created_at DESC", null);
                case 3 -> updateStatus();
                case 4 -> locationStatistics();
                case 5 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void updateStatus() {
        int id = readInt("Complaint ID: ");
        System.out.println("1. PENDING");
        System.out.println("2. UNDER REVIEW");
        System.out.println("3. IN PROGRESS");
        System.out.println("4. RESOLVED");
        int choice = readInt("New status: ");
        String status = switch (choice) {
            case 1 -> "PENDING";
            case 2 -> "UNDER REVIEW";
            case 3 -> "IN PROGRESS";
            case 4 -> "RESOLVED";
            default -> null;
        };
        if (status == null) {
            System.out.println("Invalid status choice.");
            return;
        }
        String note = readLine("Action / resolution note (optional): ").trim();
        String sql = "UPDATE complaints SET status=?, resolution_note=? WHERE complaint_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, note.isEmpty() ? null : note);
            ps.setInt(3, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Status updated successfully." : "Complaint ID not found.");
        } catch (SQLException e) {
            System.out.println("Could not update status: " + e.getMessage());
        }
    }

    private static void locationStatistics() {
        String sql = "SELECT location, COUNT(*) AS total, " +
            "SUM(status <> 'RESOLVED') AS unresolved, " +
            "SUM(status = 'RESOLVED') AS resolved " +
            "FROM complaints GROUP BY location ORDER BY unresolved DESC, total DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.printf("%n%-30s | %-8s | %-12s | %-8s%n",
                "LOCATION", "TOTAL", "UNRESOLVED", "RESOLVED");
            System.out.println("-".repeat(70));
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-30.30s | %-8d | %-12d | %-8d%n",
                    rs.getString("location"),
                    rs.getInt("total"),
                    rs.getInt("unresolved"),
                    rs.getInt("resolved"));
            }
            if (!found) System.out.println("No complaint data yet.");
        } catch (SQLException e) {
            System.out.println("Could not generate statistics: " + e.getMessage());
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt).trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String readNonEmpty(String prompt) {
        while (true) {
            String value = readLine(prompt).trim();
            if (!value.isEmpty()) return value;
            System.out.println("This field cannot be empty.");
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }
}
