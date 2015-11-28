package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.sonicmax.etiapp.network.WebRequest;

public class PostTopicFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {

    private EditText mTopicTitle;
    private EditText mMessageBody;
    private ProgressDialog mDialog;

    public PostTopicFragment() {}

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

    private View.OnClickListener newTopicHandler = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.post_new_topic:
                    postNewTopic();
                    break;
            }

        }
    };

    private void postNewTopic() {

        final String NEWLINE = "\n";

        // Make sure that total message length >= 5 characters (otherwise POST will be unsuccessful)
        String signature = SharedPreferenceManager.getString(getContext(), "signature");
        String message = mMessageBody.getText().toString() + NEWLINE + signature;

        if (message.length() >= 5) {

            String token = SharedPreferenceManager.getString(getContext(), "h");

            // Get input from editText elements
            ContentValues values = new ContentValues();
            values.put("title", mTopicTitle.getText().toString());
            values.put("tag", "");
            values.put("h", token);
            values.put("message", message);
            values.put("submit", "Post Message");

            // Create bundle for loader
            Bundle args = new Bundle();
            args.putString("method", "POST");
            args.putString("type", "newtopic");
            args.putParcelable("values", values);

            getLoaderManager().initLoader(0, args, this).forceLoad();
        }

        else {
            // TODO: Display error message
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Loader callbacks
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public Loader<Object> onCreateLoader(int id, final Bundle args) {

        final Context context = getContext();

        mDialog = new ProgressDialog(context);
        mDialog.setMessage("Posting topic...");
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
        // Redirect user to topic that was just created
        mDialog.dismiss();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }


}
