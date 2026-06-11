import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class WebServer {
    private static final int PORT = getPort();

    public static void main(String[] args) throws Exception {
        setupDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        server.createContext("/api/login", new LoginHandler());
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
            createTables(con);
            ensureAdminAccount(con);
            insertDefaultUsers(con);
            insertDefaultStudents(con);
        }
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
                    "rahul@example.com", 2024, 2027, "Studying", 0, "8.2", "", "", "A", "Pass");
            insertStudent(ps, 2, "REG102", "Priya Kumari", "9876501234", "Electrical", 4,
                    "priya@example.com", 2023, 2026, "Studying", 0, "7.8", "", "", "B", "Pass");
            insertStudent(ps, 3, "REG103", "Amit Verma", "9876512345", "Mechanical", 2,
                    "amit@example.com", 2025, 2028, "Studying", 0, "", "", "", "Not Added", "Pending");
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
                send(exchange, 500, "{\"success\":false,\"message\":\"Database error\"}");
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

            if (isBlank(username) || isBlank(password)) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Please fill all account details\"}");
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

            if (isBlank(username) || isBlank(password)) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Please fill all password reset details\"}");
                return;
            }

            if (username.equals("Admin")) {
                send(exchange, 200, "{\"success\":false,\"message\":\"Admin password cannot be changed\"}");
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
                send(exchange, 500, "{\"success\":false,\"message\":\"Database error\"}");
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
                send(exchange, 500, "{\"success\":false,\"message\":\"Database error\"}");
            }
        }
    }

    private static String getStudentsJson() throws SQLException {
        StringBuilder json = new StringBuilder("[");

        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT student_id, registration_number, student_name, phone_number, department_name, semester, email, course_start_year, passout_year, student_status, back_papers, cgpa, ogpa, photo_url, photo_data, grade, result_status "
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

        validateOptionalPointAverage(form.get("cgpa"));
        validateOptionalPointAverage(form.get("ogpa"));
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
        validateOptionalPointAverage(form.get("ogpa"));

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE sms_students SET grade = ?, result_status = ?, cgpa = NVL(?, cgpa), ogpa = NVL(?, ogpa) WHERE student_id = ?")) {
            ps.setString(1, grade);
            ps.setString(2, status);
            setOptionalDouble(ps, 3, form.get("cgpa"));
            setOptionalDouble(ps, 4, form.get("ogpa"));
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    private static void deleteStudent(int id) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM sms_students WHERE student_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
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
