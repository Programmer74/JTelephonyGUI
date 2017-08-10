package sample;

public class VoicePacket {

    public static int TYPE_VOICE = 0;

    public VoicePacket(int myID, int msgType, int mArg, byte[] payload) {
        this.myID = myID;
        this.msgType = msgType;
        this.mArg = mArg;
        this.payload = payload;
    }
    public VoicePacket() {
        this.myID = 0;
        this.msgType = 0;
        this.mArg = 0;
        this.payload = null;
    }

    public int getMyID() {
        return myID;
    }

    public void setMyID(int myID) {
        this.myID = myID;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public int getmArg() {
        return mArg;
    }

    public void setmArg(int mArg) {
        this.mArg = mArg;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    private int myID;
    private int msgType;
    private int mArg;
    private byte[] payload;

    //public VoicePacket
}
