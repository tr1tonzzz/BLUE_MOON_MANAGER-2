package vn.bluemoon.util;

/**
 * Utility class for logging errors (for web app compatibility)
 * Note: In web app, errors are handled via redirects with flash attributes
 */
public class ErrorDialog {
    
    /**
     * Log error (for web app, errors are shown via redirects)
     * @param title Dialog title
     * @param message Error message
     */
    public static void showError(String title, String message) {
        System.err.println("ERROR [" + title + "]: " + message);
    }

    /**
     * Log database error
     * @param message Error message
     */
    public static void showDbError(String message) {
        showError("Lỗi cơ sở dữ liệu", 
            "Đã xảy ra lỗi khi kết nối hoặc thao tác với cơ sở dữ liệu: " + message);
    }

    /**
     * Log information
     * @param title Dialog title
     * @param message Information message
     */
    public static void showInfo(String title, String message) {
        System.out.println("INFO [" + title + "]: " + message);
    }

    /**
     * Log confirmation request (for web app, always returns true)
     * @param title Dialog title
     * @param message Confirmation message
     * @return true (web app handles confirmation via forms)
     */
    public static boolean showConfirmation(String title, String message) {
        System.out.println("CONFIRM [" + title + "]: " + message);
        return true; // Web app handles confirmation via forms
    }
}












