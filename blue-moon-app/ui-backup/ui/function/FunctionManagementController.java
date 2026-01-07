package vn.bluemoon.ui.function;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Function;
import vn.bluemoon.service.FunctionService;
import vn.bluemoon.util.ErrorDialog;

import java.io.IOException;
import java.util.List;

/**
 * Controller for function management view
 */
public class FunctionManagementController {
    @FXML
    private TableView<Function> functionTable;
    
    @FXML
    private TableColumn<Function, Integer> sttColumn;
    
    @FXML
    private TableColumn<Function, String> nameColumn;
    
    @FXML
    private TableColumn<Function, String> functionGroupColumn;
    
    @FXML
    private TableColumn<Function, String> boundaryClassColumn;
    
    @FXML
    private Button addButton;
    
    @FXML
    private Button editButton;
    
    @FXML
    private Button deleteButton;
    
    private FunctionService functionService = new FunctionService();
    private ObservableList<Function> functionList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        // Setup table columns
        sttColumn.setCellValueFactory(param -> {
            int index = functionList.indexOf(param.getValue());
            return javafx.beans.binding.Bindings.createObjectBinding(() -> index + 1);
        });
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        functionGroupColumn.setCellValueFactory(new PropertyValueFactory<>("functionGroupName"));
        boundaryClassColumn.setCellValueFactory(new PropertyValueFactory<>("boundaryClass"));
        
        functionTable.setItems(functionList);
        
        loadFunctions();
    }
    
    private void loadFunctions() {
        try {
            List<Function> functions = functionService.getAllFunctions();
            functionList.clear();
            functionList.addAll(functions);
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        } catch (Exception e) {
            ErrorDialog.showError("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAdd() {
        openFunctionDialog(null);
    }
    
    @FXML
    private void handleEdit() {
        Function selectedFunction = functionTable.getSelectionModel().getSelectedItem();
        if (selectedFunction == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn chức năng cần sửa");
            return;
        }
        
        openFunctionDialog(selectedFunction);
    }
    
    @FXML
    private void handleDelete() {
        Function selectedFunction = functionTable.getSelectionModel().getSelectedItem();
        if (selectedFunction == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn chức năng cần xóa");
            return;
        }
        
        if (ErrorDialog.showConfirmation("Xác nhận", "Bạn có chắc chắn muốn xóa chức năng này?")) {
            try {
                functionService.deleteFunction(selectedFunction.getId());
                ErrorDialog.showInfo("Thành công", "Đã xóa chức năng thành công");
                loadFunctions();
            } catch (DbException e) {
                ErrorDialog.showDbError(e.getMessage());
            } catch (Exception e) {
                ErrorDialog.showError("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
            }
        }
    }
    
    private void openFunctionDialog(Function function) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/function/FunctionDialogView.fxml"));
            Parent root = loader.load();
            FunctionDialogController controller = loader.getController();
            controller.setFunction(function);
            controller.setParentController(this);
            
            Scene scene = new Scene(root, 500, 400);
            Stage stage = new Stage();
            stage.setTitle(function == null ? "Thêm chức năng" : "Sửa chức năng");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            ErrorDialog.showError("Lỗi", "Không thể mở dialog: " + e.getMessage());
        }
    }
    
    public void refreshTable() {
        loadFunctions();
    }
}


