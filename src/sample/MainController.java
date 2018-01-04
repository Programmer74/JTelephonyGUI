package sample;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.codefx.libfx.control.webview.WebViewHyperlinkListener;
import org.codefx.libfx.control.webview.WebViews;

import javax.imageio.ImageIO;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    @FXML private Button cmdSendImage;
    @FXML private Button cmdSendDocument;
    @FXML private Button cmdLogout;

    @FXML private TextField txtCallTo;
    @FXML private TextField txtServerIP;
    @FXML private TextField txtNickname;

    @FXML private TextArea txtLog;

    @FXML private Label lblStatus;

    @FXML private Pane paneMain;
    @FXML private Pane leftPane;
    @FXML private Pane rightPane;
    @FXML private Pane paneConnect;

    @FXML private ListView<String> lbUsersOnline;

    @FXML private TextFlow tfUser;
    @FXML private TextFlow tfMe;

    @FXML private WebView wvMessageHistory;
    @FXML private TextArea txtMessageInput;

    @FXML private MenuItem mnuCall;
    @FXML private MenuItem mnuFinishCall;

    private boolean isWaitingAnswer = false;
    private boolean launchedSound = false;
    private int callWaitCounter = 0;
    private final int callWaitTime = 300;
    private String currentStatus = "";

    public MainController() {
        Thread t = new Thread(() -> {
            while(!MainController.formShown) Thread.yield();
            updateClientsNoWrap();
        });
        t.start();
    }

    private void updateUserInfoUI(TextFlow tf, String nickname) {
        try {
            String userInfo[] = srv.doCommand("info", nickname).split(":");

            Text login = new Text(userInfo[0] + "\n");
            login.setFont(Font.font("Helvetica", 24));
            Text names = new Text(userInfo[1] + " " + userInfo[2] + "\n");
            names.setFont(Font.font("Helvetica", 16));
            Text city = new Text(userInfo[3] + "\n");
            city.setFont(Font.font("Helvetica", 12));
            Text status = new Text(userInfo[4]);
            status.setFont(Font.font("Helvetica", 10));


            tf.getChildren().clear();
            if (!userInfo[5].equals("null")) {

                BufferedImage bufferedImage=null;
                try {
                    InputStream inputStream = new ByteArrayInputStream(srv.doBinaryAnswerCommand("getimg", userInfo[5]));
                    bufferedImage = ImageIO.read(inputStream);
                    //bufferedImage = Utils.resizeBufferedImage(bufferedImage, 64, 64);
                    ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
                    imageView.setFitHeight(24);
                    imageView.setFitWidth(24);
                    tf.getChildren().add(imageView);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }

            tf.getChildren().addAll(login, names, city, status);

            if (userInfo[4].equals("online") && !nickname.equals("!!me")) {
                cmdCall.setDisable(false);
                mnuCall.setDisable(false);
            } else {
                mnuCall.setDisable(true);
                cmdCall.setDisable(true);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void formLoad() {

        formShown = true;

        Text text1 = new Text("User info\n");
        text1.setFont(Font.font("Helvetica", 16));
        Text text2 = new Text("goes here");
        text2.setFont(Font.font("Helvetica", 12));
        tfUser.getChildren().addAll(text1, text2);

        updateUserInfoUI(tfMe, "!!me");

        WebViewHyperlinkListener eventPrintingListener = event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                //System.out.println(WebViews.hyperlinkEventToString(event));
                webviewLinkFired(event.getURL().toString(), event.getDescription());
            }
            return false;
        };
        WebViews.addHyperlinkListener(wvMessageHistory, eventPrintingListener);

        txtMessageInput.addEventFilter(KeyEvent.ANY, evt -> {
            if (evt.getEventType() == KeyEvent.KEY_PRESSED) {
                //txtMessageInput.setText(evt.getCode().toString());
                if (evt.getCode().toString().equals("ENTER")) {
                    if (!evt.isShiftDown()) {
                        cmdSendMessagePressed(null);
                        evt.consume();
                    } else {
                        txtMessageInput.appendText(System.getProperty("line.separator"));
                    }
                }
            }
        });
    }

    public void formResize(int width, int height) {
        if (!MainController.formShown) return;
        System.out.println(width + ":" + height);
        paneMain.prefWidthProperty().set(width - 400);
        paneMain.prefHeightProperty().set(height - 40);
        wvMessageHistory.prefWidthProperty().set(paneMain.prefWidthProperty().get() - wvMessageHistory.layoutXProperty().get() * 2);
        wvMessageHistory.prefHeightProperty().set(paneMain.prefHeightProperty().get() - 100);
        txtMessageInput.layoutYProperty().set(height - txtMessageInput.prefHeightProperty().get() - 62);
        txtMessageInput.prefWidthProperty().set(width - 460);
        cmdSendImage.layoutYProperty().set(txtMessageInput.layoutYProperty().get() + 2);
        cmdSendImage.layoutXProperty().set(txtMessageInput.prefWidthProperty().get() + 6);
        cmdSendDocument.layoutXProperty().set(cmdSendImage.layoutXProperty().get());
        cmdSendDocument.layoutYProperty().set(cmdSendImage.layoutYProperty().add(cmdSendImage.prefHeightProperty()).add(5).get());
        /* Sorry, I have neither idea nor time on how to use those creepy layout managers */
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


        //System.out.println(status);
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
    boolean shownCallStopped = false;
    private void notifyStoppedCall() {
        if (shownCallStopped) return;
        shownCallStopped = true;
        MessageBoxes.showAlert("Your call was finished.", "Call info");
        shownCallStopped = false;
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
        mnuCall.setDisable(true);
        cmdFinishCall.setVisible(true);
        mnuFinishCall.setDisable(false);
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
                        //cmdCall.setDisable(false);
                        updateUserInfoUI(tfUser, new_val);
                        updateMessageHistory(new_val);
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

    int updateClientsCounter = 0;

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
                        String arg;
                        try {
                            arg = subans[i].split(" ")[1];
                        } catch (Exception ex) {
                            arg = "dummy";
                        }

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
                            if (audio.isTalking()) break;
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
                        if (cmd.equals("has") && arg.equals("incoming_messages")) {
                            updateMessageHistory(txtCallTo.getText());
                        }
                    }
                    updateClientsCounter++;
                    //if (updateClientsCounter == 10) {
                        updateClients();
                        updateClientsCounter = 1;
                    //}
                    // try {serverMutex.unlock(); } catch (Exception ex) {}
                    Thread.yield();
                    Thread.sleep(200);
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
        audio.getNwConnection().sendVoicePacket(new VoicePacket(audio.getMyID(), 0, 0, new byte[3]));

        formLoad();
    }

    @FXML
    void cmdCallPressed(ActionEvent event) {
        if (cmdCall.isDisabled()) return;
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
        mnuCall.setDisable(false);
        cmdFinishCall.setVisible(false);
        mnuFinishCall.setDisable(true);
    }

    @FXML
    void cmdFinishCallPressed(ActionEvent event) {
        hangupCall();

        audio.StopListening();
        audio.StopTalking();

        srv.setHasBeenTalking(false);

        launchedSound = false;
    }

    void updateMessageHistory(String nickname) {
        String rawhist = srv.doCommand("getmsg", nickname);
        String from;
        String msg;
        String attachment = "";

        StringBuilder historySb = new StringBuilder();

        historySb.append("<html>");
        historySb.append("<head>");
        historySb.append("   <script language=\"javascript\" type=\"text/javascript\">");
        historySb.append("       function toBottom(){");
        historySb.append("           window.scrollTo(0, document.body.scrollHeight);");
        historySb.append("       }");
        historySb.append("   </script>");
        historySb.append("</head>");
        historySb.append("<body onload='toBottom()'>");

        String histentry;
        String[] histentries = rawhist.split(";");

        for (int i = histentries.length - 1; i >= 0; i--) {
            histentry = histentries[i];
            if (histentry.equals("")) continue;
            String[] msgentry = histentry.split(":");
            from = msgentry[0];
            msg = Utils.Base64Decode(msgentry[1]);
            attachment = Utils.Base64Decode(msgentry[2]);

            historySb.append("<p>");
            historySb.append("<b>").append(from).append("</b>: ");

            msg = msg.replaceAll("[<]", "&lt;");
            msg = msg.replaceAll("[>]", "&gt;");
            msg = msg.replaceAll("\n|\r\n|\n\r|\r", "<br>");

            historySb.append(msg);
            if (!attachment.equals("null")) {
                if (attachment.charAt(0) == '-') {
                    historySb.append("<br><a href=").append("http://localhost:9000/").append(attachment).append(">");
                    //historySb.append("Image");
                    historySb.append("<img src=\"file://").append(downloadImageAndGetPath(attachment.substring(1))).append("\" width = 320px alt=Image />");
                    historySb.append("</a>");
                } else {
                    historySb.append("<br><a href=").append(attachment).append(">Document: ").append(attachment).append("</a>");
                }
            }
            historySb.append("</p>\n");
            /*
            history = "[" + from + "] : " + msg + "\n" + history;
            if (!attachment.equals("null")) {
                history = from + " sent " + attachment + "\n" + history;
            }*/
        }
        historySb.append("</body>");
        historySb.append("</html>");
        //System.out.println(historySb.toString());
        Platform.runLater(new Runnable() {
            @Override public void run() {
                wvMessageHistory.getEngine().loadContent(historySb.toString());
            }
        });

    }

    @FXML
    void cmdSendMessagePressed(ActionEvent event) {
        if (txtMessageInput.getText().equals("")) return;
        if (txtCallTo.getText().equals("")) return;
        srv.doCommand("sendmsg", txtCallTo.getText() + ":" + Utils.Base64Encode(txtMessageInput.getText()) + ":null");
        updateMessageHistory(txtCallTo.getText());
        txtMessageInput.setText("");
    }

    @FXML
    void cmdSendImagePressed(ActionEvent event) {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open File");
        FileChooser.ExtensionFilter jpgFilter = new FileChooser.ExtensionFilter("JPEG files (*.jpg)", "*.jpg");
        FileChooser.ExtensionFilter pngFilter = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png");
        FileChooser.ExtensionFilter gifFilter = new FileChooser.ExtensionFilter("GIF files (*.gif)", "*.gif");
        FileChooser.ExtensionFilter bmpFilter = new FileChooser.ExtensionFilter("BMP files (*.bmp)", "*.bmp");
        chooser.getExtensionFilters().addAll(jpgFilter, pngFilter, gifFilter, bmpFilter);

        File file = chooser.showOpenDialog(new Stage());
        if (file == null) return;

        Integer id = srv.doSendImage(file.toString());
        if (id <= 0) {
            MessageBoxes.showAlert("An error while attaching image", "Img info");
            return;
        }
        if (txtMessageInput.getText().equals("")) txtMessageInput.setText(" ");
        srv.doCommand("sendmsg", txtCallTo.getText() + ":" + Utils.Base64Encode(txtMessageInput.getText()) + ":" + id);

        updateMessageHistory(txtCallTo.getText());
        txtMessageInput.setText("");
    }

    @FXML
    void cmdSendDocumentPressed(ActionEvent event) {

        // request node address from user and add
        TextInputDialog input = new TextInputDialog();
        input.getEditor().setPromptText("http://something/");
        input.setTitle("New Link");
        input.setHeaderText("Enter new document address");
        Optional<String> result = input.showAndWait();

        if (result.isPresent()) {
            String address = result.get().trim();

            if (!address.startsWith("https://")) {
                if (!address.startsWith("http://")) {
                    address = "https://" + address;
                }
            }

            System.out.println(address);

            Integer id = srv.doSendDocument(address);
            if (id <= 0) {
                MessageBoxes.showAlert("An error while attaching document", "Doc info");
                return;
            }

            if (txtMessageInput.getText().equals("")) txtMessageInput.setText(" ");
            srv.doCommand("sendmsg", txtCallTo.getText() + ":" + Utils.Base64Encode(txtMessageInput.getText()) + ":" + id);

            updateMessageHistory(txtCallTo.getText());
            txtMessageInput.setText("");
        }
    }

    @FXML
    void cmdLogoutPressed(ActionEvent event) {
        // Magic. Do not touch.
        LoginController lc = new LoginController();
        lc.invalidateTokenFileAndClose(false);
    }

    @FXML
    void cmdClosePressed(ActionEvent event) {
        System.exit(0);
    }

    private Map<Integer, String> imageToPath = new HashMap<>();

    private String downloadImageAndGetPath(String strimageid) {
        try {
            Integer imageid = Integer.parseInt(strimageid);
            if (imageToPath.get(imageid) != null) {
                System.out.println(imageToPath.get(imageid));
                return imageToPath.get(imageid);
            }

            String imagepath = "";
            if (System.getProperty("os.name").equals("Linux")) {
                imagepath = System.getProperty("java.io.tmpdir") + "/" + imageid + ".jpg";
            } else {
                imagepath = System.getProperty("java.io.tmpdir").replace("\\", "/") + imageid + ".jpg";
            }

            File f = new File(imagepath);
            if(!(f.exists() && !f.isDirectory())) {
                //there is no file in cache, should re-download it now

                byte[] imagebytes = srv.doBinaryAnswerCommand("getimg", imageid.toString());
                FileOutputStream fos = new FileOutputStream(imagepath);
                fos.write(imagebytes);
                fos.close();
            }
            imageToPath.put(imageid, imagepath);
            System.out.println(imagepath) ;
            return imagepath;
        } catch (Exception ex) {
            return null;
        }
    }

    void webviewLinkFired(String url, String description) {
        System.out.println("<" + url + "><" + description + ">");
        if (description.equals("")) {
            //data:image/jpeg;base64,
            //System.getProperty("java.io.tmpdir")
            try {

                String imageid = url.split("[/]")[3].substring(1);

                String imagepath = downloadImageAndGetPath(imageid);

                if (System.getProperty("os.name").equals("Linux")) {
                    Runtime.getRuntime().exec("xdg-open " + imagepath);
                } else if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
                    Desktop.getDesktop().open(new File(imagepath));
                } else {
                    new ProcessBuilder("x-www-browser", "file://" + imagepath).start();
                }
                //new ProcessBuilder("x-www-browser", "data:image/jpeg;base64," + base64image).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (description.startsWith("Document:")){
            try {
                new ProcessBuilder("x-www-browser", url).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Platform.runLater(() -> {
            wvMessageHistory.getEngine().executeScript("window.stop()");
        });
    }

    @FXML
    void mnuQualityChanged(ActionEvent event) {
        int quality = 2;
        switch (((MenuItem)(event.getSource())).getText()) {
            case "Worst":
                quality = 0;
                break;
            case "Usable":
                quality = 1;
                break;
            case "Decent":
                quality = 2;
                break;
            case "Fine":
                quality = 3;
                break;
            case "Great":
                quality = 4;
                break;
            case "Best":
                quality = 5;
                break;
        }
        if (audio != null) {
            audio.setMyQualitySetupForMic(quality);
        }
    }

    @FXML
    void mnuChunkSizeChanged(ActionEvent event) {
        int chunkSize = Integer.parseInt(((MenuItem)(event.getSource())).getText());
        if (audio != null) {
            audio.setMicCapturedDataSize(chunkSize);
        }
    }
}
