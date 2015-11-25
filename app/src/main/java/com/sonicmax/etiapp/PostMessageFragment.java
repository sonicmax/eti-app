package com.sonicmax.etiapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class PostMessageFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {

    private EditText mMessageBody;
    private TextView errorView;
    private Topic mTopic;
    private String mQuote;
    private ProgressDialog mDialog;

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

            Bundle args = new Bundle();
            args.putString("method", "POST");
            args.putString("type", "newmessage");
            args.putParcelable("values", values);

            getLoaderManager().initLoader(0, args, this).forceLoad();

        } else {
            errorView.setText(R.string.error_5_chars_or_more);
        }
    }

    /**
     *       Loader callbacks.
     */
    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {
        final Context context = getContext();

        mDialog = new ProgressDialog(getContext());
        mDialog.setMessage("Posting message...");
        mDialog.show();

        return new AsyncLoadHandler(context, args) {

            @Override
            public String loadInBackground() {
                return new WebRequest(context, args).sendRequest();
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data != null) {
            // Create new intent for MessageListActivity using Topic data
            Intent intent = new Intent(getContext(), MessageListActivity.class);
            intent.putExtra("topic", mTopic);
            intent.putExtra("title", mTopic.getTitle());
            intent.putExtra("last_page", true);
            getContext().startActivity(intent);
        }

        mDialog.dismiss();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }

}
