package sample;

import sun.awt.Mutex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
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
            socket.setTcpNoDelay(true);
            //socket.setSoTimeout(5000);
            inputs = new DataInputStream(socket.getInputStream());
            outputs = new DataOutputStream(socket.getOutputStream());

            audio = new Audio(48000, 1, InetAddress.getByName(ip), 7002, 0);

            //outputs.writeUTF("nick " + nick);
            String myIdStr = doCommand("nick", nick); //inputs.readUTF();
            myId = Integer.parseInt(myIdStr);

            if (myId < 0) {
                throw new Exception("Wrong password");
            }

            System.out.println("my id is " + myId);
            audio.setMyID(myId);

            System.out.println("register ok");
            isConnected = true;
        } catch (Exception ex) {
            System.err.println("In connect: " + ex.toString());
            isConnected = false;
        }
    }

    byte[] buf = new byte[1024 * 1024];
    int buf_len;

    public byte[] doBinaryAnswerCommand(String command, String arg) {
        synchronized (outputs){
            try {
                String to_send = command + " " + arg;
                outputs.writeInt(to_send.getBytes().length);
                outputs.write(to_send.getBytes());
                outputs.flush();
                //System.out.println("wrote out " + command + " : " + arg);

                buf_len = inputs.readInt();
                inputs.readFully(buf, 0, buf_len);
                //String ans = new String(buf, 0, buf_len);

                //System.out.println("cmd: " + command + "(" + arg + ")" + " reply: " + ans);
                return Arrays.copyOfRange(buf, 0, buf_len);
            } catch (Exception ex) {
                System.err.println("In do command exception: " + ex.toString());
                isConnected = false;
                MessageBoxes.showCriticalErrorAlert("Connection to server was unexpectedly closed.", "Server error");
                return null;
            }
        }
    }

    public String doCommand(String command, String arg) {
        return new String(doBinaryAnswerCommand(command, arg));
    }
    public String doCommand(String command) { return doCommand(command, "dummy");}
}

