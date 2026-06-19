import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class WebServer {
    private static final int PORT = getPort();
    private static final String GMAIL_USER = System.getenv("GMAIL_USER");
    private static final String GMAIL_APP_PASSWORD = System.getenv("GMAIL_APP_PASSWORD");
    private static final long OTP_VALID_MILLIS = 5 * 60 * 1000;
    private static final int SMTP_TIMEOUT_MILLIS = 10000;
    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static boolean demoMode = false;
    private static final Map<String, String> demoPasswords = new HashMap<String, String>();
    private static final Map<String, String> demoRoles = new HashMap<String, String>();
    private static final Map<String, OtpRecord> otpRecords = new HashMap<String, OtpRecord>();
    private static final List<StudentRecord> demoStudents = new ArrayList<StudentRecord>();

    public static void main(String[] args) throws Exception {
        setupDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/send-otp", new OtpHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/forgot-password", new ForgotPasswordHandler());
        server.createContext("/api/students", new StudentHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("Student Management System website is running.");
        System.out.println("Server is listening on port " + PORT + ".");
    }

    private static int getPort() {
        String port = System.getenv("PORT");

        if (port == null || port.trim().isEmpty()) {
            return 8081;
        }

        return Integer.parseInt(port);
    }

    private static void setupDatabase() throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            if (con == null) {
                activateDemoMode();
                return;
            }

            createTables(con);
            ensureAdminAccount(con);
            insertDefaultUsers(con);
            insertDefaultStudents(con);
        }
    }

    private static void activateDemoMode() {
        demoMode = true;
        demoPasswords.clear();
        demoRoles.clear();
        demoStudents.clear();

        demoPasswords.put("Admin", "Admin123");
        demoRoles.put("Admin", "admin");
        demoPasswords.put("user", "user123");
        demoRoles.put("user", "user");

        demoStudents.add(new StudentRecord(1, "D242523980", "Santanu Chanda", "7400761833", "CST",
                4, "shantanuchanda@gmail.com", 2024, 2027, "Studying", 0, "6.6",
                "5.5", "6.6", "6.6", "", "", "", "", "", "B", "Pass"));
        demoStudents.add(new StudentRecord(2, "D24252390", "Adarsh Darjee", "9884367780", "CST",
                2, "adarshdarjee123@gmail.com", 2024, 2028, "Studying", 0, "7",
                "7", "", "", "", "", "", "", "", "A", "Pass"));
        demoStudents.add(new StudentRecord(3, "D24252678", "Biplab Bauri", "", "CST",
                4, "xstylishbiplab@gmail.com", 2024, 2026, "Studying", 1, "5.5",
                "", "", "5.5", "", "", "", "", "", "C", "Pass"));
        demoStudents.add(new StudentRecord(4, "D252625302", "Param Brata", "9653429834", "ETCE",
                2, "param@gmail.com", 2025, 2028, "Studying", 4, "3.9",
                "3.9", "", "", "", "", "", "", "", "F", "Fail"));
        demoStudents.add(new StudentRecord(5, "D252625311", "Toni Adhikari", "9798909877", "CIVIL",
                2, "tonistark@gmail.com", 2025, 2028, "Studying", 0, "7.4",
                "7.4", "", "", "", "", "", "", "", "A", "Pass"));
        demoStudents.add(new StudentRecord(6, "D242523985", "Aniket Sharma", "9883288372", "CST",
                4, "aniketsharma2971@gmail.com", 2024, 2026, "Studying", 0, "8.4",
                "8", "8.6", "8.4", "", "", "", "", "", "A", "Pass"));
        demoStudents.add(new StudentRecord(7, "D264748289", "Kaushal Rai", "9778666795", "CST",
                6, "kaushal234@gmail.com", 2023, 2026, "Studying", 0, "6.5",
                "4", "4.4", "5.5", "6.6", "6.5", "", "", "", "C", "Pass"));
        demoStudents.add(new StudentRecord(8, "D289768986", "Rajeev Ghaley", "7889887684", "CST",
                6, "rajeevgayal23@gmail.com", 2024, 2027, "Studying", 0, "7.8",
                "6.7", "7.8", "8.4", "7.8", "7.8", "", "", "", "A", "Pass"));
        demoStudents.add(new StudentRecord(9, "D247878478", "Pragyani Chettri", "9880393487", "CST",
                6, "pragyanichettri@gmail.com", 2021, 2025, "Passed Out", 0, "8.8",
                "7", "8", "7.6", "8", "7.6", "8.8", "8.2", "", "A", "Pass"));
        demoStudents.add(new StudentRecord(10, "D247878450", "Himanshu Singha", "8768758938", "CST",
                6, "himanshukau@gmail.com", 2024, 2027, "Passed Out", 2, "6.4",
                "4.5", "6", "7", "7", "7.8", "6.4", "6.2", "", "A", "Pass"));
        demoStudents.add(new StudentRecord(11, "D24787889", "Nayan Kharel", "6298786782", "ETCE",
                6, "nayankharel777@gmail.com", 2021, 2024, "Passed Out", 0, "8.3",
                "8.9", "8.6", "8.7", "8.5", "8.2", "8.3", "8.5", "", "A", "Pass"));
        demoStudents.add(new StudentRecord(12, "D247878789", "Akhil Pradhan", "8768778945", "CST",
                6, "hellopixel@gmail.com", 2021, 2024, "Passed Out", 0, "8.6",
                "7", "7.6", "8.5", "6.5", "7.5", "8.6", "8.2", "", "A", "Pass"));
        demoStudents.add(new StudentRecord(13, "D789874235", "Lopshang Lepcha", "7450934795", "ETCE",
                6, "lopbshang22@gmail.com", 2021, 2024, "Passed Out", 0, "7.5",
                "7", "7.3", "7.4", "7.5", "7.3", "7.5", "7.5", "", "A", "Pass"));

        System.out.println("Oracle database is not available. Running in online demo mode.");
    }

    private static void createTables(Connection con) throws SQLException {
        executeIgnoreTableExists(con,
                "CREATE TABLE sms_users ("
                        + "username VARCHAR2(50) PRIMARY KEY, "
                        + "password VARCHAR2(50) NOT NULL, "
                        + "role VARCHAR2(20) NOT NULL)");

        executeIgnoreTableExists(con,
                "CREATE TABLE sms_students ("
                        + "student_id NUMBER PRIMARY KEY, "
                        + "registration_number VARCHAR2(50) UNIQUE NOT NULL, "
                        + "student_name VARCHAR2(100) NOT NULL, "
                        + "phone_number VARCHAR2(20), "
                        + "department_name VARCHAR2(100) NOT NULL, "
                        + "semester NUMBER NOT NULL, "
                        + "email VARCHAR2(100), "
                        + "course_start_year NUMBER, "
                        + "passout_year NUMBER, "
                        + "student_status VARCHAR2(30), "
                        + "back_papers NUMBER, "
                        + "cgpa NUMBER(4,2), "
                        + "sem1_cgpa NUMBER(4,2), "
                        + "sem2_cgpa NUMBER(4,2), "
                        + "sem3_cgpa NUMBER(4,2), "
                        + "sem4_cgpa NUMBER(4,2), "
                        + "sem5_cgpa NUMBER(4,2), "
                        + "sem6_cgpa NUMBER(4,2), "
                        + "ogpa NUMBER(4,2), "
                        + "photo_url VARCHAR2(500), "
                        + "photo_data CLOB, "
                        + "grade VARCHAR2(20) NOT NULL, "
                        + "result_status VARCHAR2(20) NOT NULL)");

        addColumnIfMissing(con, "sms_students", "phone_number", "VARCHAR2(20)");
        addColumnIfMissing(con, "sms_students", "email", "VARCHAR2(100)");
        addColumnIfMissing(con, "sms_students", "course_start_year", "NUMBER");
        addColumnIfMissing(con, "sms_students", "passout_year", "NUMBER");
        addColumnIfMissing(con, "sms_students", "student_status", "VARCHAR2(30)");
        addColumnIfMissing(con, "sms_students", "back_papers", "NUMBER");
        addColumnIfMissing(con, "sms_students", "cgpa", "NUMBER(4,2)");
        addColumnIfMissing(con, "sms_students", "sem1_cgpa", "NUMBER(4,2)");
        addColumnIfMissing(con, "sms_students", "sem2_cgpa", "NUMBER(4,2)");
        addColumnIfMissing(con, "sms_students", "sem3_cgpa", "NUMBER(4,2)");
        addColumnIfMissing(con, "sms_students", "sem4_cgpa", "NUMBER(4,2)");
        addColumnIfMissing(con, "sms_students", "sem5_cgpa", "NUMBER(4,2)");
        addColumnIfMissing(con, "sms_students", "sem6_cgpa", "NUMBER(4,2)");
        addColumnIfMissing(con, "sms_students", "ogpa", "NUMBER(4,2)");
        addColumnIfMissing(con, "sms_students", "photo_url", "VARCHAR2(500)");
        addColumnIfMissing(con, "sms_students", "photo_data", "CLOB");
        updateDefaultStudentInfo(con);
    }

    private static void executeIgnoreTableExists(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            if (e.getErrorCode() != 955) {
                throw e;
            }
        }
    }

    private static void addColumnIfMissing(Connection con, String tableName, String columnName, String dataType)
            throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("ALTER TABLE " + tableName + " ADD " + columnName + " " + dataType);
        } catch (SQLException e) {
            if (e.getErrorCode() != 1430) {
                throw e;
            }
        }
    }

    private static void updateDefaultStudentInfo(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("UPDATE sms_students SET course_start_year = 2024 WHERE course_start_year IS NULL");
            st.executeUpdate("UPDATE sms_students SET passout_year = 2027 WHERE passout_year IS NULL");
            st.executeUpdate("UPDATE sms_students SET student_status = 'Studying' WHERE student_status IS NULL");
            st.executeUpdate("UPDATE sms_students SET back_papers = 0 WHERE back_papers IS NULL");
        }
    }

    private static void insertDefaultUsers(Connection con) throws SQLException {
        insertUserIfMissing(con, "user", "user123", "user");
    }

    private static void ensureAdminAccount(Connection con) throws SQLException {
        try (PreparedStatement deleteOldAdmin = con.prepareStatement(
                "DELETE FROM sms_users WHERE username = ?")) {
            deleteOldAdmin.setString(1, "admin");
            deleteOldAdmin.executeUpdate();
        }

        try (PreparedStatement updateAdmin = con.prepareStatement(
                "UPDATE sms_users SET password = ?, role = ? WHERE username = ?")) {
            updateAdmin.setString(1, "Admin123");
            updateAdmin.setString(2, "admin");
            updateAdmin.setString(3, "Admin");
            int updatedRows = updateAdmin.executeUpdate();

            if (updatedRows == 0) {
                try (PreparedStatement insertAdmin = con.prepareStatement(
                        "INSERT INTO sms_users (username, password, role) VALUES (?, ?, ?)")) {
                    insertUser(insertAdmin, "Admin", "Admin123", "admin");
                }
            }
        }
    }

    private static void insertUserIfMissing(Connection con, String username, String password, String role)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO sms_users (username, password, role) VALUES (?, ?, ?)")) {
            insertUser(ps, username, password, role);
        } catch (SQLException e) {
            if (e.getErrorCode() != 1) {
                throw e;
            }
        }
    }

    private static void insertUser(PreparedStatement ps, String username, String password, String role)
            throws SQLException {
        ps.setString(1, username);
        ps.setString(2, password);
        ps.setString(3, role);
        ps.executeUpdate();
    }

    private static void insertDefaultStudents(Connection con) throws SQLException {
        if (countRows(con, "sms_students") > 0) {
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO sms_students "
                        + "(student_id, registration_number, student_name, phone_number, department_name, semester, email, course_start_year, passout_year, student_status, back_papers, cgpa, ogpa, photo_url, grade, result_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insertStudent(ps, 1, "REG101", "Rahul Sharma", "9876543210", "Computer", 3,
                    "rahul@gmail.com", 2024, 2027, "Studying", 0, "8.2", "", "", "A", "Pass");
            insertStudent(ps, 2, "REG102", "Priya Kumari", "9876501234", "Electrical", 4,
                    "priya@yahoo.com", 2023, 2026, "Studying", 0, "7.8", "", "", "B", "Pass");
            insertStudent(ps, 3, "REG103", "Amit Verma", "9876512345", "Mechanical", 2,
                    "amit@outlook.com", 2025, 2028, "Studying", 0, "", "", "", "Not Added", "Pending");
        }
    }

    private static void insertStudent(PreparedStatement ps, int id, String regNo, String name,
                                      String phone, String department, int semester, String email,
                                      int courseStartYear, int passoutYear, String studentStatus,
                                      int backPapers, String cgpa, String ogpa, String photo, String grade, String status)
            throws SQLException {
        ps.setInt(1, id);
        ps.setString(2, regNo);
        ps.setString(3, name);
        ps.setString(4, phone);
        ps.setString(5, department);
        ps.setInt(6, semester);
        ps.setString(7, email);
        ps.setInt(8, courseStartYear);
        ps.setInt(9, passoutYear);
        ps.setString(10, studentStatus);
        ps.setInt(11, backPapers);
        setOptionalDouble(ps, 12, cgpa);
        setOptionalDouble(ps, 13, ogpa);
        ps.setString(14, photo);
        ps.setString(15, grade);
        ps.setString(16, status);
        ps.executeUpdate();
    }

    private static int countRows(Connection con, String tableName) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                send(exchange, 405, "{\"success\":false}");
                return;
            }

            Map<String, String> form = readForm(exchange);
            String username = form.get("username");
            String password = form.get("password");

            if (demoMode) {
                String savedPassword = demoPasswords.get(username);
                if (savedPassword != null && savedPassword.equals(password)) {
                    send(exchange, 200, "{\"success\":true,\"role\":\"" + escapeJson(demoRoles.get(username)) + "\"}");
                } else {
                    send(exchange, 200, "{\"success\":false}");
                }
                return;
            }

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT role FROM sms_users WHERE username = ? AND password = ?")) {
                ps.setString(1, username);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        send(exchange, 200, "{\"success\":true,\"role\":\"" + escapeJson(rs.getString("role")) + "\"}");
                    } else {
                        send(exchange, 200, "{\"success\":false}");
                    }
                }
            } catch (SQLException e) {
                send(exchange, 200, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static class OtpHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                send(exchange, 405, "{\"success\":false}");
                return;
            }

            Map<String, String> form = readForm(exchange);
            String username = form.get("username");
            String email = form.get("email");
            String purpose = form.get("purpose");

            if (isBlank(username) || isBlank(email) || isBlank(purpose)) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Please enter username and Gmail\"}");
                return;
            }

            if (!email.matches("[A-Za-z0-9._%+-]+@gmail\\.com")) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Please enter a valid Gmail address\"}");
                return;
            }

            if (!isGmailConfigured()) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Gmail OTP is not configured on Render\"}");
                return;
            }

            String otp = String.valueOf(100000 + OTP_RANDOM.nextInt(900000));
            otpRecords.put(getOtpKey(purpose, username, email),
                    new OtpRecord(otp, System.currentTimeMillis() + OTP_VALID_MILLIS));

            try {
                sendOtpMail(email, otp, purpose);
                send(exchange, 200, "{\"success\":true,\"message\":\"OTP sent to Gmail. It is valid for 5 minutes.\"}");
            } catch (IOException e) {
                System.out.println("OTP email error: " + e.getMessage());
                send(exchange, 200, "{\"success\":false,\"message\":\"OTP email could not be sent: "
                        + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static class RegisterHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                send(exchange, 405, "{\"success\":false}");
                return;
            }

            Map<String, String> form = readForm(exchange);
            String username = form.get("username");
            String password = form.get("password");
            String email = form.get("email");
            String otp = form.get("otp");

            if (isBlank(username) || isBlank(password) || isBlank(email) || isBlank(otp)) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Please fill all account and OTP details\"}");
                return;
            }

            if (!verifyOtp("create", username, email, otp)) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Invalid or expired OTP\"}");
                return;
            }

            if (demoMode) {
                if (username.equalsIgnoreCase("admin")) {
                    send(exchange, 200, "{\"success\":false,\"message\":\"Admin account is already fixed\"}");
                } else if (demoPasswords.containsKey(username)) {
                    send(exchange, 200, "{\"success\":false,\"message\":\"Username already exists\"}");
                } else {
                    demoPasswords.put(username, password);
                    demoRoles.put(username, "user");
                    send(exchange, 200, "{\"success\":true}");
                }
                return;
            }

            if (username.equalsIgnoreCase("admin")) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Admin account is already fixed\"}");
                return;
            }

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "INSERT INTO sms_users (username, password, role) VALUES (?, ?, ?)")) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, "user");
                ps.executeUpdate();
                send(exchange, 200, "{\"success\":true}");
            } catch (SQLException e) {
                if (e.getErrorCode() == 1) {
                    send(exchange, 200, "{\"success\":false,\"message\":\"Username already exists\"}");
                } else {
                    send(exchange, 500, "{\"success\":false,\"message\":\"Database error\"}");
                }
            }
        }
    }

    private static class ForgotPasswordHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                send(exchange, 405, "{\"success\":false}");
                return;
            }

            Map<String, String> form = readForm(exchange);
            String username = form.get("username");
            String password = form.get("password");
            String email = form.get("email");
            String otp = form.get("otp");

            if (isBlank(username) || isBlank(password) || isBlank(email) || isBlank(otp)) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Please fill all password reset and OTP details\"}");
                return;
            }

            if (username.equalsIgnoreCase("admin")) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Admin password cannot be changed\"}");
                return;
            }

            if (!verifyOtp("forgot", username, email, otp)) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Invalid or expired OTP\"}");
                return;
            }

            if (demoMode) {
                if (!demoPasswords.containsKey(username)) {
                    send(exchange, 200, "{\"success\":false,\"message\":\"Username not found\"}");
                } else {
                    demoPasswords.put(username, password);
                    send(exchange, 200, "{\"success\":true}");
                }
                return;
            }

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "UPDATE sms_users SET password = ? WHERE username = ?")) {
                ps.setString(1, password);
                ps.setString(2, username);
                int updatedRows = ps.executeUpdate();

                if (updatedRows == 0) {
                    send(exchange, 200, "{\"success\":false,\"message\":\"Username not found\"}");
                } else {
                    send(exchange, 200, "{\"success\":true}");
                }
            } catch (SQLException e) {
                send(exchange, 200, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static class StudentHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            try {
                if (method.equalsIgnoreCase("GET") && path.equals("/api/students")) {
                    send(exchange, 200, getStudentsJson());
                } else if (method.equalsIgnoreCase("POST") && path.equals("/api/students")) {
                    addStudent(readForm(exchange));
                    send(exchange, 200, "{\"success\":true}");
                } else if (method.equalsIgnoreCase("PUT") && path.matches("/api/students/[0-9]+")) {
                    int id = getIdFromPath(path);
                    updateStudent(id, readForm(exchange));
                    send(exchange, 200, "{\"success\":true}");
                } else if (method.equalsIgnoreCase("PUT") && path.matches("/api/students/[0-9]+/grade")) {
                    int id = getIdFromPath(path);
                    updateGrade(id, readForm(exchange));
                    send(exchange, 200, "{\"success\":true}");
                } else if (method.equalsIgnoreCase("DELETE") && path.matches("/api/students/[0-9]+")) {
                    int id = getIdFromPath(path);
                    deleteStudent(id);
                    send(exchange, 200, "{\"success\":true}");
                } else {
                    send(exchange, 404, "{\"success\":false}");
                }
            } catch (SQLException e) {
                send(exchange, 200, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static String getStudentsJson() throws SQLException {
        if (demoMode) {
            return getDemoStudentsJson();
        }

        StringBuilder json = new StringBuilder("[");

        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT student_id, registration_number, student_name, phone_number, department_name, semester, email, course_start_year, passout_year, student_status, back_papers, cgpa, sem1_cgpa, sem2_cgpa, sem3_cgpa, sem4_cgpa, sem5_cgpa, sem6_cgpa, ogpa, photo_url, photo_data, grade, result_status "
                             + "FROM sms_students ORDER BY student_id")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                String photo = rs.getString("photo_data");
                if (photo == null || photo.isEmpty()) {
                    photo = rs.getString("photo_url");
                }

                json.append("{")
                        .append("\"id\":").append(rs.getInt("student_id")).append(",")
                        .append("\"regNo\":\"").append(escapeJson(rs.getString("registration_number"))).append("\",")
                        .append("\"name\":\"").append(escapeJson(rs.getString("student_name"))).append("\",")
                        .append("\"phone\":\"").append(escapeJson(rs.getString("phone_number"))).append("\",")
                        .append("\"department\":\"").append(escapeJson(rs.getString("department_name"))).append("\",")
                        .append("\"semester\":").append(rs.getInt("semester")).append(",")
                        .append("\"email\":\"").append(escapeJson(rs.getString("email"))).append("\",")
                        .append("\"courseStartYear\":").append(rs.getInt("course_start_year")).append(",")
                        .append("\"passoutYear\":").append(rs.getInt("passout_year")).append(",")
                        .append("\"studentStatus\":\"").append(escapeJson(rs.getString("student_status"))).append("\",")
                        .append("\"backPapers\":").append(rs.getInt("back_papers")).append(",")
                        .append("\"cgpa\":\"").append(escapeJson(getOptionalNumber(rs, "cgpa"))).append("\",")
                        .append("\"sem1Cgpa\":\"").append(escapeJson(getOptionalNumber(rs, "sem1_cgpa"))).append("\",")
                        .append("\"sem2Cgpa\":\"").append(escapeJson(getOptionalNumber(rs, "sem2_cgpa"))).append("\",")
                        .append("\"sem3Cgpa\":\"").append(escapeJson(getOptionalNumber(rs, "sem3_cgpa"))).append("\",")
                        .append("\"sem4Cgpa\":\"").append(escapeJson(getOptionalNumber(rs, "sem4_cgpa"))).append("\",")
                        .append("\"sem5Cgpa\":\"").append(escapeJson(getOptionalNumber(rs, "sem5_cgpa"))).append("\",")
                        .append("\"sem6Cgpa\":\"").append(escapeJson(getOptionalNumber(rs, "sem6_cgpa"))).append("\",")
                        .append("\"ogpa\":\"").append(escapeJson(getOptionalNumber(rs, "ogpa"))).append("\",")
                        .append("\"photo\":\"").append(escapeJson(photo)).append("\",")
                        .append("\"grade\":\"").append(escapeJson(rs.getString("grade"))).append("\",")
                        .append("\"status\":\"").append(escapeJson(rs.getString("result_status"))).append("\"")
                        .append("}");
            }
        }

        json.append("]");
        return json.toString();
    }

    private static void addStudent(Map<String, String> form) throws SQLException {
        validateStudentForm(form);
        ensureUniqueRegistrationNumber(form.get("regNo"), 0);

        if (demoMode) {
            demoStudents.add(new StudentRecord(nextDemoStudentId(), form.get("regNo"), form.get("name"),
                    form.get("phone"), form.get("department"), Integer.parseInt(form.get("semester")),
                    form.get("email"), Integer.parseInt(form.get("courseStartYear")),
                    Integer.parseInt(form.get("passoutYear")), form.get("studentStatus"),
                    Integer.parseInt(form.get("backPapers")), blankToEmpty(form.get("cgpa")),
                    "", "", "", "", "", "", blankToEmpty(form.get("ogpa")),
                    blankToEmpty(form.get("photo")), "Not Added", "Pending"));
            return;
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO sms_students "
                             + "(student_id, registration_number, student_name, phone_number, department_name, semester, email, course_start_year, passout_year, student_status, back_papers, cgpa, ogpa, photo_data, grade, result_status) "
                             + "VALUES ((SELECT NVL(MAX(student_id), 0) + 1 FROM sms_students), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Not Added', 'Pending')")) {
            ps.setString(1, form.get("regNo"));
            ps.setString(2, form.get("name"));
            ps.setString(3, form.get("phone"));
            ps.setString(4, form.get("department"));
            ps.setInt(5, Integer.parseInt(form.get("semester")));
            ps.setString(6, form.get("email"));
            ps.setInt(7, Integer.parseInt(form.get("courseStartYear")));
            ps.setInt(8, Integer.parseInt(form.get("passoutYear")));
            ps.setString(9, form.get("studentStatus"));
            ps.setInt(10, Integer.parseInt(form.get("backPapers")));
            setOptionalDouble(ps, 11, form.get("cgpa"));
            setOptionalDouble(ps, 12, form.get("ogpa"));
            ps.setString(13, form.get("photo"));
            ps.executeUpdate();
        }
    }

    private static void updateStudent(int id, Map<String, String> form) throws SQLException {
        validateStudentForm(form);
        ensureUniqueRegistrationNumber(form.get("regNo"), id);

        if (demoMode) {
            StudentRecord student = findDemoStudent(id);
            if (student != null) {
                student.regNo = form.get("regNo");
                student.name = form.get("name");
                student.phone = form.get("phone");
                student.department = form.get("department");
                student.semester = Integer.parseInt(form.get("semester"));
                student.email = form.get("email");
                student.courseStartYear = Integer.parseInt(form.get("courseStartYear"));
                student.passoutYear = Integer.parseInt(form.get("passoutYear"));
                student.studentStatus = form.get("studentStatus");
                student.backPapers = Integer.parseInt(form.get("backPapers"));
                student.cgpa = blankToEmpty(form.get("cgpa"));
                student.ogpa = blankToEmpty(form.get("ogpa"));
                student.photo = blankToEmpty(form.get("photo"));
            }
            return;
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE sms_students SET registration_number = ?, student_name = ?, phone_number = ?, "
                             + "department_name = ?, semester = ?, email = ?, course_start_year = ?, "
                             + "passout_year = ?, student_status = ?, back_papers = ?, cgpa = ?, ogpa = ?, photo_data = ? WHERE student_id = ?")) {
            ps.setString(1, form.get("regNo"));
            ps.setString(2, form.get("name"));
            ps.setString(3, form.get("phone"));
            ps.setString(4, form.get("department"));
            ps.setInt(5, Integer.parseInt(form.get("semester")));
            ps.setString(6, form.get("email"));
            ps.setInt(7, Integer.parseInt(form.get("courseStartYear")));
            ps.setInt(8, Integer.parseInt(form.get("passoutYear")));
            ps.setString(9, form.get("studentStatus"));
            ps.setInt(10, Integer.parseInt(form.get("backPapers")));
            setOptionalDouble(ps, 11, form.get("cgpa"));
            setOptionalDouble(ps, 12, form.get("ogpa"));
            ps.setString(13, form.get("photo"));
            ps.setInt(14, id);
            ps.executeUpdate();
        }
    }

    private static void validateStudentForm(Map<String, String> form) throws SQLException {
        String phone = form.get("phone");
        String backPapers = form.get("backPapers");
        String name = form.get("name");
        String semester = form.get("semester");
        String email = form.get("email");
        String courseStartYear = form.get("courseStartYear");
        String passoutYear = form.get("passoutYear");

        if (phone == null || !phone.matches("[0-9]{1,10}")) {
            throw new SQLException("Invalid phone number");
        }

        if (name == null || name.matches(".*[0-9].*")) {
            throw new SQLException("Invalid student name");
        }

        if (semester == null || !semester.matches("[1-6]")) {
            throw new SQLException("Invalid semester");
        }

        if (email == null || !email.matches("[A-Za-z0-9._%+-]+@(gmail|yahoo|outlook)\\.com")) {
            throw new SQLException("Invalid email");
        }

        if (backPapers == null || !backPapers.matches("[0-9]+")) {
            throw new SQLException("Invalid back papers");
        }

        if (courseStartYear == null || !courseStartYear.matches("[0-9]+")
                || passoutYear == null || !passoutYear.matches("[0-9]+")) {
            throw new SQLException("Course years cannot be negative");
        }

        validateOptionalPointAverage(form.get("cgpa"));
        validateOptionalPointAverage(form.get("ogpa"));
    }

    private static void ensureUniqueRegistrationNumber(String regNo, int currentStudentId) throws SQLException {
        if (isBlank(regNo)) {
            throw new SQLException("Registration number is required");
        }

        if (demoMode) {
            for (StudentRecord student : demoStudents) {
                if (student.regNo.equalsIgnoreCase(regNo) && student.id != currentStudentId) {
                    throw new SQLException("Registration number already exists");
                }
            }
            return;
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM sms_students WHERE LOWER(registration_number) = LOWER(?) AND student_id <> ?")) {
            ps.setString(1, regNo);
            ps.setInt(2, currentStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new SQLException("Registration number already exists");
                }
            }
        }
    }

    private static void validateOptionalPointAverage(String value) throws SQLException {
        if (isBlank(value)) {
            return;
        }

        try {
            double number = Double.parseDouble(value);
            if (number < 0 || number > 10) {
                throw new SQLException("Invalid point average");
            }
        } catch (NumberFormatException e) {
            throw new SQLException("Invalid point average");
        }
    }

    private static void setOptionalDouble(PreparedStatement ps, int index, String value) throws SQLException {
        if (isBlank(value)) {
            ps.setNull(index, java.sql.Types.NUMERIC);
        } else {
            ps.setDouble(index, Double.parseDouble(value));
        }
    }

    private static String getOptionalNumber(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        if (rs.wasNull()) {
            return "";
        }
        return String.valueOf(value);
    }

    private static void updateGrade(int id, Map<String, String> form) throws SQLException {
        String grade = form.get("grade");
        String status = grade.equalsIgnoreCase("F") ? "Fail" : "Pass";
        validateOptionalPointAverage(form.get("cgpa"));
        validateOptionalPointAverage(form.get("sem1Cgpa"));
        validateOptionalPointAverage(form.get("sem2Cgpa"));
        validateOptionalPointAverage(form.get("sem3Cgpa"));
        validateOptionalPointAverage(form.get("sem4Cgpa"));
        validateOptionalPointAverage(form.get("sem5Cgpa"));
        validateOptionalPointAverage(form.get("sem6Cgpa"));
        validateOptionalPointAverage(form.get("ogpa"));

        if (demoMode) {
            StudentRecord student = findDemoStudent(id);
            if (student != null) {
                student.grade = grade;
                student.status = status;
                if (!isBlank(form.get("cgpa"))) {
                    student.cgpa = form.get("cgpa");
                }
                if (!isBlank(form.get("sem1Cgpa"))) {
                    student.sem1Cgpa = form.get("sem1Cgpa");
                }
                if (!isBlank(form.get("sem2Cgpa"))) {
                    student.sem2Cgpa = form.get("sem2Cgpa");
                }
                if (!isBlank(form.get("sem3Cgpa"))) {
                    student.sem3Cgpa = form.get("sem3Cgpa");
                }
                if (!isBlank(form.get("sem4Cgpa"))) {
                    student.sem4Cgpa = form.get("sem4Cgpa");
                }
                if (!isBlank(form.get("sem5Cgpa"))) {
                    student.sem5Cgpa = form.get("sem5Cgpa");
                }
                if (!isBlank(form.get("sem6Cgpa"))) {
                    student.sem6Cgpa = form.get("sem6Cgpa");
                }
                if (!isBlank(form.get("ogpa"))) {
                    student.ogpa = form.get("ogpa");
                }
            }
            return;
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE sms_students SET grade = ?, result_status = ?, cgpa = NVL(?, cgpa), "
                             + "sem1_cgpa = NVL(?, sem1_cgpa), sem2_cgpa = NVL(?, sem2_cgpa), "
                             + "sem3_cgpa = NVL(?, sem3_cgpa), sem4_cgpa = NVL(?, sem4_cgpa), "
                             + "sem5_cgpa = NVL(?, sem5_cgpa), sem6_cgpa = NVL(?, sem6_cgpa), "
                             + "ogpa = NVL(?, ogpa) WHERE student_id = ?")) {
            ps.setString(1, grade);
            ps.setString(2, status);
            setOptionalDouble(ps, 3, form.get("cgpa"));
            setOptionalDouble(ps, 4, form.get("sem1Cgpa"));
            setOptionalDouble(ps, 5, form.get("sem2Cgpa"));
            setOptionalDouble(ps, 6, form.get("sem3Cgpa"));
            setOptionalDouble(ps, 7, form.get("sem4Cgpa"));
            setOptionalDouble(ps, 8, form.get("sem5Cgpa"));
            setOptionalDouble(ps, 9, form.get("sem6Cgpa"));
            setOptionalDouble(ps, 10, form.get("ogpa"));
            ps.setInt(11, id);
            ps.executeUpdate();
        }
    }

    private static void deleteStudent(int id) throws SQLException {
        if (demoMode) {
            StudentRecord student = findDemoStudent(id);
            if (student != null) {
                demoStudents.remove(student);
            }
            return;
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM sms_students WHERE student_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static String getDemoStudentsJson() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < demoStudents.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(demoStudents.get(i).toJson());
        }
        json.append("]");
        return json.toString();
    }

    private static int nextDemoStudentId() {
        int maxId = 0;
        for (StudentRecord student : demoStudents) {
            if (student.id > maxId) {
                maxId = student.id;
            }
        }
        return maxId + 1;
    }

    private static StudentRecord findDemoStudent(int id) {
        for (StudentRecord student : demoStudents) {
            if (student.id == id) {
                return student;
            }
        }
        return null;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean isGmailConfigured() {
        return !isBlank(GMAIL_USER) && !isBlank(GMAIL_APP_PASSWORD);
    }

    private static String getOtpKey(String purpose, String username, String email) {
        return purpose.toLowerCase() + ":" + username.toLowerCase() + ":" + email.toLowerCase();
    }

    private static boolean verifyOtp(String purpose, String username, String email, String otp) {
        String key = getOtpKey(purpose, username, email);
        OtpRecord record = otpRecords.get(key);

        if (record == null || System.currentTimeMillis() > record.expiresAt || !record.otp.equals(otp)) {
            return false;
        }

        otpRecords.remove(key);
        return true;
    }

    private static void sendOtpMail(String toEmail, String otp, String purpose) throws IOException {
        String subject = purpose.equals("forgot")
                ? "Student Management System Password Reset OTP"
                : "Student Management System Account OTP";
        String body = "Your Student Management System OTP is " + otp
                + ". It is valid for 5 minutes. Do not share this OTP.";
        String gmailUser = GMAIL_USER.trim();
        String gmailAppPassword = GMAIL_APP_PASSWORD.replace(" ", "").trim();

        try {
            sendOtpMailWithSsl(toEmail, subject, body, gmailUser, gmailAppPassword);
        } catch (IOException sslError) {
            try {
                sendOtpMailWithStartTls(toEmail, subject, body, gmailUser, gmailAppPassword);
            } catch (IOException startTlsError) {
                throw new IOException("SSL 465 failed: " + sslError.getMessage()
                        + " | STARTTLS 587 failed: " + startTlsError.getMessage());
            }
        }
    }

    private static void sendOtpMailWithSsl(String toEmail, String subject, String body,
                                           String gmailUser, String gmailAppPassword) throws IOException {
        try (SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket()) {
            socket.connect(new InetSocketAddress("smtp.gmail.com", 465), SMTP_TIMEOUT_MILLIS);
            socket.setSoTimeout(SMTP_TIMEOUT_MILLIS);
            socket.startHandshake();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            sendAuthenticatedEmail(reader, writer, toEmail, subject, body, gmailUser, gmailAppPassword, true);
        }
    }

    private static void sendOtpMailWithStartTls(String toEmail, String subject, String body,
                                                String gmailUser, String gmailAppPassword) throws IOException {
        try (Socket plainSocket = new Socket()) {
            plainSocket.connect(new InetSocketAddress("smtp.gmail.com", 587), SMTP_TIMEOUT_MILLIS);
            plainSocket.setSoTimeout(SMTP_TIMEOUT_MILLIS);

            BufferedReader plainReader = new BufferedReader(new InputStreamReader(plainSocket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter plainWriter = new PrintWriter(plainSocket.getOutputStream(), true);

            readExpectedSmtpResponse(plainReader, "220");
            sendSmtpCommand(plainWriter, plainReader, "EHLO student-management-system", "250");
            sendSmtpCommand(plainWriter, plainReader, "STARTTLS", "220");

            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(plainSocket, "smtp.gmail.com", 587, true);
            sslSocket.setSoTimeout(SMTP_TIMEOUT_MILLIS);
            sslSocket.startHandshake();

            BufferedReader reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(sslSocket.getOutputStream(), true);

            sendAuthenticatedEmail(reader, writer, toEmail, subject, body, gmailUser, gmailAppPassword, false);
        }
    }

    private static void sendAuthenticatedEmail(BufferedReader reader, PrintWriter writer, String toEmail,
                                               String subject, String body, String gmailUser,
                                               String gmailAppPassword, boolean readGreeting) throws IOException {
        if (readGreeting) {
            readExpectedSmtpResponse(reader, "220");
        }
        sendSmtpCommand(writer, reader, "EHLO student-management-system", "250");
        sendSmtpCommand(writer, reader, "AUTH LOGIN", "334");
        sendSmtpCommand(writer, reader, Base64.getEncoder().encodeToString(gmailUser.getBytes(StandardCharsets.UTF_8)), "334");
        sendSmtpCommand(writer, reader, Base64.getEncoder().encodeToString(gmailAppPassword.getBytes(StandardCharsets.UTF_8)), "235");
        sendSmtpCommand(writer, reader, "MAIL FROM:<" + gmailUser + ">", "250");
        sendSmtpCommand(writer, reader, "RCPT TO:<" + toEmail + ">");
        sendSmtpCommand(writer, reader, "DATA", "354");

        writer.print("From: Student Management System <" + gmailUser + ">\r\n");
        writer.print("To: " + toEmail + "\r\n");
        writer.print("Subject: " + subject + "\r\n");
        writer.print("Content-Type: text/plain; charset=UTF-8\r\n");
        writer.print("\r\n");
        writer.print(body + "\r\n");
        writer.print(".\r\n");
        writer.flush();
        readExpectedSmtpResponse(reader, "250");
        sendSmtpCommand(writer, reader, "QUIT", "221");
    }

    private static void sendSmtpCommand(PrintWriter writer, BufferedReader reader, String command) throws IOException {
        sendSmtpCommand(writer, reader, command, "250");
    }

    private static void sendSmtpCommand(PrintWriter writer, BufferedReader reader, String command, String expectedCode) throws IOException {
        writer.print(command + "\r\n");
        writer.flush();
        readExpectedSmtpResponse(reader, expectedCode);
    }

    private static void readExpectedSmtpResponse(BufferedReader reader, String expectedCode) throws IOException {
        String response = readSmtpResponse(reader);
        if (!response.startsWith(expectedCode)) {
            throw new IOException(response);
        }
    }

    private static String readSmtpResponse(BufferedReader reader) throws IOException {
        String line;
        StringBuilder response = new StringBuilder();
        do {
            line = reader.readLine();
            if (line == null) {
                throw new IOException("SMTP server closed connection");
            }
            if (response.length() > 0) {
                response.append(" ");
            }
            response.append(line);
        } while (line.length() > 3 && line.charAt(3) == '-');
        return response.toString();
    }

    private static class OtpRecord {
        private final String otp;
        private final long expiresAt;

        OtpRecord(String otp, long expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }

    private static class StudentRecord {
        private int id;
        private String regNo;
        private String name;
        private String phone;
        private String department;
        private int semester;
        private String email;
        private int courseStartYear;
        private int passoutYear;
        private String studentStatus;
        private int backPapers;
        private String cgpa;
        private String sem1Cgpa;
        private String sem2Cgpa;
        private String sem3Cgpa;
        private String sem4Cgpa;
        private String sem5Cgpa;
        private String sem6Cgpa;
        private String ogpa;
        private String photo;
        private String grade;
        private String status;

        StudentRecord(int id, String regNo, String name, String phone, String department, int semester,
                      String email, int courseStartYear, int passoutYear, String studentStatus,
                      int backPapers, String cgpa, String sem1Cgpa, String sem2Cgpa, String sem3Cgpa,
                      String sem4Cgpa, String sem5Cgpa, String sem6Cgpa, String ogpa, String photo,
                      String grade, String status) {
            this.id = id;
            this.regNo = regNo;
            this.name = name;
            this.phone = phone;
            this.department = department;
            this.semester = semester;
            this.email = email;
            this.courseStartYear = courseStartYear;
            this.passoutYear = passoutYear;
            this.studentStatus = studentStatus;
            this.backPapers = backPapers;
            this.cgpa = cgpa;
            this.sem1Cgpa = sem1Cgpa;
            this.sem2Cgpa = sem2Cgpa;
            this.sem3Cgpa = sem3Cgpa;
            this.sem4Cgpa = sem4Cgpa;
            this.sem5Cgpa = sem5Cgpa;
            this.sem6Cgpa = sem6Cgpa;
            this.ogpa = ogpa;
            this.photo = photo;
            this.grade = grade;
            this.status = status;
        }

        private String toJson() {
            return "{"
                    + "\"id\":" + id + ","
                    + "\"regNo\":\"" + escapeJson(regNo) + "\","
                    + "\"name\":\"" + escapeJson(name) + "\","
                    + "\"phone\":\"" + escapeJson(phone) + "\","
                    + "\"department\":\"" + escapeJson(department) + "\","
                    + "\"semester\":" + semester + ","
                    + "\"email\":\"" + escapeJson(email) + "\","
                    + "\"courseStartYear\":" + courseStartYear + ","
                    + "\"passoutYear\":" + passoutYear + ","
                    + "\"studentStatus\":\"" + escapeJson(studentStatus) + "\","
                    + "\"backPapers\":" + backPapers + ","
                    + "\"cgpa\":\"" + escapeJson(cgpa) + "\","
                    + "\"sem1Cgpa\":\"" + escapeJson(sem1Cgpa) + "\","
                    + "\"sem2Cgpa\":\"" + escapeJson(sem2Cgpa) + "\","
                    + "\"sem3Cgpa\":\"" + escapeJson(sem3Cgpa) + "\","
                    + "\"sem4Cgpa\":\"" + escapeJson(sem4Cgpa) + "\","
                    + "\"sem5Cgpa\":\"" + escapeJson(sem5Cgpa) + "\","
                    + "\"sem6Cgpa\":\"" + escapeJson(sem6Cgpa) + "\","
                    + "\"ogpa\":\"" + escapeJson(ogpa) + "\","
                    + "\"photo\":\"" + escapeJson(photo) + "\","
                    + "\"grade\":\"" + escapeJson(grade) + "\","
                    + "\"status\":\"" + escapeJson(status) + "\""
                    + "}";
        }
    }

    private static int getIdFromPath(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[3]);
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new HashMap<String, String>();

        if (body.trim().isEmpty()) {
            return form;
        }

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
            form.put(key, value);
        }

        return form;
    }

    private static class StaticFileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String requestedPath = exchange.getRequestURI().getPath();
            if (requestedPath.equals("/")) {
                requestedPath = "/index.html";
            }

            Path filePath = Path.of("." + requestedPath).normalize();
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendText(exchange, 404, "File not found", "text/plain");
                return;
            }

            String contentType = getContentType(filePath.toString());
            byte[] fileBytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html";
        }
        if (fileName.endsWith(".css")) {
            return "text/css";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript";
        }
        return "text/plain";
    }

    private static void send(HttpExchange exchange, int statusCode, String json) throws IOException {
        sendText(exchange, statusCode, json, "application/json");
    }

    private static void sendText(HttpExchange exchange, int statusCode, String text, String contentType)
            throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
