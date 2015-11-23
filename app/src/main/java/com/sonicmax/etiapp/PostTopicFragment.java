package com.sonicmax.etiapp;

import android.content.ContentValues;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class PostTopicFragment extends Fragment {

    private EditText topicTitle;
    private EditText messageBody;

    public PostTopicFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_new_topic, container, false);
        topicTitle = (EditText) rootView.findViewById(R.id.new_topic_title);
        messageBody = (EditText) rootView.findViewById(R.id.new_topic_message);
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

        String token = SharedPreferenceManager.getString(getContext(), "h");
        // Get input from editText elements
        ContentValues values = new ContentValues();
        values.put("title", topicTitle.getText().toString());
        values.put("tag", "");
        values.put("h", token);
        values.put("message", messageBody.getText().toString());
        values.put("submit", "Post Message");

        new PostTopicHandler(getContext(), values).submitTopic();

    }


}
