package com.sonicmax.etiapp.fragments;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.loaders.AccountManager;
import com.sonicmax.etiapp.utilities.SharedPreferenceManager;

public class LoginFragment extends Fragment implements AccountManager.EventInterface {
    private final String LOG_TAG = LoginFragment.class.getSimpleName();

    private EditText mUsername;
    private EditText mPassword;
    private ProgressDialog mDialog;
    private AccountManager mAccountManager;

    ///////////////////////////////////////////////////////////////////////////
    // Fragment methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountManager = new AccountManager(getContext(), this);

        boolean reuseSessionToken = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean("pref_reuse_session", false);

        if (reuseSessionToken) {
            mAccountManager.checkLoginStatus();
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

    @Override
    public void onDetach() {
        // Make sure that we don't leak progress dialog when exiting activity
        dismissDialog();
        super.onDetach();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
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

        mDialog = new ProgressDialog(context);
        mDialog.setMessage("Logging in...");
        mDialog.show();

        ContentValues values = new ContentValues(2);
        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();
        values.put("username", username);
        values.put("password", password);

        Bundle args = new Bundle(3);
        args.putString("method", "POST");
        args.putString("type", "login");
        args.putParcelable("values", values);

        // Store credentials for later use
        SharedPreferenceManager.putString(context, "username", username);
        SharedPreferenceManager.putString(context, "password", password);

        mAccountManager.login(args);
    }

    @Override
    public void onLoadComplete(Intent intent) {
        dismissDialog();
        getContext().startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
    }

    @Override
    public void onRequiresLogin() {
    }
}
