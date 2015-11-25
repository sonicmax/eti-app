package com.sonicmax.etiapp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

    private EditText mMessageBody;
    private TextView errorView;
    private Topic mTopic;
    private String mQuote;

    public PostMessageFragment() {}

    @Override
    public void onAttach(Context context) {

        Intent intent = ((Activity) context).getIntent();
        this.mTopic = intent.getParcelableExtra("topic");
        this.mQuote = intent.getStringExtra("quote");
        super.onAttach(context);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_new_message, container, false);
        mMessageBody = (EditText) rootView.findViewById(R.id.new_message);
        Button postMessage = (Button) rootView.findViewById(R.id.post_new_message);
        errorView = (TextView) rootView.findViewById(R.id.error_view);

        if (mQuote != null) {
            // TODO: Display quoted message using MessageBuilder unless user requests to edit quote
            mMessageBody.setText(mQuote);
            // Move cursor to end of quoted text
            mMessageBody.setSelection(mMessageBody.getText().length());
        }

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
        String message = mMessageBody.getText().toString();

        if (message.length() >= 5) {

            String token = SharedPreferenceManager.getString(getContext(), "h");
            String signature = SharedPreferenceManager.getString(getContext(), "signature");

            // Get input from editText elements
            ContentValues values = new ContentValues();
            values.put("id", getActivity().getIntent().getStringExtra("id"));
            values.put("title", getActivity().getIntent().getStringExtra("title"));
            values.put("message", message + NEWLINE + signature);
            values.put("lastpage", getActivity().getIntent().getIntExtra("lastpage", 1));
            values.put("h", token);
            values.put("submit", "Post Message");

            new PostMessageHandler(getContext(), mTopic).submitMessage(values);

        } else {
            errorView.setText(R.string.error_5_chars_or_more);
        }

    }
}
