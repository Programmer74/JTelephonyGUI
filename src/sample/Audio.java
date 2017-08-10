package sample;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import javax.swing.JOptionPane;
import java.io.*;
import java.net.*;

class AudioFilter {
    public static void lowPassFilter(byte[] data, int length, int smoothing) {
        if (smoothing == 0) return;
        int value = ((data[0] << 8) | data[1]) << 16 >> 16; // start with the first input
        for (int i = 2; i < length; i += 2){
            int currentValue = ((data[i] << 8) | data[i + 1]) << 16 >> 16;
            value += (int)((currentValue - value) * 1.0 / smoothing);
            data[i] = (byte)((value >> 8) & 0xFF);
            data[i + 1] = (byte)(value & 0xFF);
        }
    }
}

public class Audio {

    boolean DEBUG = true;

    AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
    float rate = 8000.0f;
    int channels = 1;
    int sampleSize = 16;
    boolean bigEndian = true;
    AudioFormat format = null;

    TargetDataLine microphone;
    SourceDataLine speakers;
    DataLine.Info dataLineInfo;

    public boolean isTalking() {
        return isTalking;
    }

    public boolean isListening() {
        return isListening;
    }

    boolean isTalking = false;
    boolean isListening = false;

    Thread talkingThread = null;
    Thread listeningThread = null;

    int bytesUpload = 0;
    int bytesDownload = 0;

    int CHUNK_SIZE = 64;

    //DatagramSocket socket = null;
    InetAddress sendToIP;
    int sendToPort;

    public int getMyID() {
        return myID;
    }

    public void setMyID(int myID) {
        this.myID = myID;
    }

    int myID = 0;
    Networking nwConnection = null;

    public Audio(float rate, int channels, InetAddress sendToIP, int sendToPort, int myID) {

        this.rate = rate;
        this.channels = channels;

        this.sendToIP = sendToIP;
        this.sendToPort = sendToPort;
        this.myID = myID;
        System.out.println(sendToIP.toString());
        try {
            format = new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8) * channels, rate, bigEndian);

            microphone = AudioSystem.getTargetDataLine(format);

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);

            dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
            speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

        }
        catch (Exception ex) {
            format = null;
            ex.printStackTrace();
            Controller.showCriticalErrorAlert(ex.getMessage(), ex.toString());
        }

        nwConnection = new Networking(sendToIP, sendToPort);
    }

    public void Talk() {

        System.err.println("talk is called");
        if (isTalking) return;
        if (format == null) {
            Controller.showCriticalErrorAlert("format is null", "");
            return;
        }
        if (DEBUG) System.out.println("Talking initialized");
        if (talkingThread != null) return;
        if (DEBUG) System.out.println("Talking began");


        ByteArrayOutputStream out = new ByteArrayOutputStream();


        try {
            microphone.open(format);
            microphone.start();

        }
        catch (Exception ex) {
            System.out.println("Error: " + ex.toString());
            Controller.showCriticalErrorAlert(ex.getMessage(), ex.toString());
            isTalking = false;
            return;
        }

        talkingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int numbytesUpload = 0;

                byte[] data = new byte[CHUNK_SIZE];

                DatagramPacket sendPacket;

                while (isTalking) {
                    numbytesUpload = microphone.read(data, 0, CHUNK_SIZE);
                    bytesUpload += numbytesUpload;

                    VoicePacket packet = new VoicePacket(myID, VoicePacket.TYPE_VOICE, 0, data);
                    nwConnection.sendVoicePacket(packet);
                    //System.out.println("Talking!");
                }
            }
        });
        isTalking = true;
        talkingThread.start();

        if (DEBUG) System.out.println("Talking thread started");
    }
    public void StopTalking() {
        isTalking = false;
        try {
            talkingThread.join();
        }
        catch (Exception ex) {};
        talkingThread = null;
        microphone.close();
    }


    public void Listen() {
        if (format == null) return;
        if (DEBUG) System.out.println("Listening initialized");
        if (listeningThread != null) return;
        if (DEBUG) System.out.println("Listening began");

        try {

            speakers.open(format);
            isListening = true;
            speakers.start();
        }
        catch (Exception ex) {
            if (DEBUG) System.out.println(ex.toString());
            ex.printStackTrace();
            Controller.showCriticalErrorAlert(ex.getMessage(), ex.toString());
            isListening = false;
        }

        listeningThread = new Thread(new Runnable() {
            @Override
            public void run() {

                VoicePacket packet = new VoicePacket();

                while(isListening)
                {
                    try {
                        if (isListening == false) break;
                        if (nwConnection.receiveVoicePacketBlocking(packet) == 0) {
                            speakers.write(packet.getPayload(), 0, packet.getPayload().length);
                            bytesDownload +=  packet.getPayload().length + 3;

                            //if (DEBUG) System.out.println("Spk: " + packet.getPayload().length + 3);
                            //System.out.println("Listening!");
                        }
                    }
                    catch (Exception ex) {
                        System.err.println("Listening error: " + ex.toString());
                        ex.printStackTrace();
                    }
                }

            }
        });
        isListening = true;
        listeningThread.start();
    }
    public void StopListening() {

        isListening = false;
        try {

            listeningThread.join();
        }
        catch (Exception ex) {
            if (DEBUG) System.out.println(ex.toString());
        };

        listeningThread = null;
        speakers.drain();
        speakers.close();
        if (DEBUG) System.out.println("Listening stopped");
    }

    public float getVolumePercentage(byte[] data) {
        float[] samples = new float[data.length / 2];
        float lastPeak = 0f;

        // convert bytes to samples here
        for(int i = 0, s = 0; i < data.length;) {
            int sample = 0;

            sample |= data[i++] & 0xFF; // (reverse these two lines
            sample |= data[i++] << 8;   //  if the format is big endian)

            // normalize to range of +/-1.0f
            samples[s++] = sample / 32768f;
        }

        float rms = 0f;
        float peak = 0f;
        for(float sample : samples) {

            float abs = Math.abs(sample);
            if(abs > peak) {
                peak = abs;
            }

            rms += sample * sample;
        }

        rms = (float)Math.sqrt(rms / samples.length);
        //if (DEBUG) System.out.println(rms);
        return rms;
    }
}
