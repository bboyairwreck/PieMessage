package com.ericchee.bboyairwreck.piemessage;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by echee on 12/16/15.
 */
public class ChatsAdapter extends ArrayAdapter<Chat> {
    public static final String TAG = ChatsAdapter.class.getSimpleName();

    public ChatsAdapter(Context context, ArrayList<Chat> chats) {
        super(context, 0, chats);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "Hitting ChatsAdapter to insert chat");

        Chat chat = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat, parent, false);
        }

        TextView tvChatName = (TextView) convertView.findViewById(R.id.tvChatName);
        TextView tvLastMessage = (TextView) convertView.findViewById(R.id.tvLastMessage);

        String handlesString = chat.getHandlesString();

        String lastMessageText = chat.getLastText();

        tvChatName.setText(handlesString);
        tvLastMessage.setText(lastMessageText);

        return convertView;
    }


}
