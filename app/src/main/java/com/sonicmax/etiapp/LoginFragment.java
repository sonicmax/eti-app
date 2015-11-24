package com.sonicmax.etiapp;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class LoginFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {

    private final String LOG_TAG = LoginFragment.class.getSimpleName();
    private final int SCRIPT_BUILD = 0;
    private final int STATUS_CHECK = 1;
    private final int LOGIN = 2;
    private int mCurrentId;
    private EditText mUsername;
    private EditText mPassword;
    private ProgressDialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Check whether "is_logged_in" flag has been set, so we can use stored cookies
        if (SharedPreferenceManager.getBoolean(getContext(), "is_logged_in")) {

            // Make sure that user is still logged in
            // (ie. hasn't logged in using a different IP address)
            getLoaderManager().initLoader(SCRIPT_BUILD, null, this).forceLoad();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_login, container, false);
        mUsername = (EditText) rootView.findViewById(R.id.login_username);
        mPassword = (EditText) rootView.findViewById(R.id.login_password);
        Button loginButton = (Button) rootView.findViewById(R.id.login_button);

        // Insert username and password from SharedPreferences (if they exist)
        String username = SharedPreferenceManager.getString(getActivity(), "username");
        String password = SharedPreferenceManager.getString(getActivity(), "password");
        if (username != null & password != null) {
            mUsername.setText(username);
            mPassword.setText(password);
        }

        loginButton.setOnClickListener(loginHandler);

        return rootView;

    }

    private View.OnClickListener loginHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.login_button:
                    makeLoginRequest();
                    break;
            }
        }
    };

    private void makeLoginRequest() {

        Context context = getContext();

        ContentValues values = new ContentValues();
        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();
        values.put("username", username);
        values.put("password", password);

        Bundle args = new Bundle();
        args.putString("method", "POST");
        args.putString("type", "login");
        args.putParcelable("values", values);

        // Store credentials for later use
        SharedPreferenceManager.putString(context, "username", username);
        SharedPreferenceManager.putString(context, "password", password);

        getLoaderManager().initLoader(LOGIN, args, this).forceLoad();
    }

    @Override
    public void onDetach() {

        // Make sure that we don't leak progress dialog when exiting activity
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }

        super.onDetach();
    }

    /**
     * Loader callbacks.
     */
    public Loader<Object> onCreateLoader(int id, final Bundle args) {

        final Context context = getContext();
        mCurrentId = id;

        switch (id) {
            // TODO: Perform SCRIPT_BUILD and STATUS_CHECK in same AsyncLoadHandler
            case SCRIPT_BUILD:
                return new LoginScriptBuilder(context);

            // Allow STATUS_CHECK case to fallthrough
            case STATUS_CHECK:
            case LOGIN:

                mDialog = new ProgressDialog(context);
                mDialog.setMessage(getDialogMessage(id));
                mDialog.show();

                return new AsyncLoadHandler(context, args) {
                    @Override
                    public String loadInBackground() {
                        return new WebRequest(context, args).sendRequest();
                    }
                };

            default:
                Log.e(LOG_TAG, "Couldn't find loader with id " + id);
                return null;
        }
    }

    public void onLoadFinished(Loader<Object> loader, Object data) {

        String response = (String) data;

        Context context = getContext();
        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mCurrentId == SCRIPT_BUILD) {

            Log.v(LOG_TAG, "received script build response");
            Bundle args = new Bundle();
            args.putString("method", "GET");
            args.putString("type", "url");
            args.putString("url", response);

            getLoaderManager().initLoader(STATUS_CHECK, args, this).forceLoad();
        }

        else if (mCurrentId == STATUS_CHECK) {

            Log.v(LOG_TAG, "status check response = " + data);

            // Possible responses:
            // "0"              Not logged in
            // "1:username"     Logged in

            if (response == null || response.trim().equals("0")) {
                // Wait for user to login - do nothing. Handle null responses here as well
                mDialog.dismiss();
            }

            else {
                // Use stored cookies to get board list and start activity
                mDialog.dismiss();
                Intent intent = new Intent(context, BoardListActivity.class);
                intent.putExtra("title", "ETI");
                context.startActivity(intent);
            }
        }

        else if (mCurrentId == LOGIN) {

            // Get list of bookmarks from main.php & start BoardListActivity
            SharedPreferenceManager.putBoolean(context, "is_logged_in", true);
            mDialog.dismiss();
            Intent intent = new Intent(context, BoardListActivity.class);
            context.startActivity(intent);
        }
    }

    public void onLoaderReset(Loader<Object> loader) {
        loader.reset();
    }

    private String getDialogMessage(int id) {
        return (id == STATUS_CHECK) ? "Checking status..." : "Logging in...";
    }
}
