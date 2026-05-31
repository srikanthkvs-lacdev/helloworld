import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class Server {

    static Connection conn;

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:mem:dashboarddb;DB_CLOSE_DELAY=-1", "sa", "");
        initDb();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/auth/login", new Handler() { public void handle(HttpExchange ex) throws IOException { handleLogin(ex); } });
        server.createContext("/api/summary",    new Handler() { public void handle(HttpExchange ex) throws IOException { handleSummary(ex); } });
        server.createContext("/api/activities", new Handler() { public void handle(HttpExchange ex) throws IOException { routeActivities(ex); } });
        server.createContext("/api/policies",   new Handler() { public void handle(HttpExchange ex) throws IOException { routePolicies(ex); } });
        server.createContext("/api/users",      new Handler() { public void handle(HttpExchange ex) throws IOException { routeUsers(ex); } });
        server.createContext("/api/search",     new Handler() { public void handle(HttpExchange ex) throws IOException { handleSearch(ex); } });
        server.createContext("/api/settings",   new Handler() { public void handle(HttpExchange ex) throws IOException { routeSettings(ex); } });
        server.setExecutor(null);
        server.start();
        System.out.println("Backend running at http://localhost:8080");
        System.out.println("Login: user@example.com / password123");
    }

    // ── Database setup ────────────────────────────────────────────────────────

    static void initDb() throws SQLException {
        Statement s = conn.createStatement();
        s.execute("CREATE TABLE app_user (id INT PRIMARY KEY, first_name VARCHAR, last_name VARCHAR, email VARCHAR UNIQUE, password VARCHAR, role VARCHAR, department VARCHAR, status VARCHAR)");
        s.execute("CREATE TABLE activity  (id INT PRIMARY KEY, name VARCHAR, user_name VARCHAR, activity_date VARCHAR, status VARCHAR, type VARCHAR)");
        s.execute("CREATE TABLE policy    (id INT PRIMARY KEY, policy_code VARCHAR, name VARCHAR, category VARCHAR, effective_date VARCHAR, expiry_date VARCHAR, status VARCHAR)");

        s.execute("INSERT INTO app_user VALUES (1,'John','Doe','user@example.com','password123','User','Operations','Active')");
        s.execute("INSERT INTO app_user VALUES (2,'Jane','Admin','admin@example.com','admin123','Admin','IT','Active')");
        s.execute("INSERT INTO app_user VALUES (3,'Bob','Smith','bob@example.com','bob123','Manager','HR','Inactive')");
        s.execute("INSERT INTO app_user VALUES (4,'Alice','Lee','alice@example.com','alice123','User','Finance','Active')");

        s.execute("INSERT INTO activity VALUES (1,'Login','John Doe','2026-05-30 09:42','Success','Auth')");
        s.execute("INSERT INTO activity VALUES (2,'Policy Updated','John Doe','2026-05-29 14:10','Info','Policy')");
        s.execute("INSERT INTO activity VALUES (3,'Export Report','John Doe','2026-05-28 11:03','Success','Report')");
        s.execute("INSERT INTO activity VALUES (4,'Password Change','John Doe','2026-05-27 16:55','Success','Auth')");
        s.execute("INSERT INTO activity VALUES (5,'Failed Login','Unknown','2026-05-26 02:14','Failed','Auth')");
        s.execute("INSERT INTO activity VALUES (6,'Account Update','John Doe','2026-05-24 10:30','Success','Account')");
        s.execute("INSERT INTO activity VALUES (7,'Policy Review','Jane Admin','2026-05-22 09:00','Pending','Policy')");

        s.execute("INSERT INTO policy VALUES (1,'POL-001','Data Retention','Compliance','2026-01-01','2026-12-31','Active')");
        s.execute("INSERT INTO policy VALUES (2,'POL-002','Access Control','Security','2026-01-01','2026-12-31','Active')");
        s.execute("INSERT INTO policy VALUES (3,'POL-003','Remote Work','HR','2026-03-01','2027-02-28','Active')");
        s.execute("INSERT INTO policy VALUES (4,'POL-004','Incident Response','Security','2025-07-01','2026-06-30','Expiring Soon')");
        s.execute("INSERT INTO policy VALUES (5,'POL-005','Password Policy','Security','2024-01-01','2025-12-31','Expired')");

        s.execute("CREATE TABLE setting (setting_key VARCHAR PRIMARY KEY, label VARCHAR, description VARCHAR, setting_value VARCHAR)");
        s.execute("INSERT INTO setting VALUES ('maintenance_mode',   'Maintenance Mode',    'Disable access for all users',    'false')");
        s.execute("INSERT INTO setting VALUES ('email_notifications','Email Notifications', 'Send alerts via email',           'true')");
        s.execute("INSERT INTO setting VALUES ('two_factor_auth',    'Two-Factor Auth',     'Require 2FA for all users',       'true')");
        s.execute("INSERT INTO setting VALUES ('audit_logging',      'Audit Logging',       'Log all user actions',            'true')");
        s.execute("INSERT INTO setting VALUES ('auto_logout',        'Auto Logout',         'Session expires after 30 min',    'false')");
        s.execute("INSERT INTO setting VALUES ('dark_mode',          'Dark Mode (Beta)',     'Enable dark theme globally',      'false')");

        s.close();
        System.out.println("H2 database initialised.");
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json");
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        byte[] bytes = json.getBytes("UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    static String readBody(HttpExchange ex) throws IOException {
        Scanner sc = new Scanner(ex.getRequestBody(), "UTF-8");
        return sc.hasNext() ? sc.useDelimiter("\\A").next() : "";
    }

    static String jsonField(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return "";
        int c = json.indexOf(":", i);
        int s = json.indexOf("\"", c) + 1;
        int e = json.indexOf("\"", s);
        return (s > 0 && e > s) ? json.substring(s, e) : "";
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static long count(String sql) throws SQLException {
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery(sql);
        long v = rs.next() ? rs.getLong(1) : 0;
        rs.close(); s.close();
        return v;
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    static void handleLogin(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
        String body  = readBody(ex);
        String email = jsonField(body, "email");
        String pass  = jsonField(body, "password");
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM app_user WHERE email=? AND password=?");
            ps.setString(1, email); ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String json = "{\"success\":true,\"user\":{\"id\":" + rs.getInt("id") +
                    ",\"firstName\":\"" + esc(rs.getString("first_name")) + "\"" +
                    ",\"lastName\":\""  + esc(rs.getString("last_name"))  + "\"" +
                    ",\"email\":\""     + esc(rs.getString("email"))      + "\"" +
                    ",\"role\":\""      + esc(rs.getString("role"))       + "\"" +
                    ",\"department\":\"" + esc(rs.getString("department"))+ "\"}}";
                sendJson(ex, 200, json);
            } else {
                sendJson(ex, 401, "{\"success\":false,\"message\":\"Invalid email or password\"}");
            }
            rs.close(); ps.close();
        } catch (SQLException e) { sendJson(ex, 500, "{\"success\":false,\"message\":\"DB error\"}"); }
    }

    static void handleSummary(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
        try {
            long users  = count("SELECT COUNT(*) FROM app_user");
            long active = count("SELECT COUNT(*) FROM policy WHERE status='Active'");
            long failed = count("SELECT COUNT(*) FROM activity WHERE status='Failed'");
            sendJson(ex, 200, "{\"totalUsers\":" + users + ",\"activePolicies\":" + active + ",\"alerts\":" + failed + ",\"openTasks\":12}");
        } catch (SQLException e) { sendJson(ex, 500, "{}"); }
    }

    static void routeActivities(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();
        if ("OPTIONS".equalsIgnoreCase(method))                          { sendJson(ex, 204, ""); return; }
        if ("POST".equalsIgnoreCase(method))                             { handleAddActivity(ex);    return; }
        if ("PUT".equalsIgnoreCase(method) && path.matches(".*/\\d+"))   { handleUpdateActivity(ex); return; }
        handleGetActivities(ex);
    }

    static String activityJson(ResultSet rs) throws SQLException {
        return "{\"id\":"            + rs.getInt("id")
             + ",\"name\":\""        + esc(rs.getString("name"))          + "\""
             + ",\"userName\":\""    + esc(rs.getString("user_name"))     + "\""
             + ",\"activityDate\":\"" + esc(rs.getString("activity_date")) + "\""
             + ",\"status\":\""      + esc(rs.getString("status"))        + "\""
             + ",\"type\":\""        + esc(rs.getString("type"))          + "\"}";
    }

    static void handleGetActivities(HttpExchange ex) throws IOException {
        String raw = ex.getRequestURI().getQuery();
        String q   = (raw != null && raw.contains("q="))
            ? "%" + raw.replaceAll(".*q=([^&]*).*", "$1").toLowerCase() + "%" : "%";
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM activity WHERE LOWER(name) LIKE ? OR LOWER(user_name) LIKE ? OR LOWER(status) LIKE ? OR LOWER(type) LIKE ? ORDER BY id DESC");
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q); ps.setString(4, q);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) { if (!first) sb.append(","); sb.append(activityJson(rs)); first = false; }
            sb.append("]");
            rs.close(); ps.close();
            sendJson(ex, 200, sb.toString());
        } catch (SQLException e) { sendJson(ex, 500, "[]"); }
    }

    static void handleAddActivity(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        String name = jsonField(body, "name");
        String user = jsonField(body, "userName");
        String date = jsonField(body, "activityDate");
        String stat = jsonField(body, "status");
        String type = jsonField(body, "type");
        try {
            Statement s = conn.createStatement();
            ResultSet r = s.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM activity");
            int newId   = r.next() ? r.getInt(1) : 1;
            r.close(); s.close();

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO activity VALUES (?,?,?,?,?,?)");
            ps.setInt(1, newId); ps.setString(2, name); ps.setString(3, user);
            ps.setString(4, date); ps.setString(5, stat); ps.setString(6, type);
            ps.executeUpdate(); ps.close();

            PreparedStatement sel = conn.prepareStatement("SELECT * FROM activity WHERE id=?");
            sel.setInt(1, newId);
            ResultSet rs = sel.executeQuery();
            String json  = rs.next() ? activityJson(rs) : "{}";
            rs.close(); sel.close();
            sendJson(ex, 201, json);
        } catch (SQLException e) {
            sendJson(ex, 400, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    static void handleUpdateActivity(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        int id      = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        String body = readBody(ex);
        String name = jsonField(body, "name");
        String user = jsonField(body, "userName");
        String date = jsonField(body, "activityDate");
        String stat = jsonField(body, "status");
        String type = jsonField(body, "type");
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE activity SET name=?,user_name=?,activity_date=?,status=?,type=? WHERE id=?");
            ps.setString(1, name); ps.setString(2, user); ps.setString(3, date);
            ps.setString(4, stat); ps.setString(5, type); ps.setInt(6, id);
            int rows = ps.executeUpdate(); ps.close();
            if (rows == 0) { sendJson(ex, 404, "{\"error\":\"Activity not found\"}"); return; }

            PreparedStatement sel = conn.prepareStatement("SELECT * FROM activity WHERE id=?");
            sel.setInt(1, id);
            ResultSet rs = sel.executeQuery();
            String json  = rs.next() ? activityJson(rs) : "{}";
            rs.close(); sel.close();
            sendJson(ex, 200, json);
        } catch (SQLException e) {
            sendJson(ex, 400, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    static void routePolicies(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();
        if ("OPTIONS".equalsIgnoreCase(method))                            { sendJson(ex, 204, ""); return; }
        if ("POST".equalsIgnoreCase(method))                               { handleAddPolicy(ex);    return; }
        if ("PUT".equalsIgnoreCase(method) && path.matches(".*/\\d+"))     { handleUpdatePolicy(ex); return; }
        handleGetPolicies(ex);
    }

    static String policyJson(ResultSet rs) throws SQLException {
        return "{\"id\":"             + rs.getInt("id")
             + ",\"policyCode\":\""   + esc(rs.getString("policy_code"))    + "\""
             + ",\"name\":\""         + esc(rs.getString("name"))           + "\""
             + ",\"category\":\""     + esc(rs.getString("category"))       + "\""
             + ",\"effectiveDate\":\"" + esc(rs.getString("effective_date")) + "\""
             + ",\"expiryDate\":\""   + esc(rs.getString("expiry_date"))    + "\""
             + ",\"status\":\""       + esc(rs.getString("status"))         + "\"}";
    }

    static void handleGetPolicies(HttpExchange ex) throws IOException {
        String raw = ex.getRequestURI().getQuery();
        String q   = (raw != null && raw.contains("q="))
            ? "%" + raw.replaceAll(".*q=([^&]*).*", "$1").toLowerCase() + "%" : "%";
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM policy WHERE LOWER(name) LIKE ? OR LOWER(category) LIKE ? OR LOWER(status) LIKE ? OR LOWER(policy_code) LIKE ? ORDER BY id");
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q); ps.setString(4, q);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) { if (!first) sb.append(","); sb.append(policyJson(rs)); first = false; }
            sb.append("]");
            rs.close(); ps.close();
            sendJson(ex, 200, sb.toString());
        } catch (SQLException e) { sendJson(ex, 500, "[]"); }
    }

    static void handleAddPolicy(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        String code = jsonField(body, "policyCode");
        String name = jsonField(body, "name");
        String cat  = jsonField(body, "category");
        String eff  = jsonField(body, "effectiveDate");
        String exp  = jsonField(body, "expiryDate");
        String stat = jsonField(body, "status");
        try {
            Statement s  = conn.createStatement();
            ResultSet r  = s.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM policy");
            int newId    = r.next() ? r.getInt(1) : 1;
            r.close(); s.close();

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO policy VALUES (?,?,?,?,?,?,?)");
            ps.setInt(1, newId); ps.setString(2, code); ps.setString(3, name);
            ps.setString(4, cat); ps.setString(5, eff); ps.setString(6, exp); ps.setString(7, stat);
            ps.executeUpdate(); ps.close();

            PreparedStatement sel = conn.prepareStatement("SELECT * FROM policy WHERE id=?");
            sel.setInt(1, newId);
            ResultSet rs = sel.executeQuery();
            String json  = rs.next() ? policyJson(rs) : "{}";
            rs.close(); sel.close();
            sendJson(ex, 201, json);
        } catch (SQLException e) {
            sendJson(ex, 400, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    static void handleUpdatePolicy(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        int id      = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        String body = readBody(ex);
        String code = jsonField(body, "policyCode");
        String name = jsonField(body, "name");
        String cat  = jsonField(body, "category");
        String eff  = jsonField(body, "effectiveDate");
        String exp  = jsonField(body, "expiryDate");
        String stat = jsonField(body, "status");
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE policy SET policy_code=?,name=?,category=?,effective_date=?,expiry_date=?,status=? WHERE id=?");
            ps.setString(1, code); ps.setString(2, name); ps.setString(3, cat);
            ps.setString(4, eff);  ps.setString(5, exp);  ps.setString(6, stat); ps.setInt(7, id);
            int rows = ps.executeUpdate(); ps.close();
            if (rows == 0) { sendJson(ex, 404, "{\"error\":\"Policy not found\"}"); return; }

            PreparedStatement sel = conn.prepareStatement("SELECT * FROM policy WHERE id=?");
            sel.setInt(1, id);
            ResultSet rs = sel.executeQuery();
            String json  = rs.next() ? policyJson(rs) : "{}";
            rs.close(); sel.close();
            sendJson(ex, 200, json);
        } catch (SQLException e) {
            sendJson(ex, 400, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    static void routeUsers(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();
        if ("OPTIONS".equalsIgnoreCase(method)) { sendJson(ex, 204, ""); return; }
        if ("POST".equalsIgnoreCase(method))                          { handleAddUser(ex);    return; }
        if ("PUT".equalsIgnoreCase(method) && path.matches(".*/\\d+")) { handleUpdateUser(ex); return; }
        handleGetUsers(ex);
    }

    static String userJson(ResultSet rs) throws SQLException {
        return "{\"id\":"          + rs.getInt("id")
             + ",\"firstName\":\"" + esc(rs.getString("first_name"))  + "\""
             + ",\"lastName\":\""  + esc(rs.getString("last_name"))   + "\""
             + ",\"email\":\""     + esc(rs.getString("email"))       + "\""
             + ",\"role\":\""      + esc(rs.getString("role"))        + "\""
             + ",\"department\":\"" + esc(rs.getString("department")) + "\""
             + ",\"status\":\""    + esc(rs.getString("status"))      + "\"}";
    }

    static void handleGetUsers(HttpExchange ex) throws IOException {
        String raw = ex.getRequestURI().getQuery();
        String q   = (raw != null && raw.contains("q="))
            ? "%" + raw.replaceAll(".*q=([^&]*).*", "$1").toLowerCase() + "%" : "%";
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM app_user WHERE LOWER(first_name||' '||last_name) LIKE ? OR LOWER(email) LIKE ? OR LOWER(role) LIKE ? OR LOWER(status) LIKE ? ORDER BY id");
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q); ps.setString(4, q);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) { if (!first) sb.append(","); sb.append(userJson(rs)); first = false; }
            sb.append("]");
            rs.close(); ps.close();
            sendJson(ex, 200, sb.toString());
        } catch (SQLException e) { sendJson(ex, 500, "[]"); }
    }

    static void handleAddUser(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        String fn   = jsonField(body, "firstName");
        String ln   = jsonField(body, "lastName");
        String em   = jsonField(body, "email");
        String pw   = jsonField(body, "password");
        String role = jsonField(body, "role");
        String dept = jsonField(body, "department");
        String stat = jsonField(body, "status");
        try {
            Statement s = conn.createStatement();
            ResultSet r = s.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM app_user");
            int newId = r.next() ? r.getInt(1) : 1;
            r.close(); s.close();

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO app_user VALUES (?,?,?,?,?,?,?,?)");
            ps.setInt(1, newId); ps.setString(2, fn); ps.setString(3, ln);
            ps.setString(4, em); ps.setString(5, pw); ps.setString(6, role);
            ps.setString(7, dept); ps.setString(8, stat);
            ps.executeUpdate(); ps.close();

            PreparedStatement sel = conn.prepareStatement("SELECT * FROM app_user WHERE id=?");
            sel.setInt(1, newId);
            ResultSet rs = sel.executeQuery();
            String json = rs.next() ? userJson(rs) : "{}";
            rs.close(); sel.close();
            sendJson(ex, 201, json);
        } catch (SQLException e) {
            sendJson(ex, 400, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    static void handleUpdateUser(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        String body = readBody(ex);
        String fn   = jsonField(body, "firstName");
        String ln   = jsonField(body, "lastName");
        String em   = jsonField(body, "email");
        String pw   = jsonField(body, "password");
        String role = jsonField(body, "role");
        String dept = jsonField(body, "department");
        String stat = jsonField(body, "status");
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE app_user SET first_name=?,last_name=?,email=?,password=?,role=?,department=?,status=? WHERE id=?");
            ps.setString(1, fn); ps.setString(2, ln); ps.setString(3, em);
            ps.setString(4, pw); ps.setString(5, role); ps.setString(6, dept);
            ps.setString(7, stat); ps.setInt(8, id);
            int rows = ps.executeUpdate(); ps.close();

            if (rows == 0) { sendJson(ex, 404, "{\"error\":\"User not found\"}"); return; }

            PreparedStatement sel = conn.prepareStatement("SELECT * FROM app_user WHERE id=?");
            sel.setInt(1, id);
            ResultSet rs = sel.executeQuery();
            String json = rs.next() ? userJson(rs) : "{}";
            rs.close(); sel.close();
            sendJson(ex, 200, json);
        } catch (SQLException e) {
            sendJson(ex, 400, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    static void handleSearch(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
        String raw = ex.getRequestURI().getQuery();
        String q = "";
        if (raw != null && raw.contains("q=")) {
            try { q = URLDecoder.decode(raw.replaceAll(".*q=([^&]*).*", "$1"), "UTF-8").toLowerCase(); }
            catch (Exception ignored) {}
        }
        List<String> results = new ArrayList<String>();
        try {
            String like = "%" + q + "%";
            PreparedStatement ps;
            ResultSet rs;

            ps = conn.prepareStatement("SELECT * FROM app_user WHERE LOWER(first_name||' '||last_name) LIKE ? OR LOWER(role) LIKE ? OR LOWER(status) LIKE ?");
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            rs = ps.executeQuery();
            while (rs.next())
                results.add("{\"id\":\"USR-" + rs.getInt("id") + "\",\"name\":\"" + esc(rs.getString("first_name") + " " + rs.getString("last_name")) + "\",\"type\":\"" + esc(rs.getString("role")) + "\",\"date\":\"-\",\"status\":\"" + esc(rs.getString("status")) + "\"}");
            rs.close(); ps.close();

            ps = conn.prepareStatement("SELECT * FROM policy WHERE LOWER(name) LIKE ? OR LOWER(category) LIKE ? OR LOWER(status) LIKE ?");
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            rs = ps.executeQuery();
            while (rs.next())
                results.add("{\"id\":\"" + esc(rs.getString("policy_code")) + "\",\"name\":\"" + esc(rs.getString("name")) + "\",\"type\":\"Policy\",\"date\":\"" + esc(rs.getString("effective_date")) + "\",\"status\":\"" + esc(rs.getString("status")) + "\"}");
            rs.close(); ps.close();

            ps = conn.prepareStatement("SELECT * FROM activity WHERE LOWER(name) LIKE ? OR LOWER(user_name) LIKE ? OR LOWER(status) LIKE ?");
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            rs = ps.executeQuery();
            while (rs.next())
                results.add("{\"id\":\"ACT-" + rs.getInt("id") + "\",\"name\":\"" + esc(rs.getString("name")) + "\",\"type\":\"Activity\",\"date\":\"" + esc(rs.getString("activity_date")) + "\",\"status\":\"" + esc(rs.getString("status")) + "\"}");
            rs.close(); ps.close();
        } catch (SQLException e) { sendJson(ex, 500, "[]"); return; }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) { if (i > 0) sb.append(","); sb.append(results.get(i)); }
        sb.append("]");
        sendJson(ex, 200, sb.toString());
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    static void routeSettings(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) { sendJson(ex, 204, ""); return; }
        if ("PUT".equalsIgnoreCase(method))     { handleUpdateSetting(ex); return; }
        handleGetSettings(ex);
    }

    static void handleGetSettings(HttpExchange ex) throws IOException {
        try {
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM setting ORDER BY setting_key");
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("{\"key\":\"").append(esc(rs.getString("setting_key"))).append("\"")
                  .append(",\"label\":\"").append(esc(rs.getString("label"))).append("\"")
                  .append(",\"description\":\"").append(esc(rs.getString("description"))).append("\"")
                  .append(",\"value\":\"").append(esc(rs.getString("setting_value"))).append("\"}");
                first = false;
            }
            sb.append("]");
            rs.close(); s.close();
            sendJson(ex, 200, sb.toString());
        } catch (SQLException e) { sendJson(ex, 500, "[]"); }
    }

    static void handleUpdateSetting(HttpExchange ex) throws IOException {
        String path  = ex.getRequestURI().getPath();
        String key   = path.substring(path.lastIndexOf('/') + 1);
        String body  = readBody(ex);
        String value = jsonField(body, "value");
        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE setting SET setting_value=? WHERE setting_key=?");
            ps.setString(1, value); ps.setString(2, key);
            int rows = ps.executeUpdate(); ps.close();
            if (rows == 0) { sendJson(ex, 404, "{\"error\":\"Setting not found\"}"); return; }
            sendJson(ex, 200, "{\"key\":\"" + esc(key) + "\",\"value\":\"" + esc(value) + "\"}");
        } catch (SQLException e) { sendJson(ex, 500, "{\"error\":\"DB error\"}"); }
    }

    abstract static class Handler implements HttpHandler {}
}
