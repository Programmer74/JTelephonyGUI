package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.event.ActionEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class MainController {

    public ServerInteraction getSrv() {
        return srv;
    }

    private ServerInteraction srv = new ServerInteraction();
    private Audio audio;

    public Date beganTalking = new Date();

    public static boolean formShown = false;

    @FXML private Button cmdCall;
    @FXML private Button cmdConnect;
    @FXML private Button cmdFinishCall;

    @FXML private TextField txtCallTo;
    @FXML private TextField txtServerIP;
    @FXML private TextField txtNickname;

    @FXML private TextArea txtLog;

    @FXML private Label lblStatus;

    @FXML private Pane paneMain;
    @FXML private Pane paneConnect;

    @FXML private ListView<String> lbUsersOnline;

    @FXML private TextFlow tfUser;
    @FXML private TextFlow tfMe;


    private boolean isWaitingAnswer = false;
    private boolean launchedSound = false;
    private int callWaitCounter = 0;
    private final int callWaitTime = 30;
    private String currentStatus = "";

    public MainController() {
        Thread t = new Thread(() -> {
            while(!MainController.formShown) Thread.yield();
        });
        t.start();
    }

    private void fillUserInfo(TextFlow tf, String nickname) {
        String userInfo[] = srv.doCommand("info", nickname).split(":");

        Text login = new Text(userInfo[0] + "\n");
        login.setFont(Font.font("Helvetica", 20));
        Text names = new Text(userInfo[1] + " " + userInfo[2] + "\n");
        names.setFont(Font.font("Helvetica", 16));
        Text city = new Text(userInfo[3]);
        city.setFont(Font.font("Helvetica", 12));
        tf.getChildren().clear();
        tf.getChildren().addAll(login, names, city);
    }

    private void formLoad() {

        Text text1 = new Text("User info\n");
        text1.setFont(Font.font("Helvetica", 16));
        Text text2 = new Text("goes here");
        text2.setFont(Font.font("Helvetica", 12));
        tfUser.getChildren().addAll(text1, text2);

        fillUserInfo(tfMe, "!!me");
    }

    public static String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    private void appendTxtLog(String status) {
        if (!(status.split(" ")[0].equals("Talking"))) {
            txtLog.appendText(getCurrentDate() + " " + status + "\n");
        }
    }

    //updates status label
    private void setStatus(String status) {
        currentStatus = status;

        Task task = new Task<Void>() {
            @Override
            public Void call() throws Exception {

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        lblStatus.setText(currentStatus);
                        appendTxtLog(currentStatus);
                    }
                });
                return null;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();


        System.out.println(status);
    }

    //when we detect incoming call in proxy
    private void notifyIncomingCall(String arg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert msgbox = new Alert(Alert.AlertType.INFORMATION);
                msgbox.setTitle("Incoming call");
                msgbox.setContentText("Incoming call from " + arg + ". Accept?");
                msgbox.getButtonTypes().remove(0);
                msgbox.getButtonTypes().add(new ButtonType("Yes", ButtonBar.ButtonData.YES));
                msgbox.getButtonTypes().add(new ButtonType("No", ButtonBar.ButtonData.NO));

                Optional<ButtonType> result = msgbox.showAndWait();
                if (result.get().getText().equals("Yes")) {
                    srv.doCommand("call_accept");


                    //   serverMutex.unlock();
                    //audio.Talk();
                    //audio.Listen();
                    srv.setMeTalking(true);
                    srv.setHasBeenTalking(true);
                    isWaitingAnswer = false;
                    launchedSound = false;
                    //shouldUpdate = false;


                } else {
                    srv.doCommand("call_decline");
                    isWaitingAnswer = false;
                }

            }});
    }
    //when person we are talking to finishes call
    private void notifyStoppedCall() {
        //srv.hasBeenTalking = false;
        MessageBoxes.showAlert("Your call was finished.", "Call info");
        //srv.doCommand("call_hangup");
    }
    //when person we are talking to breaks connection
    private void notifyCompanionErrorCall() {
        srv.hasBeenTalking = false;
        MessageBoxes.showAlert("Your companion suddenly disconnected", "Call info");
        srv.doCommand("call_hangup");
    }
    private void notifyCompanionBusy() {
        srv.hasBeenTalking = false;
        MessageBoxes.showAlert("Your companion is busy now", "Call info");
        srv.doCommand("call_hangup");
    }
    private void notifyCallError() {
        srv.hasBeenTalking = false;
        MessageBoxes.showAlert("An error occurred in call", "Call info");
        srv.doCommand("call_hangup");
        System.exit(0);
    }
    //when person we are trying to call says no
    private void notifyNotAcceptedCall() {
        System.out.println("Note: your call was not accepted.");
        MessageBoxes.showAlert("Your call was not accepted.", "Call info");
        srv.doCommand("call_hangup");
        setStatus("Call declined");
    }
    //updates timings and stuff
    private void updateTalkingStatus(String arg) {
        String status;
        long totalSeconds = ((new Date()).getTime() - (beganTalking.getTime())) / 1000;
        long hours = totalSeconds / 60 / 60;
        long minutes = totalSeconds / 60 % 60;
        long seconds = totalSeconds % 60;
        status = String.format("Talking to %s %02d:%02d:%02d", arg, hours, minutes, seconds);
        //status += minutes + ":" + seconds;
        setStatus(status);
        txtCallTo.setDisable(true);
        cmdCall.setDisable(true);
        cmdFinishCall.setVisible(true);
    }

    private void updateClientsNoWrap() {


        String allClients = srv.doCommand("ls");
        ObservableList<String> items = FXCollections.observableArrayList();
        for(String nick: allClients.split(";")) {
            if (!nick.equals("")) {
                items.add(nick);
            }
        }
        lbUsersOnline.setItems(items);
        lbUsersOnline.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<String>() {
                    public void changed(ObservableValue<? extends String> ov,
                                        String old_val, String new_val) {
                        if (new_val == null) return;
                        if (new_val.equals(old_val)) return;
                        if (txtCallTo.getText().equals(new_val)) return;
                        txtCallTo.setText(new_val);
                        cmdCall.setDisable(false);
                        fillUserInfo(tfUser, new_val);
                    }
                });
    }
    private void updateClients() {
        Task task = new Task<Void>() {
            @Override
            public Void call() throws Exception {

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        updateClientsNoWrap();
                    }
                });
                return null;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

    }

    //MAIN STATUS THREAD
    Thread statusThread = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean shouldUpdate = true;
            while (shouldUpdate && srv.isConnected()) {
                //System.out.println(">> ok");
//                        serverMutex.lock();
                try {
                    String serverAnswer = srv.doCommand("status");

                    //System.out.println(">" + serverAnswer);
                    String subans[] = serverAnswer.split(";");
                    for (int i = 0; i < subans.length; i++) {
                        String cmd = subans[i].split(" ")[0];
                        String arg = subans[i].split(" ")[1];

                        //System.out.println(cmd + "(" + arg + ")");

                        if (cmd.equals("talking_to")) {

                            if (!launchedSound) {
                                launchedSound = true;
                                if (!audio.isListening()) audio.Listen();
                                if (!audio.isTalking()) audio.Talk();
                                beganTalking = new Date();
                                //setStatus("Began talking to " + arg);
                            }

                            srv.setMeTalking(true);
                            srv.setHasBeenTalking(true);
                            i = 100;
                            updateTalkingStatus(arg);
                            callWaitCounter = 0;

                            break;
                        } else {
                            srv.setMeTalking(false);
                            if (srv.isHasBeenTalking()) {
                                audio.StopListening();
                                audio.StopTalking();
                                //srv.hasBeenTalking = false;
                            }

                        }

                        if (cmd.equals("called_by") && (srv.isMeTalking() == false)) {
                            if (audio.isTalking) break;
                            if (isWaitingAnswer) break;
                            isWaitingAnswer = true;
                            notifyIncomingCall(arg);

                        }
                        if (cmd.equals("nothing") && (srv.hasBeenTalking)) {
                            /*if (isWaitingAnswer) {
                                //System.out.println("OH SHIT");
                                //System.exit(0);
                            }
                            cmdFinishCallPressed(null);
                            notifyStoppedCall();*/

                        }
                        if (cmd.equals("calling_to") && arg.equals("finished")) {
                            cmdFinishCallPressed(null);
                            notifyStoppedCall();
                        }
                        if (cmd.equals("calling_to") && arg.equals("hanged")) {
                            callWaitCounter = 0;
                            if (srv.hasBeenTalking) {
                                //we were talking but hangup happened
                                notifyCompanionErrorCall();
                                cmdFinishCallPressed(null);
                            } else {
                                //we havent been talking, client we were calling said no
                                notifyNotAcceptedCall();
                                hangupCall();
                            }
                        }
                        if (cmd.equals("calling_to") && (arg.equals("wait"))) {
                            System.out.println("call wait");
                            callWaitCounter++;
                            if (callWaitCounter == callWaitTime) {
                                callWaitCounter = 0;
                                notifyNotAcceptedCall();
                            }
                        }

                    }
                    updateClients();
                    // try {serverMutex.unlock(); } catch (Exception ex) {}
                    Thread.yield();
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    System.out.println("StatusThread: " + ex.toString());
                    ex.printStackTrace();
                }
                ;

            }
        }
    });

    //@FXML
    public void doConnectSuccess() {

        audio = srv.getAudio();
        statusThread.start();
        audio.nwConnection.sendVoicePacket(new VoicePacket(audio.getMyID(), 0, 0, new byte[3]));

        formLoad();
    }

    @FXML
    void cmdCallPressed(ActionEvent event) {
        String s = txtCallTo.getText();
        if (s.equals("")) return;
        System.out.println("CALL " + s);
        String ans = srv.doCommand("call", s);
        System.out.println(ans);
        if (ans.equals("busy")) {
            notifyCompanionBusy();
            return;
        }
        if (ans.equals("error")) {
            notifyCallError();
            return;
        }
        setStatus("Calling to " + s);
    }

    void hangupCall() {
        setStatus("Call finished");
        srv.doCommand("call_hangup");

        txtCallTo.setDisable(false);
        cmdCall.setDisable(false);
        cmdFinishCall.setVisible(false);

    }

    @FXML
    void cmdFinishCallPressed(ActionEvent event) {

        hangupCall();

        audio.StopListening();
        audio.StopTalking();

        srv.setHasBeenTalking(false);

        launchedSound = false;
    }
}
