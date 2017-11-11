package sample;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

    @FXML private PasswordField txtPassword;

    @FXML private Pane paneConnect;


    public LoginController() {
        Thread t = new Thread(() -> {
            while(!LoginController.formShown) Thread.yield();
            updateServerIP();
            formLoad();
        });
        t.start();
    }

    private void formLoad() {
        EventHandler<KeyEvent> eh = new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent ke)
            {
                if (ke.getCode().equals(KeyCode.ENTER))
                {
                    cmdConnect.fire();
                }
            }
        };
        txtServerIP.setOnKeyPressed(eh);
        txtNickname.setOnKeyPressed(eh);
        txtPassword.setOnKeyPressed(eh);
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
            ip = (Networking.getUrlSource("http://p74apps.tk/JT/ip.txt"));
            //ip = "localhost";
            while(txtServerIP == null) {Thread.yield();}
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
            stage.setScene(new Scene(fxmlLoader.load(), 800, 480));
            stage.setMinWidth(800);
            stage.setMinHeight(480);

            // Hide this current window (if this is what you want)
            ((Node)(event.getSource())).getScene().getWindow().hide();

            MainController mc = (MainController)fxmlLoader.getController();
            stage.setOnCloseRequest(e -> {Platform.exit(); System.exit(0);});
            final ChangeListener<Number> listener = new ChangeListener<Number>()
            {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, final Number newValue)
                {
                    mc.formResize(stage.widthProperty().intValue(), stage.heightProperty().intValue());
                }
            };
            stage.widthProperty().addListener(listener);
            stage.heightProperty().addListener(listener);
            String s2 = txtServerIP.getText();
            String nick = txtNickname.getText();
            String pass = txtPassword.getText();
            System.out.println("CONNECT " + s2);
            mc.getSrv().connect(s2, 7000, nick + ":" + Utils.stringToMD5(pass));
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
