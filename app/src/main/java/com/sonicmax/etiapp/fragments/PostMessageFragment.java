package com.sonicmax.etiapp.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
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

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.activities.MessageListActivity;
import com.sonicmax.etiapp.loaders.PostHandler;
import com.sonicmax.etiapp.objects.Topic;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class PostMessageFragment extends Fragment implements PostHandler.EventInterface {
    private EditText mMessageBody;
    private TextView errorView;
    private Topic mTopic;
    private String mQuote;
    private ProgressDialog mDialog;
    private PostHandler mPostHandler;

    public PostMessageFragment() {}

    @Override
    public void onAttach(Context context) {
        mPostHandler = new PostHandler(context, this);
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

    ///////////////////////////////////////////////////////////////////////////
    // PostHandler.EventInterface methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onPostComplete() {
        dismissDialog();
        // Create new intent for MessageListActivity using Topic data
        Intent intent = new Intent(getContext(), MessageListActivity.class);
        intent.putExtra("topic", mTopic);
        intent.putExtra("title", mTopic.getTitle());
        intent.putExtra("last_page", true);
        getContext().startActivity(intent);
    }

    @Override
    public void onPostFail() {
        dismissDialog();
        // Lol error handling
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    private void makeBundleAndPostMessage() {
        final String NEWLINE = "\n";

        // Make sure that total message length >= 5 characters (otherwise POST will be unsuccessful)
        String signature = SharedPreferenceManager.getString(getContext(), "signature");
        String message = mMessageBody.getText().toString() + NEWLINE + signature;

        if (message.length() >= 5) {
            String token = SharedPreferenceManager.getString(getContext(), "h");

            String urlEncodedMessage = getUrlEncodedString(message);

            ContentValues values = new ContentValues();
            values.put("id", getActivity().getIntent().getStringExtra("id"));
            values.put("title", getActivity().getIntent().getStringExtra("title"));
            values.put("message", urlEncodedMessage);
            values.put("lastpage", getActivity().getIntent().getIntExtra("lastpage", 1));
            values.put("h", token);
            values.put("submit", "Post Message");

            Bundle args = new Bundle();
            args.putString("method", "POST");
            args.putString("type", "newmessage");
            args.putParcelable("values", values);

            mPostHandler.postMessage(args);

        } else {
            errorView.setText(R.string.error_5_chars_or_more);
        }
    }


    private String getUrlEncodedString(String content) {
        final String UTF_8 = "UTF-8";
        try {
            return URLEncoder.encode(content, UTF_8);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Input listeners
    ///////////////////////////////////////////////////////////////////////////

    private View.OnClickListener newMessageHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.post_new_message:
                    mDialog = new ProgressDialog(getContext());
                    mDialog.setMessage("Posting message...");
                    mDialog.show();
                    makeBundleAndPostMessage();
                    break;
            }
        }
    };
}
