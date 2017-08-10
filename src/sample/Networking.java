package sample;
import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class Networking {

    public boolean DEBUG = true;

    public DatagramSocket socket = null;

    InetAddress serverIp = null;
    public String humanIp = null;
    int serverPort = 0;
    int myPort = 0;

    public Networking(InetAddress serverIp, int serverPort) {
        try {
            socket = new DatagramSocket();
        } catch (Exception ex) {
            System.out.println(ex.toString());
            return; }
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        this.myPort = socket.getLocalPort();
    }

    public int sendVoicePacket(VoicePacket packet) {
        DatagramPacket sendPacket;
        int size = packet.getPayload().length + 3;
        byte[] buf = new byte[size + 3];
        System.arraycopy(packet.getPayload(), 0, buf, 3, packet.getPayload().length);
        buf[0] = (byte)packet.getMyID();
        buf[1] = (byte)packet.getMsgType();
        buf[2] = (byte)packet.getmArg();

        sendPacket = new DatagramPacket(buf, size);
        sendPacket.setAddress((serverIp));
        sendPacket.setPort(serverPort);
        try {
            socket.send(sendPacket);
            // System.out.println(size + " uploaded");
            return 0;
        }
        catch (Exception ex) {
            if (DEBUG) { System.out.println(ex.toString()); ex.printStackTrace(); }
            return -1;
        }
    }

    public int receiveVoicePacketBlocking(VoicePacket packet) {
        byte[] receiveData = new byte[2100];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        try {
            socket.setSoTimeout(100);
            socket.receive(receivePacket);
            int actualSize = receivePacket.getLength();

            //System.out.println(actualSize + " downloaded");

            packet.setPayload(new byte[actualSize - 3]);
            System.arraycopy(receiveData, 3,packet.getPayload(), 0, actualSize - 3);
            packet.setMyID(receiveData[0]);
            packet.setMsgType(receiveData[1]);
            packet.setmArg(receiveData[2]);

            return 0;
        }
        catch (Exception ex) {
            return -1;
        }
    }

    public static String getUrlSource(String url) throws IOException {
        URL uurl = new URL(url);
        URLConnection yc = uurl.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                yc.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuilder a = new StringBuilder();
        while ((inputLine = in.readLine()) != null)
            a.append(inputLine);
        in.close();

        return a.toString();
    }
}
