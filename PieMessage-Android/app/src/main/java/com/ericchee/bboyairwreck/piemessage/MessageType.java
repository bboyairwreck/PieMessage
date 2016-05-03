package com.ericchee.bboyairwreck.piemessage;

/**
 * Created by eric on 11/13/15.
 */
public enum MessageType {
    SENT (0),
    RECEIVED (1);

    private int messageNum;

    MessageType(int messageNum) {
        this.messageNum = messageNum;
    }

    public int getVal() {
        return this.messageNum;
    }
}

