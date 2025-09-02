import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class LoadHolidaysStandalone {
    
    private static class Holiday {
        String name;
        LocalDate date;
        String description;
        boolean workingAllowed;
        
        Holiday(String name, LocalDate date, String description, boolean workingAllowed) {
            this.name = name;
            this.date = date;
            this.description = description;
            this.workingAllowed = workingAllowed;
        }
    }
    
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("Connected to database: " + dbPath);
            
            // Create table if it doesn't exist
            createTableIfNotExists(conn);
            
            // Get the year
            int year = 2025;
            if (args.length > 0) {
                year = Integer.parseInt(args[0]);
            }
            System.out.println("Loading federal holidays for year: " + year);
            
            // Generate federal holidays
            List<Holiday> holidays = generateFederalHolidays(year);
            
            // Clear existing FEDERAL holidays for this year (keep user-added ones)
            System.out.println("Clearing existing FEDERAL holidays for year " + year + "...");
            clearFederalHolidaysForYear(conn, year);
            
            // Insert holidays
            System.out.println("Inserting " + holidays.size() + " federal holidays...");
            for (Holiday holiday : holidays) {
                insertHoliday(conn, holiday);
                System.out.println("  Added: " + holiday.name + " - " + holiday.date);
            }
            
            // Also add holidays for next year
            int nextYear = year + 1;
            System.out.println("\nLoading federal holidays for year: " + nextYear);
            clearFederalHolidaysForYear(conn, nextYear);
            List<Holiday> nextYearHolidays = generateFederalHolidays(nextYear);
            for (Holiday holiday : nextYearHolidays) {
                insertHoliday(conn, holiday);
                System.out.println("  Added: " + holiday.name + " - " + holiday.date);
            }
            
            System.out.println("\n===========================================");
            System.out.println("Federal holidays loaded successfully!");
            System.out.println("Total holidays added: " + (holidays.size() + nextYearHolidays.size()));
            System.out.println("\nUsers can now edit these in the Holiday Calendar");
            System.out.println("to match company policies.");
            System.out.println("===========================================");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS company_holidays (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "date TEXT NOT NULL, " +
            "type TEXT, " +
            "description TEXT, " +
            "working_holiday_allowed BOOLEAN DEFAULT 0, " +
            "active BOOLEAN DEFAULT 1, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'company_holidays' is ready");
        }
    }
    
    private static void clearFederalHolidaysForYear(Connection conn, int year) throws SQLException {
        String sql = "DELETE FROM company_holidays WHERE date LIKE ? AND type = 'FEDERAL'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, year + "-%");
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                System.out.println("Deleted " + deleted + " existing FEDERAL holidays for year " + year);
            }
        }
    }
    
    private static void insertHoliday(Connection conn, Holiday holiday) throws SQLException {
        String sql = "INSERT INTO company_holidays (name, date, type, description, working_holiday_allowed, active) " +
                    "VALUES (?, ?, 'FEDERAL', ?, ?, 1)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, holiday.name);
            stmt.setString(2, holiday.date.toString());
            stmt.setString(3, holiday.description);
            stmt.setBoolean(4, holiday.workingAllowed);
            stmt.executeUpdate();
        }
    }
    
    private static List<Holiday> generateFederalHolidays(int year) {
        List<Holiday> holidays = new ArrayList<>();
        
        // New Year's Day - January 1
        holidays.add(new Holiday("New Year's Day", 
            LocalDate.of(year, Month.JANUARY, 1),
            "Federal holiday - Company closed", false));
        
        // Martin Luther King Jr. Day - Third Monday of January
        holidays.add(new Holiday("Martin Luther King Jr. Day",
            LocalDate.of(year, Month.JANUARY, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY)),
            "Federal holiday - Company closed", false));
        
        // Presidents' Day - Third Monday of February
        holidays.add(new Holiday("Presidents' Day",
            LocalDate.of(year, Month.FEBRUARY, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY)),
            "Federal holiday - Company closed", false));
        
        // Memorial Day - Last Monday of May
        holidays.add(new Holiday("Memorial Day",
            LocalDate.of(year, Month.MAY, 1)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY)),
            "Federal holiday - Company closed", false));
        
        // Juneteenth - June 19
        holidays.add(new Holiday("Juneteenth",
            LocalDate.of(year, Month.JUNE, 19),
            "Federal holiday - Company closed", false));
        
        // Independence Day - July 4
        holidays.add(new Holiday("Independence Day",
            LocalDate.of(year, Month.JULY, 4),
            "Federal holiday - Company closed", false));
        
        // Labor Day - First Monday of September
        holidays.add(new Holiday("Labor Day",
            LocalDate.of(year, Month.SEPTEMBER, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)),
            "Federal holiday - Company closed", false));
        
        // Columbus Day - Second Monday of October
        holidays.add(new Holiday("Columbus Day",
            LocalDate.of(year, Month.OCTOBER, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY)),
            "Federal holiday - Some offices open", true));
        
        // Veterans Day - November 11
        holidays.add(new Holiday("Veterans Day",
            LocalDate.of(year, Month.NOVEMBER, 11),
            "Federal holiday - Company closed", false));
        
        // Thanksgiving Day - Fourth Thursday of November
        holidays.add(new Holiday("Thanksgiving Day",
            LocalDate.of(year, Month.NOVEMBER, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY)),
            "Federal holiday - Company closed", false));
        
        // Day after Thanksgiving (common business holiday)
        LocalDate thanksgiving = LocalDate.of(year, Month.NOVEMBER, 1)
            .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY));
        holidays.add(new Holiday("Day After Thanksgiving",
            thanksgiving.plusDays(1),
            "Company holiday - Office closed", false));
        
        // Christmas Eve (common business holiday) - December 24
        holidays.add(new Holiday("Christmas Eve",
            LocalDate.of(year, Month.DECEMBER, 24),
            "Company holiday - Early closing", true));
        
        // Christmas Day - December 25
        holidays.add(new Holiday("Christmas Day",
            LocalDate.of(year, Month.DECEMBER, 25),
            "Federal holiday - Company closed", false));
        
        // New Year's Eve (common business holiday) - December 31
        holidays.add(new Holiday("New Year's Eve",
            LocalDate.of(year, Month.DECEMBER, 31),
            "Company holiday - Early closing", true));
        
        return holidays;
    }
}