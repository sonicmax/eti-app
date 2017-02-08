package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.loaders.PostHandler;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class PostTopicFragment extends Fragment implements PostHandler.EventInterface {

    private PostHandler mPostHandler;
    private EditText mTopicTitle;
    private EditText mMessageBody;
    private ProgressDialog mDialog;

    public PostTopicFragment() {}

    @Override
    public void onAttach(Context context) {
        mPostHandler = new PostHandler(context, this);
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_new_topic, container, false);
        mTopicTitle = (EditText) rootView.findViewById(R.id.new_topic_title);
        mMessageBody = (EditText) rootView.findViewById(R.id.new_topic_message);
        Button postMessage = (Button) rootView.findViewById(R.id.post_new_topic);
        postMessage.setOnClickListener(newTopicHandler);
        return rootView;
    }

    ///////////////////////////////////////////////////////////////////////////
    // PostHandler.EventInterface callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onPostComplete() {
        dismissDialog();
        // TODO: Redirect user to topic that was just created
    }

    @Override
    public void onPostFail() {
        dismissDialog();
        // TODO: Error handling
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    private void makeBundleAndPostTopic() {

        final String NEWLINE = "\n";

        // Make sure that total message length >= 5 characters (otherwise POST will be unsuccessful)
        String signature = SharedPreferenceManager.getString(getContext(), "signature");
        String message = mMessageBody.getText().toString() + NEWLINE + signature;

        if (message.length() >= 5) {

            String token = SharedPreferenceManager.getString(getContext(), "h");

            String urlEncodedTitle = getUrlEncodedString(mTopicTitle.getText().toString());
            String urlEncodedMessage = getUrlEncodedString(message);

            ContentValues values = new ContentValues();
            values.put("title", urlEncodedTitle);
            values.put("tag", "");
            values.put("h", token);
            values.put("message", urlEncodedMessage);
            values.put("submit", "Post Message");

            // Create bundle for loader
            Bundle args = new Bundle();
            args.putString("method", "POST");
            args.putString("type", "newtopic");
            args.putParcelable("values", values);

            mPostHandler.postTopic(args);
        }

        else {
            // TODO: Display error message
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

    private View.OnClickListener newTopicHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.post_new_topic:
                    mDialog = new ProgressDialog(getContext());
                    mDialog.setMessage("Posting topic...");
                    mDialog.show();
                    makeBundleAndPostTopic();
                    break;
            }

        }
    };
}
