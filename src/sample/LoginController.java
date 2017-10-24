package sample;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class LoginController {


    public static boolean formShown = false;

    @FXML private Button cmdConnect;

    @FXML private TextField txtServerIP;
    @FXML private TextField txtNickname;


    @FXML private Pane paneConnect;


    public LoginController() {
        Thread t = new Thread(() -> {
            while(!LoginController.formShown) Thread.yield();
            updateServerIP();
        });
        t.start();
    }

    public static String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    //updates server ip address
    private void updateServerIP() {
        String ip = "";
        try {
            //ip = (Networking.getUrlSource("http://p74apps.tk/JT/ip.txt"));
            //ip = "146.185.142.134";
            ip = "localhost";
            //while(txtServerIP == null) {Thread.yield();}
            txtServerIP.setText(ip);
            //txtServerIP.setDisable(true);
        } catch (Exception ex ) {ex.printStackTrace();}
    }

    @FXML
    void cmdConnectPressed(ActionEvent event) {

        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("main.fxml"));
            Stage stage = new Stage();
            stage.setTitle("JT");
            stage.setScene(new Scene(fxmlLoader.load(), 300, 300));

            // Hide this current window (if this is what you want)
            ((Node)(event.getSource())).getScene().getWindow().hide();

            MainController mc = (MainController)fxmlLoader.getController();

            String s2 = txtServerIP.getText();
            String nick = txtNickname.getText();
            System.out.println("CONNECT " + s2);
            mc.getSrv().connect(s2, 7000, nick);
            if (mc.getSrv().isConnected()) {
                stage.show();
                mc.doConnectSuccess();
            } else {
                MessageBoxes.showCriticalErrorAlert("Cannot connect to server.\nTry again later.", "Error");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
