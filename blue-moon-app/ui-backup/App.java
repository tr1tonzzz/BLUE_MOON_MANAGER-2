package vn.bluemoon;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.ui.login.LoginController;
import vn.bluemoon.util.DatabaseInitializer;
import vn.bluemoon.util.ErrorDialog;

/**
 * Main application class
 */
public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize database before showing UI
        try {
            DatabaseInitializer.initialize();
        } catch (DbException e) {
            // Show error dialog and exit
            Platform.runLater(() -> {
                ErrorDialog.showError(
                    "Lỗi khởi tạo Database",
                    e.getMessage()
                );
                Platform.exit();
            });
            return;
        }
        
        // Load and show login view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/login/LoginView.fxml"));
        Parent root = loader.load();
        
        LoginController controller = loader.getController();
        controller.setStage(primaryStage);
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Blue Moon - Hệ thống quản lý chung cư");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}