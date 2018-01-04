package sample;

import java.util.*;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class LoginController {

    private String tokenString = null;
    public static boolean formShown = false;

    @FXML private Button cmdConnect;

    @FXML private TextField txtServerIP;
    @FXML private TextField txtNickname;

    @FXML private PasswordField txtPassword;

    @FXML private Pane paneConnect;

    private final String nicknameFilePath = "nickname.dat";
    private final String tokenFilePath = "token.dat";

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

        try {
            txtNickname.setText(new Scanner(new File(nicknameFilePath)).useDelimiter("\\Z").next());
            String content = new Scanner(new File(tokenFilePath)).useDelimiter("\\Z").next();
            tokenString = content;
            Platform.runLater(() -> cmdConnect.fire());
        } catch (IOException ex) {
            tokenString = null;
            System.out.println("No token file =(");
        }

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

    public void invalidateTokenFileAndClose(boolean rebootRequired) {
        File file = new File(tokenFilePath);
        file.delete();
        file = new File(nicknameFilePath);
        file.delete();
        if (rebootRequired) {
            try {
                final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());

                /* is it a jar file? */
                if (!currentJar.getName().endsWith(".jar"))
                    return;

                /* Build command: java -jar application.jar */
                final ArrayList<String> command = new ArrayList<String>();
                command.add(javaBin);
                command.add("-jar");
                command.add(currentJar.getPath());

                final ProcessBuilder builder = new ProcessBuilder(command);
                builder.start();

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    @FXML
    void cmdConnectPressed(ActionEvent event) {
        cmdConnect.setDisable(true);
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

            if (tokenString == null) {
                mc.getSrv().connect(s2, 7000, nick + ":" + Utils.stringToMD5(pass));
            } else {
                mc.getSrv().connect(s2, 7000, nick + ":-" + tokenString);
            }

            if (mc.getSrv().isConnected()) {

                if (tokenString == null) {
                    String achievedToken = mc.getSrv().doCommand("gettoken");

                    try (PrintWriter out = new PrintWriter(tokenFilePath)) {
                        System.out.println("Token " + achievedToken + " saved.");
                        out.print(achievedToken);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                }

                try (PrintWriter out = new PrintWriter(nicknameFilePath)) {
                    System.out.println("Nick " + txtNickname.getText() + " saved.");
                    out.print(txtNickname.getText());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }

                stage.show();
                mc.doConnectSuccess();

            } else {
                MessageBoxes.showCriticalErrorAlert("Cannot connect to server.\nTry again later.", "Error");
                if (tokenString != null) {
                    invalidateTokenFileAndClose(true);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
