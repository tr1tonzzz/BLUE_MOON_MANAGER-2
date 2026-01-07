package vn.bluemoon.ui.main;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.security.Authorization;
import vn.bluemoon.security.SessionManager;
import vn.bluemoon.util.ErrorDialog;

import java.io.IOException;
import java.util.Set;

/**
 * Controller for main view
 */
public class MainController {
    private Stage mainStage;
    
    @FXML
    private MenuBar menuBar;
    
    @FXML
    private Menu systemMenu;
    
    @FXML
    private MenuItem userManagementMenuItem;
    
    @FXML
    private MenuItem functionManagementMenuItem;
    
    @FXML
    private MenuItem menuManagementMenuItem;
    
    @FXML
    private Menu residentMenu;
    
    @FXML
    private MenuItem residentManagementMenuItem;
    
    @FXML
    private Menu feeMenu;
    
    @FXML
    private MenuItem feeCollectionMenuItem;
    
    @FXML
    private Menu personalMenu;
    
    @FXML
    private MenuItem personalInfoMenuItem;
    
    @FXML
    private MenuItem paymentMenuItem;
    
    @FXML
    private GridPane functionGridPane;
    
    @FXML
    public void initialize() {
        // Kiểm tra quyền và ẩn/hiện menu items
        checkPermissions();
        // Kiểm tra quyền và ẩn/hiện function cards
        checkFunctionCards();
    }
    
    /**
     * Kiểm tra quyền và ẩn/hiện menu items
     */
    private void checkPermissions() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                // Nếu không có user, ẩn tất cả menu trừ đăng xuất
                hideAllMenus();
                return;
            }
            
            // Kiểm tra quyền cho từng chức năng
            boolean hasUserManagement = Authorization.hasAccess(currentUser, "UserManagementForm");
            boolean hasFunctionManagement = Authorization.hasAccess(currentUser, "FunctionManagementForm");
            boolean hasMenuManagement = Authorization.hasAccess(currentUser, "MenuForm");
            boolean hasResidentManagement = Authorization.hasAccess(currentUser, "ResidentManagementView");
            boolean hasFeeCollection = Authorization.hasAccess(currentUser, "FeeCollectionView");
            
            // Ẩn/hiện menu items
            userManagementMenuItem.setVisible(hasUserManagement);
            functionManagementMenuItem.setVisible(hasFunctionManagement);
            menuManagementMenuItem.setVisible(hasMenuManagement);
            residentManagementMenuItem.setVisible(hasResidentManagement);
            feeCollectionMenuItem.setVisible(hasFeeCollection);
            
            // Ẩn menu nếu không có item nào hiển thị
            // Menu "Hệ thống" luôn hiển thị vì có "Đăng xuất"
            if (!hasResidentManagement) {
                residentMenu.setVisible(false);
            }
            if (!hasFeeCollection) {
                feeMenu.setVisible(false);
            }
            
            // Menu "Cá nhân" hiển thị cho tất cả user đã đăng nhập (không phải admin)
            // Admin cũng có thể dùng nhưng thường dùng quản lý nhân khẩu
            boolean isAdmin = Authorization.hasRole(currentUser, "Quản trị viên");
            personalMenu.setVisible(true);
            personalInfoMenuItem.setVisible(!isAdmin);
            paymentMenuItem.setVisible(!isAdmin);
            
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi khi kiểm tra quyền: " + e.getMessage());
        }
    }
    
    private void hideAllMenus() {
        systemMenu.setVisible(false);
        residentMenu.setVisible(false);
        feeMenu.setVisible(false);
    }
    
    /**
     * Kiểm tra quyền và ẩn/hiện function cards
     */
    private void checkFunctionCards() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                // Ẩn tất cả cards nếu chưa đăng nhập
                if (functionGridPane != null) {
                    functionGridPane.getChildren().forEach(node -> {
                        if (node instanceof VBox) {
                            node.setVisible(false);
                        }
                    });
                }
                return;
            }
            
            // Kiểm tra quyền cho từng chức năng
            boolean hasUserManagement = Authorization.hasAccess(currentUser, "UserManagementForm");
            boolean hasFunctionManagement = Authorization.hasAccess(currentUser, "FunctionManagementForm");
            boolean hasMenuManagement = Authorization.hasAccess(currentUser, "MenuForm");
            boolean hasResidentManagement = Authorization.hasAccess(currentUser, "ResidentManagementView");
            boolean hasFeeCollection = Authorization.hasAccess(currentUser, "FeeCollectionView");
            
            // Menu "Cá nhân" hiển thị cho tất cả user đã đăng nhập (trừ admin)
            boolean hasPersonalInfo = !Authorization.hasRole(currentUser, "Quản trị viên");
            // Chức năng "Đóng tiền" chỉ hiển thị cho user (không phải admin)
            boolean hasPayment = !Authorization.hasRole(currentUser, "Quản trị viên");
            // Chức năng "Đăng xuất" hiển thị cho tất cả user đã đăng nhập
            boolean hasLogout = true; // Tất cả user đều có thể đăng xuất
            
            // Ẩn/hiện các cards dựa trên quyền
            if (functionGridPane != null) {
                // Tìm các VBox trong GridPane theo vị trí
                for (javafx.scene.Node node : functionGridPane.getChildren()) {
                    if (node instanceof VBox) {
                        Integer colIndex = GridPane.getColumnIndex(node);
                        Integer rowIndex = GridPane.getRowIndex(node);
                        
                        if (colIndex == null) colIndex = 0;
                        if (rowIndex == null) rowIndex = 0;
                        
                        // Card 0,0: Quản lý người dùng
                        if (colIndex == 0 && rowIndex == 0) {
                            node.setVisible(hasUserManagement);
                        }
                        // Card 1,0: Quản lý nhân khẩu
                        else if (colIndex == 1 && rowIndex == 0) {
                            node.setVisible(hasResidentManagement);
                        }
                        // Card 2,0: Quản lý thu phí
                        else if (colIndex == 2 && rowIndex == 0) {
                            node.setVisible(hasFeeCollection);
                        }
                        // Card 0,1: Thông tin cá nhân hoặc Quản lý chức năng
                        else if (colIndex == 0 && rowIndex == 1) {
                            // Kiểm tra xem có phải là card "Thông tin cá nhân" không
                            if (node instanceof VBox) {
                                VBox vbox = (VBox) node;
                                boolean isPersonalInfo = false;
                                for (javafx.scene.Node child : vbox.getChildren()) {
                                    if (child instanceof Label) {
                                        Label label = (Label) child;
                                        if ("Thông tin cá nhân".equals(label.getText())) {
                                            isPersonalInfo = true;
                                            break;
                                        }
                                    }
                                }
                                if (isPersonalInfo) {
                                    node.setVisible(hasPersonalInfo);
                                } else {
                                    // Card "Quản lý chức năng" - ẩn đi vì đã có card khác
                                    node.setVisible(hasFunctionManagement);
                                }
                            }
                        }
                        // Card 1,1: Đăng xuất
                        else if (colIndex == 1 && rowIndex == 1) {
                            node.setVisible(hasLogout);
                        }
                        // Card 2,1: Đóng tiền
                        else if (colIndex == 2 && rowIndex == 1) {
                            node.setVisible(hasPayment);
                        }
                    }
                }
            }
            
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi khi kiểm tra quyền: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleUserManagement() {
        try {
            // Kiểm tra quyền
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (!Authorization.hasAccess(currentUser, "UserManagementForm")) {
                ErrorDialog.showError("Lỗi", "Bạn không có quyền truy cập chức năng này");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/user/UserManagementView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 700);
            Stage stage = new Stage();
            stage.setTitle("Quản lý người dùng");
            stage.setScene(scene);
            stage.show();
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi khi kiểm tra quyền: " + e.getMessage());
        } catch (IOException e) {
            ErrorDialog.showError("Lỗi", "Không thể mở màn hình quản lý người dùng: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleFunctionManagement() {
        try {
            // Kiểm tra quyền - chỉ Quản trị viên
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (!Authorization.hasAccess(currentUser, "FunctionManagementForm")) {
                ErrorDialog.showError("Lỗi", "Bạn không có quyền truy cập chức năng này. Chỉ Quản trị viên mới có quyền.");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/function/FunctionManagementView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 600);
            Stage stage = new Stage();
            stage.setTitle("Quản lý chức năng");
            stage.setScene(scene);
            stage.show();
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi khi kiểm tra quyền: " + e.getMessage());
        } catch (IOException e) {
            ErrorDialog.showError("Lỗi", "Không thể mở màn hình quản lý chức năng: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleMenuManagement() {
        try {
            // Kiểm tra quyền - chỉ Quản trị viên
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (!Authorization.hasAccess(currentUser, "MenuForm")) {
                ErrorDialog.showError("Lỗi", "Bạn không có quyền truy cập chức năng này. Chỉ Quản trị viên mới có quyền.");
                return;
            }
            
            ErrorDialog.showInfo("Thông tin", "Chức năng tạo menu đang được phát triển");
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi khi kiểm tra quyền: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleResidentManagement() {
        try {
            // Kiểm tra quyền
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (!Authorization.hasAccess(currentUser, "ResidentManagementView")) {
                ErrorDialog.showError("Lỗi", "Bạn không có quyền truy cập chức năng này");
                return;
            }
            
            java.net.URL resource = getClass().getResource("/ui/resident/ResidentManagementView.fxml");
            if (resource == null) {
                ErrorDialog.showError("Lỗi", "Không tìm thấy file FXML: /ui/resident/ResidentManagementView.fxml\nVui lòng rebuild project.");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1400, 800);
            Stage stage = new Stage();
            stage.setTitle("Quản lý nhân khẩu");
            stage.setScene(scene);
            stage.show();
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi khi kiểm tra quyền: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError("Lỗi", "Không thể mở màn hình quản lý nhân khẩu: " + e.getMessage() + "\n" + e.getClass().getName());
        }
    }
    
    @FXML
    private void handleFeeCollection() {
        try {
            // Kiểm tra quyền
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (!Authorization.hasAccess(currentUser, "FeeCollectionView")) {
                ErrorDialog.showError("Lỗi", "Bạn không có quyền truy cập chức năng này");
                return;
            }
            
            java.net.URL resource = getClass().getResource("/ui/fee/FeeCollectionView.fxml");
            if (resource == null) {
                ErrorDialog.showError("Lỗi", "Không tìm thấy file FXML: /ui/fee/FeeCollectionView.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1400, 800);
            Stage stage = new Stage();
            stage.setTitle("Quản lý thu phí");
            stage.setScene(scene);
            stage.show();
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi khi kiểm tra quyền: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError("Lỗi", "Không thể mở màn hình quản lý thu phí: " + e.getMessage() + "\n" + e.getClass().getName());
        }
    }
    
    @FXML
    private void handlePersonalInfo() {
        try {
            // Kiểm tra user đã đăng nhập
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                ErrorDialog.showError("Lỗi", "Bạn chưa đăng nhập");
                return;
            }
            
            // Kiểm tra nếu là admin thì không cho dùng (admin dùng quản lý nhân khẩu)
            if (Authorization.hasRole(currentUser, "Quản trị viên")) {
                ErrorDialog.showInfo("Thông tin", "Quản trị viên vui lòng sử dụng chức năng 'Quản lý nhân khẩu' để quản lý thông tin");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/personal/PersonalInfoView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 700, 800);
            Stage stage = new Stage();
            stage.setTitle("Thông tin cá nhân");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            ErrorDialog.showError("Lỗi", "Không thể mở màn hình thông tin cá nhân: " + e.getMessage());
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi: " + e.getMessage());
        }
    }
    
    @FXML
    private void handlePayment() {
        try {
            // Kiểm tra user đã đăng nhập
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                ErrorDialog.showError("Lỗi", "Bạn chưa đăng nhập");
                return;
            }
            
            // Kiểm tra nếu là admin thì không cho dùng
            if (Authorization.hasRole(currentUser, "Quản trị viên")) {
                ErrorDialog.showInfo("Thông tin", "Quản trị viên vui lòng sử dụng chức năng 'Quản lý thu phí' để quản lý thanh toán");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/payment/PaymentView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 700);
            Stage stage = new Stage();
            stage.setTitle("Đóng tiền");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            ErrorDialog.showError("Lỗi", "Không thể mở màn hình đóng tiền: " + e.getMessage());
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLogout() {
        try {
            // Xóa session
            SessionManager.getInstance().clearSession();
            
            // Đóng tất cả các cửa sổ con (các Stage được mở từ MainView)
            Stage primaryStage = getMainStage();
            javafx.collections.ObservableList<javafx.stage.Window> windows = javafx.stage.Window.getWindows();
            for (javafx.stage.Window window : windows) {
                if (window instanceof Stage) {
                    Stage stage = (Stage) window;
                    // Đóng tất cả các Stage trừ primary stage
                    if (stage != primaryStage && stage.isShowing()) {
                        stage.close();
                    }
                }
            }
            
            // Chuyển về màn hình đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/login/LoginView.fxml"));
            Parent root = loader.load();
            
            // Set stage cho LoginController
            vn.bluemoon.ui.login.LoginController loginController = loader.getController();
            if (loginController != null) {
                loginController.setStage(primaryStage);
            }
            
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setTitle("Blue Moon - Hệ thống quản lý chung cư");
            primaryStage.setMaximized(false); // Reset về kích thước bình thường
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen(); // Căn giữa màn hình
            
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError("Lỗi", "Không thể đăng xuất: " + e.getMessage());
        }
    }
    
    /**
     * Lấy Stage chính (primary stage)
     */
    private Stage getMainStage() {
        if (mainStage != null) {
            return mainStage;
        }
        
        // Tìm primary stage từ các window đang mở
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof Stage) {
                Stage stage = (Stage) window;
                // Stage chính thường là stage đầu tiên hoặc stage có scene là MainView
                if (stage.getScene() != null && 
                    stage.getScene().getRoot() != null &&
                    stage.getScene().getRoot().getUserData() != null) {
                    mainStage = stage;
                    return stage;
                }
            }
        }
        
        // Nếu không tìm thấy, lấy stage đầu tiên
        Set<Stage> stages = javafx.stage.Window.getWindows().stream()
            .filter(window -> window instanceof Stage)
            .map(window -> (Stage) window)
            .filter(stage -> stage.isShowing())
            .collect(java.util.stream.Collectors.toSet());
        
        if (!stages.isEmpty()) {
            mainStage = stages.iterator().next();
            return mainStage;
        }
        
        return null;
    }
    
    /**
     * Set main stage (được gọi từ LoginController khi chuyển sang MainView)
     */
    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }
}

