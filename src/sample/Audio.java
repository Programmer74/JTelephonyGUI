package sample;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

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

    private boolean DEBUG = true;

    private AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

    private int channels = 1;
    private boolean bigEndian = true;

    private AudioFormat format_spk = null;
    private AudioFormat format_mic = null;

    private TargetDataLine microphone;
    private SourceDataLine speakers;

    public boolean isTalking() {
        return isTalking;
    }

    public boolean isListening() {
        return isListening;
    }

    private boolean isTalking = false;
    private boolean isListening = false;

    private Thread talkingThread = null;
    private Thread listeningThread = null;

    private int bytesUpload = 0;
    private int bytesDownload = 0;

    public int getMicCapturedDataSize() {
        return micCapturedDataSize;
    }

    public void setMicCapturedDataSize(int micCapturedDataSize) {
        this.micCapturedDataSize = micCapturedDataSize;
    }

    private int micCapturedDataSize = 180;

    private InetAddress sendToIP;
    private int sendToPort;

    public int getMyID() {
        return myID;
    }

    public void setMyID(int myID) {
        this.myID = myID;
    }

    private int myID = 0;

    public Networking getNwConnection() {
        return nwConnection;
    }

    private Networking nwConnection = null;

    /*
        Quality setups:
        0 - 8kHz, 8 bit
        1 - 8kHz, 16 bit
        2 - 16kHz, 16 bit
        3 - 16kHz, 24 bit
        4 - 44.1kHz, 16 bit
        5 - 44.1kHz, 24 bit
    */
    private float getRateByQuality(int quality) {
        switch (quality) {
            case 0:
            case 1:
                return 8000.0f;
            case 2:
            case 3:
                return 16000.0f;
            case 4:
            case 5:
                return 44100.0f;
        }
        return 16000.0f;
    }
    private int getSSizeByQuality(int quality) {
        switch (quality) {
            case 0:
                return 8;
            case 1:
            case 2:
            case 4:
                return 16;
            case 3:
            case 5:
                return 24;
        }
        return 16;
    }

    private int myQualitySetupForSpeakers = 5;
    private int myQualitySetupForMic = 5;

    private int newQualitySetupForSpeakers = -1;
    private int newQualitySetupForMic = -1;

    public int getMyQualitySetupForSpeakers() {
        return myQualitySetupForSpeakers;
    }

    public void setMyQualitySetupForSpeakers(int myQualitySetupForSpeakers) {
        this.myQualitySetupForSpeakers = myQualitySetupForSpeakers;
        updateSpeakerParamsByQuality(myQualitySetupForSpeakers);
    }

    public int getMyQualitySetupForMic() {
        return myQualitySetupForMic;
    }

    public void setMyQualitySetupForMic(int myQualitySetupForMic) {
        this.myQualitySetupForMic = myQualitySetupForMic;
        updateMicrophoneParamsByQuality(myQualitySetupForMic);
    }

    private void updateSpeakerParams(float rate_spk, int sampleSize_spk) {
        boolean wasListening = isListening();
        if (isListening()) StopListening();

        try {
            format_spk = new AudioFormat(encoding, rate_spk, sampleSize_spk, channels, (sampleSize_spk / 8) * channels, rate_spk, bigEndian);

            DataLine.Info info_spk = new DataLine.Info(SourceDataLine.class, format_spk);
            speakers = (SourceDataLine) AudioSystem.getLine(info_spk);

        }
        catch (Exception ex) {
            format_spk = null;
            System.out.println("UpdateSpeakerParams: " + ex.toString());
            ex.printStackTrace();
            MessageBoxes.showCriticalErrorAlert(ex.getMessage(), ex.toString());
        }
        if (wasListening) Listen();
    }
    private void updateSpeakerParamsByQuality(int quality) {
        float rate_spk = getRateByQuality(quality);
        int ssize = getSSizeByQuality(quality);
        updateSpeakerParams(rate_spk, ssize);
        System.out.println("Updated Speaker quality to " + quality);
    }

    private void updateMicrophoneParams(float rate_mic, int sampleSize_mic) {
        boolean wasTalking = isTalking();
        if (isTalking()) StopTalking();

        try {
            format_mic = new AudioFormat(encoding, rate_mic, sampleSize_mic, channels, (sampleSize_mic / 8) * channels, rate_mic, bigEndian);

            DataLine.Info info_mic = new DataLine.Info(TargetDataLine.class, format_mic);
            microphone = (TargetDataLine) AudioSystem.getLine(info_mic);
        }
        catch (Exception ex) {
            format_mic = null;
            System.out.println("UpdateMicrophoneParams: " + ex.toString());
            ex.printStackTrace();
            MessageBoxes.showCriticalErrorAlert(ex.getMessage(), ex.toString());
        }
        if (wasTalking) Talk();
    }
    private void updateMicrophoneParamsByQuality(int quality) {
        float rate_mic = getRateByQuality(quality);
        int ssize = getSSizeByQuality(quality);
        updateMicrophoneParams(rate_mic, ssize);
        System.out.println("Updated Mic quality to " + quality);
    }

    public Audio(int qualitySetup, InetAddress sendToIP, int sendToPort, int myID) {

        this.sendToIP = sendToIP;
        this.sendToPort = sendToPort;
        this.myID = myID;
        System.out.println(sendToIP.toString());

        this.myQualitySetupForSpeakers = qualitySetup;
        this.myQualitySetupForMic = qualitySetup;
        updateSpeakerParamsByQuality(myQualitySetupForSpeakers);
        updateMicrophoneParamsByQuality(myQualitySetupForMic);

        nwConnection = new Networking(sendToIP, sendToPort);

        Thread spkQualityThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (newQualitySetupForSpeakers != -1) {
                        if (newQualitySetupForSpeakers != myQualitySetupForSpeakers) {
                            updateSpeakerParamsByQuality(newQualitySetupForSpeakers);
                            myQualitySetupForSpeakers = newQualitySetupForSpeakers;
                            newQualitySetupForSpeakers = -1;
                        }
                    }
                    Thread.yield();
                }
            }
        });
        spkQualityThread.start();
    }

    public void Talk() {

        System.err.println("talk is called");
        if (isTalking) return;
        if (format_mic == null) {
            MessageBoxes.showCriticalErrorAlert("format_mic is null", "");
            return;
        }
        if (DEBUG) System.out.println("Talking initialized");
        if (talkingThread != null) return;
        if (DEBUG) System.out.println("Talking began");


        ByteArrayOutputStream out = new ByteArrayOutputStream();


        try {
            microphone.open(format_mic);
            microphone.start();

        }
        catch (Exception ex) {
            System.out.println("Error: " + ex.toString());
            MessageBoxes.showCriticalErrorAlert(ex.getMessage(), ex.toString());
            isTalking = false;
            return;
        }

        talkingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int numbytesUpload = 0;

                byte[] data = new byte[micCapturedDataSize];

                DatagramPacket sendPacket;

                while (isTalking) {
                    numbytesUpload = microphone.read(data, 0, micCapturedDataSize);
                    bytesUpload += numbytesUpload;

                    VoicePacket packet = new VoicePacket(myID, VoicePacket.TYPE_VOICE, myQualitySetupForMic, data);
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
        if (isTalking == false) return;
        isTalking = false;
        try {
            talkingThread.join();
        }
        catch (Exception ex) {};
        talkingThread = null;
        microphone.close();
    }


    public void Listen() {
        if (format_spk == null) return;
        if (DEBUG) System.out.println("Listening initialized");
        if (listeningThread != null) return;
        if (DEBUG) System.out.println("Listening began");

        try {

            speakers.open(format_spk);
            isListening = true;
            speakers.start();
        }
        catch (Exception ex) {
            if (DEBUG) {
                System.out.println("Listen: " + ex.toString());
            }
            ex.printStackTrace();
            MessageBoxes.showCriticalErrorAlert(ex.getMessage(), ex.toString());
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
                            if (packet.getmArg() != myQualitySetupForSpeakers) {
                                if (newQualitySetupForSpeakers == -1) {
                                    System.out.println("Mismatch found!");
                                    newQualitySetupForSpeakers = packet.getmArg();
                                }
                                continue;
                            }
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
        if (isListening == false) return;
        isListening = false;
        try {
            listeningThread.join();
        }
        catch (Exception ex) {
            if (DEBUG) System.out.println("StopListening: " + ex.toString());
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
            sample |= data[i++] << 8;   //  if the format_spk is big endian)

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
