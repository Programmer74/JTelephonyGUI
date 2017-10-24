package sample;

import sun.awt.Mutex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by Hotaro on 09.07.2017.
 */
public class ServerInteraction {

    public int myId = 0;

    public void setMeTalking(boolean meTalking) {
        this.meTalking = meTalking;
    }

    public void setHasBeenTalking(boolean hasBeenTalking) {
        this.hasBeenTalking = hasBeenTalking;
    }

    public boolean meTalking = false;
    public boolean hasBeenTalking = false;

    public boolean isConnected() {
        return isConnected;
    }

    private boolean isConnected = false;

    DataInputStream inputs;
    DataOutputStream outputs;
    Socket socket;

    public int getMyId() {
        return myId;
    }

    public boolean isMeTalking() {
        return meTalking;
    }

    public boolean isHasBeenTalking() {
        return hasBeenTalking;
    }

    public Audio getAudio() {
        return audio;
    }

    Audio audio;

    public ServerInteraction() {
        isConnected = false;
    }


    public void connect(String ip, int port, String nick) {
        try {

            socket = new Socket(InetAddress.getByName(ip), port);
            //socket.setSoTimeout(5000);
            inputs = new DataInputStream(socket.getInputStream());
            outputs = new DataOutputStream(socket.getOutputStream());

            audio = new Audio(48000, 1, InetAddress.getByName(ip), 7002, 0);

            outputs.writeUTF("nick " + nick);
            String myIdStr = inputs.readUTF();
            myId = Integer.parseInt(myIdStr);
            System.out.println("my id is " + myId);
            audio.setMyID(myId);

            System.out.println("register ok");
            isConnected = true;
        } catch (Exception ex) {
            System.err.println("In connect: " + ex.toString());
            isConnected = false;
        }
    }

    public String doCommand(String command, String arg) {
        try {
            outputs.writeUTF(command + " " + arg);

            String ans = inputs.readUTF();
            //System.out.println("cmd: " + command + "(" + arg + ")" + " reply: " + ans);
            return ans;
        } catch (Exception ex) {
            System.err.println("In do command exception: " + ex.toString());
            isConnected = false;
            MessageBoxes.showCriticalErrorAlert("Connection to server was unexpectedly closed.", "Server error");
            return "";
        }
    }
    public String doCommand(String command) { return doCommand(command, "dummy");}
}

