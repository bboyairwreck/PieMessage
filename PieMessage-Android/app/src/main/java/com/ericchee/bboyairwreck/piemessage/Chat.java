package com.ericchee.bboyairwreck.piemessage;

import java.util.HashSet;

/**
 * Created by echee on 12/16/15.
 */
public class Chat {
    HashSet<String> handles;
    long cROWID;
    long date;
    String lastText;

    public Chat(HashSet<String> handles, long cROWID) {
        this.handles = handles;
        this.cROWID = cROWID;
        this.lastText = "...";
    }

    public Chat(HashSet<String> handles, long cROWID, long date) {
        this.handles = handles;
        this.cROWID = cROWID;
        this.date = date;
        this.lastText = "...";
    }

    public Chat() {
        this.handles = new HashSet<>();
        this.cROWID = -1;
        this.date = -1;
        this.lastText = "...";
    }

    public String getHandlesString() {
        String handlesString = "";
        int i = 0;
        for (String handle : this.handles) {
            handlesString += handle;
            if (i < this.handles.size() -1) {
                handlesString += ", ";
            }
            i++;
        }

        return handlesString;
    }

    public String getLastText() {
        return lastText;
    }
}
