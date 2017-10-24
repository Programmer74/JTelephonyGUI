package sample;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class MessageBoxes {

    //alerts
    public static void showAlert(String prompt, String title) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert msgbox = new Alert(Alert.AlertType.INFORMATION);
                msgbox.setTitle(title);
                msgbox.setContentText(prompt);
                msgbox.showAndWait();
            }
        });
    }
    public static void showCriticalErrorAlert(String prompt, String title) {
        Platform.runLater(() -> {
            Alert msgbox = new Alert(Alert.AlertType.ERROR);
            msgbox.setTitle(title);
            msgbox.setContentText(prompt);
            msgbox.showAndWait();
            System.exit(-1);
        });
    }

}
