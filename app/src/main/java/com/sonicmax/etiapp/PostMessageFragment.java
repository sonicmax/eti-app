package com.sonicmax.etiapp;

import android.content.ContentValues;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class PostMessageFragment extends Fragment {

    private EditText messageBody;
    private TextView errorView;

    public PostMessageFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_new_message, container, false);
        messageBody = (EditText) rootView.findViewById(R.id.new_message);
        String quote = getActivity().getIntent().getStringExtra("quote");
        if (quote != null) {
            messageBody.setText(quote);
            // Move cursor to end of quoted text
            messageBody.setSelection(messageBody.getText().length());
        }
        Button postMessage = (Button) rootView.findViewById(R.id.post_new_message);
        errorView = (TextView) rootView.findViewById(R.id.error_view);
        postMessage.setOnClickListener(newMessageHandler);
        return rootView;
    }

    private View.OnClickListener newMessageHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.post_new_message:
                    postNewMessage();
                    break;
            }

        }
    };

    private void postNewMessage() {

        final String NEWLINE = "\n";

        // Make sure that message length >= 5 characters (otherwise POST will be unsuccessful)
        String message = messageBody.getText().toString();

        if (!message.equals("") && message.length() >= 5) {

            String token = SharedPreferenceManager.getString(getContext(), "h");
            String signature = SharedPreferenceManager.getString(getContext(), "signature");

            // Get input from editText elements
            ContentValues values = new ContentValues();
            values.put("id", getActivity().getIntent().getStringExtra("id"));
            values.put("title", getActivity().getIntent().getStringExtra("title"));
            values.put("message", message + NEWLINE + "---" + NEWLINE + signature );
            values.put("lastpage", getActivity().getIntent().getIntExtra("lastpage", 1));
            values.put("h", token);
            values.put("submit", "Post Message");

            new PostMessageHandler(getContext()).submitMessage(values);

        } else {

            errorView.setText("Your message must be at least 5 characters");
        }

    }
}
