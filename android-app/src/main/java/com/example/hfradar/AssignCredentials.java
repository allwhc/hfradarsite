package com.example.hfradar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class AssignCredentials extends Fragment {
    EditText txtPassword, edtUrl;
    Button btnLogin, btnUnlockUrl, btnUpdateSites;
    TextView tvSiteListStatus;
    SwitchCompat switchReminders;
    String password = "", surl = "";
    public static final String MyPREFERENCES = "MyPrefs1";
    public static final String PREF_SITES_LIST = "sites_list";
    private static final String ADMIN_PASSWORD = "niot@321";
    SharedPreferences sharedpreferences;
    View view;
    private boolean urlUnlocked = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.assign_cred, container, false);
        sharedpreferences = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        String serverurl = sharedpreferences.getString("serv_url", "https://hfradarsite.pythonanywhere.com");

        txtPassword = view.findViewById(R.id.txtPassword);
        edtUrl = view.findViewById(R.id.edtUrl);
        btnUnlockUrl = view.findViewById(R.id.btnUnlockUrl);
        btnUpdateSites = view.findViewById(R.id.btnUpdateSites);
        tvSiteListStatus = view.findViewById(R.id.tvSiteListStatus);

        edtUrl.setText(serverurl);
        edtUrl.setEnabled(false); // Start locked

        // Show current sites list
        updateSiteListStatus();

        txtPassword.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                Validation.hasText(txtPassword);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // Unlock URL button - requires admin password
        btnUnlockUrl.setOnClickListener(v -> showUnlockPasswordDialog());

        // Update Sites button
        btnUpdateSites.setOnClickListener(v -> fetchSitesFromServer());

        // Login button
        btnLogin = view.findViewById(R.id.btnLogin);

        // Monthly reminder toggle
        switchReminders = view.findViewById(R.id.switchReminders);
        switchReminders.setChecked(MonthlyReminderWorker.isEnabled(getActivity()));
        switchReminders.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MonthlyReminderWorker.setEnabled(getActivity(), isChecked);
                if (isChecked) {
                    MonthlyReminderWorker.schedule(getActivity());
                    Toast.makeText(getActivity(), "Monthly reminders enabled", Toast.LENGTH_SHORT).show();
                } else {
                    MonthlyReminderWorker.cancel(getActivity());
                    Toast.makeText(getActivity(), "Monthly reminders disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Save button click event
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (checkValidation()) {
                    txtPassword = view.findViewById(R.id.txtPassword);
                    password = txtPassword.getText().toString();
                    edtUrl = view.findViewById(R.id.edtUrl);
                    surl = edtUrl.getText().toString();

                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("app_pwd", password);
                    editor.putString("serv_url", surl);
                    editor.apply();

                    // Lock URL again after saving
                    lockUrl();

                    Toast.makeText(getActivity().getApplicationContext(), "Configuration saved successfully!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }

    private void showUnlockPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Admin Authorization");
        builder.setMessage("Enter admin password to edit server URL:");

        final EditText input = new EditText(getActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Unlock", (dialog, which) -> {
            String enteredPassword = input.getText().toString();
            if (enteredPassword.equals(ADMIN_PASSWORD)) {
                unlockUrl();
                Toast.makeText(getActivity(), "URL editing unlocked", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void unlockUrl() {
        urlUnlocked = true;
        edtUrl.setEnabled(true);
        btnUnlockUrl.setText("🔓");
    }

    private void lockUrl() {
        urlUnlocked = false;
        edtUrl.setEnabled(false);
        btnUnlockUrl.setText("🔒");
    }

    private void updateSiteListStatus() {
        String sitesList = sharedpreferences.getString(PREF_SITES_LIST, "");
        if (sitesList.isEmpty()) {
            tvSiteListStatus.setText("Sites: Using default list");
        } else {
            // Count sites
            String[] sites = sitesList.split(",");
            tvSiteListStatus.setText("Sites: " + sites.length + " sites loaded (" + sitesList + ")");
        }
    }

    private void fetchSitesFromServer() {
        String serverUrl = edtUrl.getText().toString();
        if (serverUrl.isEmpty()) {
            Toast.makeText(getActivity(), "Server URL is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpdateSites.setEnabled(false);
        btnUpdateSites.setText("Fetching...");
        tvSiteListStatus.setText("Connecting to server...");

        new FetchSitesTask().execute(serverUrl + "/getConfig");
    }

    private class FetchSitesTask extends AsyncTask<String, Void, String> {
        private String errorMessage = "";

        @Override
        protected String doInBackground(String... params) {
            try {
                String serverUrl = params[0];
                HttpClientExample httpClient = new HttpClientExample();
                // Send empty JSON object for POST request
                JSONObject requestBody = new JSONObject();
                return httpClient.sendPost(serverUrl, requestBody);
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            btnUpdateSites.setEnabled(true);
            btnUpdateSites.setText("Update Site List from Server");

            if (result != null && !result.isEmpty()) {
                try {
                    JSONObject response = new JSONObject(result);
                    if (response.has("sites")) {
                        JSONArray sitesArray = response.getJSONArray("sites");
                        StringBuilder sitesList = new StringBuilder();
                        for (int i = 0; i < sitesArray.length(); i++) {
                            if (i > 0) sitesList.append(",");
                            sitesList.append(sitesArray.getString(i));
                        }

                        // Save to SharedPreferences
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(PREF_SITES_LIST, sitesList.toString());
                        editor.apply();

                        tvSiteListStatus.setText("Sites updated: " + sitesArray.length() + " sites (" + sitesList + ")");
                        Toast.makeText(getActivity(), "Site list updated successfully! " + sitesArray.length() + " sites loaded.", Toast.LENGTH_LONG).show();
                    } else {
                        tvSiteListStatus.setText("Error: No sites in response");
                        Toast.makeText(getActivity(), "Invalid response from server", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    tvSiteListStatus.setText("Error parsing response");
                    Toast.makeText(getActivity(), "Error parsing server response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                tvSiteListStatus.setText("Failed to connect to server");
                Toast.makeText(getActivity(), "Failed to fetch sites. " + errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onBackPressed() {
        //do nothing
    }

    private boolean checkValidation() {
        boolean ret = true;

        if (!Validation.hasText(txtPassword)) {
            Toast.makeText(getActivity().getApplicationContext(), "Please enter password!",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return ret;
    }
}
