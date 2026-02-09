package com.example.hfradar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class VerifyDataFragment extends Fragment {

    private View view;
    private Spinner spinnerMonth, spinnerYear;
    private Button btnFetchData;
    private TextView tvStatus;
    private LinearLayout llTableContainer;

    private static final String ADMIN_PASSWORD = "niot@321";
    private String[] months = {"January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};
    private String[] siteIds = {"Cuda", "Kalp", "Mach", "Yanm", "Wasi", "Jgri", "Gopa", "Puri", "Ptbl", "Htby"};

    private int selectedMonth;
    private int selectedYear;
    private int defaultMonth;
    private int defaultYear;
    private boolean isPasswordVerified = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_verify_data, container, false);

        initViews();
        setupSpinners();
        setupButtons();

        return view;
    }

    private void initViews() {
        spinnerMonth = view.findViewById(R.id.spinnerVerifyMonth);
        spinnerYear = view.findViewById(R.id.spinnerVerifyYear);
        btnFetchData = view.findViewById(R.id.btnFetchData);
        tvStatus = view.findViewById(R.id.tvVerifyStatus);
        llTableContainer = view.findViewById(R.id.llTableContainer);
    }

    private void setupSpinners() {
        // Month spinner
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        // Year spinner (last 5 years)
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear; i >= currentYear - 5; i--) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // Calculate previous month (default)
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH); // 0-indexed

        if (currentMonth == 0) {
            // January, so previous month is December of previous year
            defaultMonth = 12;
            defaultYear = currentYear - 1;
            spinnerMonth.setSelection(11); // December
            spinnerYear.setSelection(1); // Previous year
        } else {
            defaultMonth = currentMonth; // Previous month (1-indexed)
            defaultYear = currentYear;
            spinnerMonth.setSelection(currentMonth - 1);
            spinnerYear.setSelection(0);
        }

        selectedMonth = defaultMonth;
        selectedYear = defaultYear;

        // Month selection listener
        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int newMonth = position + 1;
                if (newMonth != selectedMonth) {
                    selectedMonth = newMonth;
                    checkIfPasswordRequired();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Year selection listener
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int newYear = Integer.parseInt(parent.getItemAtPosition(position).toString());
                if (newYear != selectedYear) {
                    selectedYear = newYear;
                    checkIfPasswordRequired();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void checkIfPasswordRequired() {
        // If user changed month/year from default, require password
        boolean isDefaultSelection = (selectedMonth == defaultMonth && selectedYear == defaultYear);

        if (!isDefaultSelection && !isPasswordVerified) {
            // Need password
            showPasswordDialog();
        }
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Authorization Required");
        builder.setMessage("Enter password to view data for this month:");

        final EditText input = new EditText(getActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.equals(ADMIN_PASSWORD)) {
                isPasswordVerified = true;
                Toast.makeText(getActivity(), "Access granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Incorrect password", Toast.LENGTH_SHORT).show();
                // Reset to default
                resetToDefault();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Reset to default
            resetToDefault();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void resetToDefault() {
        selectedMonth = defaultMonth;
        selectedYear = defaultYear;

        if (defaultMonth == 12) {
            spinnerMonth.setSelection(11);
            spinnerYear.setSelection(1);
        } else {
            spinnerMonth.setSelection(defaultMonth - 1);
            spinnerYear.setSelection(0);
        }
    }

    private void setupButtons() {
        btnFetchData.setOnClickListener(v -> fetchData());
    }

    private void fetchData() {
        // Check if non-default month requires password
        boolean isDefaultSelection = (selectedMonth == defaultMonth && selectedYear == defaultYear);
        if (!isDefaultSelection && !isPasswordVerified) {
            showPasswordDialog();
            return;
        }

        tvStatus.setText("Fetching data...");
        tvStatus.setVisibility(View.VISIBLE);
        llTableContainer.removeAllViews();

        // Get server URL
        SharedPreferences prefs = getActivity().getSharedPreferences("MyPrefs1", Context.MODE_PRIVATE);
        String baseUrl = prefs.getString("serv_url", "https://hfradarsite.pythonanywhere.com");
        String serverUrl = baseUrl + "/getalldatabymon";

        new FetchDataTask().execute(serverUrl);
    }

    private class FetchDataTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            btnFetchData.setEnabled(false);
            btnFetchData.setText("Loading...");
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                String serverUrl = params[0];

                // Build JSON request
                JSONObject request = new JSONObject();
                request.put("mon", String.valueOf(selectedMonth));
                request.put("yr", String.valueOf(selectedYear));

                HttpClientExample httpClient = new HttpClientExample();
                return httpClient.sendPost(serverUrl, request);

            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            btnFetchData.setEnabled(true);
            btnFetchData.setText("FETCH DATA");

            if (result == null || result.isEmpty()) {
                tvStatus.setText("Failed to fetch data");
                tvStatus.setTextColor(Color.RED);
                return;
            }

            if (result.startsWith("ERROR:")) {
                tvStatus.setText(result);
                tvStatus.setTextColor(Color.RED);
                return;
            }

            try {
                parseAndDisplayData(result);
                tvStatus.setText(months[selectedMonth - 1] + " " + selectedYear);
                tvStatus.setTextColor(Color.WHITE);
            } catch (Exception e) {
                tvStatus.setText("Error parsing data: " + e.getMessage());
                tvStatus.setTextColor(Color.RED);
            }
        }
    }

    private void parseAndDisplayData(String jsonResponse) throws Exception {
        JSONObject response = new JSONObject(jsonResponse);

        if (!response.has("rcnt")) {
            tvStatus.setText("No data available");
            return;
        }

        JSONArray rcnt = response.getJSONArray("rcnt");
        JSONArray idlist = response.getJSONArray("idlist");
        int numRows = Integer.parseInt(response.getString("hrows"));

        // Build table
        llTableContainer.removeAllViews();

        // Create TableLayout
        TableLayout table = new TableLayout(getActivity());
        table.setStretchAllColumns(false);

        // Header row
        TableRow headerRow = new TableRow(getActivity());
        headerRow.setBackgroundColor(Color.parseColor("#2196F3"));

        // Day column header
        TextView dayHeader = createTableCell("Day", true);
        headerRow.addView(dayHeader);

        // Site headers
        for (int i = 0; i < idlist.length(); i++) {
            TextView siteHeader = createTableCell(idlist.getString(i), true);
            headerRow.addView(siteHeader);
        }
        table.addView(headerRow);

        // Data rows
        for (int row = 0; row < rcnt.length(); row++) {
            TableRow dataRow = new TableRow(getActivity());

            // Alternate row colors
            if (row == rcnt.length() - 1) {
                // Last row is percentage - special color
                dataRow.setBackgroundColor(Color.parseColor("#4CAF50"));
            } else if (row % 2 == 0) {
                dataRow.setBackgroundColor(Color.parseColor("#E3F2FD"));
            } else {
                dataRow.setBackgroundColor(Color.WHITE);
            }

            // Day number or "%" for last row
            String dayLabel;
            if (row == rcnt.length() - 1) {
                dayLabel = "%";
            } else {
                dayLabel = String.valueOf(row + 1);
            }
            TextView dayCell = createTableCell(dayLabel, row == rcnt.length() - 1);
            headerRow.setBackgroundColor(Color.parseColor("#2196F3"));
            dataRow.addView(dayCell);

            // Site data
            JSONArray rowData = rcnt.getJSONArray(row);
            for (int col = 0; col < rowData.length(); col++) {
                String value = rowData.getString(col);
                TextView dataCell = createTableCell(value, false);

                // Color code percentage row
                if (row == rcnt.length() - 1) {
                    try {
                        int perc = Integer.parseInt(value);
                        if (perc >= 70) {
                            dataCell.setBackgroundColor(Color.parseColor("#80FF00")); // Green
                        } else if (perc >= 50) {
                            dataCell.setBackgroundColor(Color.parseColor("#FFFF66")); // Yellow
                        } else {
                            dataCell.setBackgroundColor(Color.parseColor("#FF9933")); // Orange
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }

                dataRow.addView(dataCell);
            }

            table.addView(dataRow);
        }

        llTableContainer.addView(table);
    }

    private TextView createTableCell(String text, boolean isHeader) {
        TextView tv = new TextView(getActivity());
        tv.setText(text);
        tv.setPadding(12, 8, 12, 8);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(11); // Small text
        tv.setMinWidth(45);

        if (isHeader) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.WHITE);
        } else {
            tv.setTextColor(Color.BLACK);
        }

        return tv;
    }
}
