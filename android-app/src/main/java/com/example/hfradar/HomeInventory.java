package com.example.hfradar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeInventory extends Fragment {

    private View view;
    private Spinner spinnerMonth, spinnerYear, spinnerSite;
    private TextView tvTotalRadial, tvDataStatus;
    private LinearLayout llDaysList;
    private Button btnSave, btnSend, btnScanQR, btnReset;

    private HistoryDbHelper historyDb;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "FieldSyncPrefs";
    private static final String PREF_LAST_SITE = "last_site";
    private static final String ADMIN_PASSWORD = "niot@321";
    private static final String CONFIG_PREFS = "MyPrefs1";
    private static final String PREF_SITES_LIST = "sites_list";

    // Default sites (fallback if no dynamic list)
    private static final String[] DEFAULT_SITES = {"Cuda", "Kalp", "Mach", "Yanm", "Wasi", "Jgri", "Gopa", "Puri", "Ptbl", "Htby"};
    private String[] sites;
    private String[] months = {"January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    private int selectedMonth;
    private int selectedYear;
    private String selectedSite = "";
    private int[] radialCounts;
    private String[] dayReasons;
    private List<Spinner> radialSpinners = new ArrayList<>();
    private List<ImageButton> noteButtons = new ArrayList<>();
    private boolean dataSaved = false;
    private boolean dataLocked = false;
    private boolean isFromQRScan = false;
    private int[] qrRadialData = null;
    private boolean isParsingQRData = false; // Flag to prevent spinner listeners during QR parsing
    private boolean qrDataJustLoaded = false; // Flag to track if QR data was just loaded

    private static final String[] REASON_OPTIONS = {
            "Select Reason",
            "Radar System Problem",
            "Macmini Not Working",
            "Generator Not Working",
            "UPS Not Working",
            "Stabilizer Not Working",
            "No Diesel",
            "No Electricity For Long",
            "Antenna Fall Down",
            "AC Failed",
            "System Service/Repair/APM",
            "Other"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.home_inv, container, false);

        historyDb = new HistoryDbHelper(getActivity());
        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load sites list (dynamic from server or fallback to default)
        loadSitesList();

        initViews();
        setupSpinners();
        setupButtons();

        // Check if coming from QR scan
        checkForQRData();

        return view;
    }

    private void loadSitesList() {
        SharedPreferences configPrefs = getActivity().getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE);
        String sitesList = configPrefs.getString(PREF_SITES_LIST, "");

        if (sitesList.isEmpty()) {
            // Use default sites
            sites = new String[DEFAULT_SITES.length + 1];
            sites[0] = "Select Site";
            System.arraycopy(DEFAULT_SITES, 0, sites, 1, DEFAULT_SITES.length);
        } else {
            // Parse sites from comma-separated string
            String[] dynamicSites = sitesList.split(",");
            sites = new String[dynamicSites.length + 1];
            sites[0] = "Select Site";
            System.arraycopy(dynamicSites, 0, sites, 1, dynamicSites.length);
        }
    }

    private void initViews() {
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        spinnerYear = view.findViewById(R.id.spinnerYear);
        spinnerSite = view.findViewById(R.id.spinnerSite);
        tvTotalRadial = view.findViewById(R.id.tvTotalRadial);
        tvDataStatus = view.findViewById(R.id.tvDataStatus);
        llDaysList = view.findViewById(R.id.llDaysList);
        btnSave = view.findViewById(R.id.btnSave);
        btnSend = view.findViewById(R.id.btnSend);
        btnScanQR = view.findViewById(R.id.btnScanQR);
        btnReset = view.findViewById(R.id.btnReset);

        // Initially disable send button
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.5f);
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

        // Site spinner
        ArrayAdapter<String> siteAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, sites);
        siteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSite.setAdapter(siteAdapter);

        // Set default to previous month
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH); // 0-indexed
        if (currentMonth == 0) {
            // January, so previous month is December of previous year
            selectedMonth = 12;
            selectedYear = currentYear - 1;
            spinnerMonth.setSelection(11); // December
            spinnerYear.setSelection(1); // Previous year
        } else {
            selectedMonth = currentMonth; // Previous month (1-indexed)
            selectedYear = currentYear;
            spinnerMonth.setSelection(currentMonth - 1);
            spinnerYear.setSelection(0);
        }

        // Restore last selected site (radial data always starts at zero)
        String lastSite = sharedPreferences.getString(PREF_LAST_SITE, "");
        if (!lastSite.isEmpty()) {
            for (int i = 0; i < sites.length; i++) {
                if (sites[i].equals(lastSite)) {
                    spinnerSite.setSelection(i);
                    selectedSite = lastSite;
                    break;
                }
            }
        }

        // Month selection listener
        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position + 1;
                onSelectionChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Year selection listener
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = Integer.parseInt(parent.getItemAtPosition(position).toString());
                onSelectionChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Site selection listener
        spinnerSite.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedSite = "";
                } else {
                    selectedSite = sites[position];
                    // Save last selected site
                    sharedPreferences.edit().putString(PREF_LAST_SITE, selectedSite).apply();
                }
                onSelectionChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void onSelectionChanged() {
        // Skip if we're in the middle of parsing QR data
        if (isParsingQRData) {
            return;
        }

        // Skip regeneration if QR data was just loaded (prevents race conditions)
        if (qrDataJustLoaded) {
            qrDataJustLoaded = false; // Reset flag after first skip
            return;
        }

        dataSaved = false;
        dataLocked = false;
        btnSave.setText("SAVE & LOCK");
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.5f);
        tvDataStatus.setVisibility(View.GONE);

        if (!selectedSite.isEmpty()) {
            generateDaysList();
            // Never load historical radial data - always start with zeros
        } else {
            llDaysList.removeAllViews();
            tvTotalRadial.setText("0");
        }
    }

    private void generateDaysList() {
        llDaysList.removeAllViews();
        radialSpinners.clear();
        noteButtons.clear();

        int daysInMonth = getDaysInMonth(selectedMonth, selectedYear);
        radialCounts = new int[daysInMonth];
        dayReasons = new String[daysInMonth];

        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        // Create adapter for radial count spinner (24-0, reversed order)
        String[] radialOptions = new String[25];
        for (int i = 0; i <= 24; i++) {
            radialOptions[i] = String.valueOf(24 - i); // 24, 23, 22, ... 0
        }

        for (int day = 1; day <= daysInMonth; day++) {
            View dayView = LayoutInflater.from(getActivity()).inflate(R.layout.item_day_radial, llDaysList, false);

            TextView tvDayLabel = dayView.findViewById(R.id.tvDayLabel);
            TextView tvDayInfo = dayView.findViewById(R.id.tvDayInfo);
            Button btnMinus = dayView.findViewById(R.id.btnMinus);
            Spinner spinnerRadialCount = dayView.findViewById(R.id.spinnerRadialCount);
            Button btnPlus = dayView.findViewById(R.id.btnPlus);
            ImageButton btnNote = dayView.findViewById(R.id.btnNote);

            // Get day of week
            Calendar cal = Calendar.getInstance();
            cal.set(selectedYear, selectedMonth - 1, day);
            String dayOfWeek = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1];

            // Format: "1/12/2025" for main label, "Day 1 (Mon)" for info label
            tvDayLabel.setText(day + "/" + selectedMonth + "/" + selectedYear);
            tvDayInfo.setText("Day " + day + " (" + dayOfWeek + ")");

            // Setup spinner adapter
            ArrayAdapter<String> radialAdapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_item, radialOptions);
            radialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRadialCount.setAdapter(radialAdapter);

            final int dayIndex = day - 1;
            radialSpinners.add(spinnerRadialCount);
            noteButtons.add(btnNote);

            // Set initial value - either from QR data or default to 0
            int initialValue = 0;
            if (qrRadialData != null && dayIndex < qrRadialData.length) {
                int value = qrRadialData[dayIndex];
                if (value >= 0 && value <= 24) {
                    initialValue = value;
                }
            }
            radialCounts[dayIndex] = initialValue;
            // Set spinner position (reversed: value 0 = position 24, value 24 = position 0)
            spinnerRadialCount.setSelection(24 - initialValue);

            // Show note button if radial < 24
            if (initialValue < 24) {
                btnNote.setVisibility(View.VISIBLE);
            } else {
                btnNote.setVisibility(View.GONE);
            }

            // Note button click - show reason dialog
            btnNote.setOnClickListener(v -> showReasonDialog(dayIndex));

            // Minus button - decrease value
            btnMinus.setOnClickListener(v -> {
                int currentValue = getRadialValue(spinnerRadialCount);
                if (currentValue > 0) {
                    // Decrease value = increase position (since reversed)
                    spinnerRadialCount.setSelection(24 - (currentValue - 1));
                }
            });

            // Plus button - increase value
            btnPlus.setOnClickListener(v -> {
                int currentValue = getRadialValue(spinnerRadialCount);
                if (currentValue < 24) {
                    // Increase value = decrease position (since reversed)
                    spinnerRadialCount.setSelection(24 - (currentValue + 1));
                }
            });

            // Spinner selection listener
            spinnerRadialCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Convert position to actual value (reversed: position 0 = value 24, position 24 = value 0)
                    int actualValue = 24 - position;
                    radialCounts[dayIndex] = actualValue;
                    updateTotalRadial();

                    // Show/hide note button based on radial value
                    if (actualValue < 24) {
                        btnNote.setVisibility(View.VISIBLE);
                    } else {
                        btnNote.setVisibility(View.GONE);
                        // Clear reason when radial is 24
                        dayReasons[dayIndex] = null;
                        updateNoteButtonTint(btnNote, null);
                    }

                    // Mark as unsaved
                    dataSaved = false;
                    btnSend.setEnabled(false);
                    btnSend.setAlpha(0.5f);
                    tvDataStatus.setVisibility(View.GONE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // Alternate row colors
            if (day % 2 == 0) {
                dayView.setBackgroundColor(0xFFF5F5F5);
            }

            llDaysList.addView(dayView);
        }

        // DON'T clear qrRadialData here - it will be cleared after all callbacks fire
        // via view.post() in parseQRData()

        updateTotalRadial();
    }

    private int getRadialValue(Spinner spinner) {
        // Reversed spinner: position 0 = value 24, position 24 = value 0
        return 24 - spinner.getSelectedItemPosition();
    }

    private void updateTotalRadial() {
        int total = 0;
        for (int count : radialCounts) {
            total += count;
        }
        tvTotalRadial.setText(String.valueOf(total));
    }

    private void showReasonDialog(int dayIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Reason for Day " + (dayIndex + 1) + " (Radial: " + radialCounts[dayIndex] + ")");

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        // Reason dropdown spinner
        final Spinner reasonSpinner = new Spinner(getActivity());
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, REASON_OPTIONS);
        reasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reasonSpinner.setAdapter(reasonAdapter);

        // EditText for "Other" reason
        final EditText etOtherReason = new EditText(getActivity());
        etOtherReason.setHint("Type your reason here...");
        etOtherReason.setVisibility(View.GONE);

        // Pre-select existing reason if any
        String existingReason = dayReasons[dayIndex];
        if (existingReason != null && !existingReason.isEmpty()) {
            if (existingReason.startsWith("Other: ")) {
                // It's a custom "Other" reason
                reasonSpinner.setSelection(REASON_OPTIONS.length - 1); // "Other"
                etOtherReason.setVisibility(View.VISIBLE);
                etOtherReason.setText(existingReason.substring(7)); // Remove "Other: " prefix
            } else {
                // Find matching predefined reason
                for (int i = 0; i < REASON_OPTIONS.length; i++) {
                    if (REASON_OPTIONS[i].equals(existingReason)) {
                        reasonSpinner.setSelection(i);
                        break;
                    }
                }
            }
        }

        // Show/hide EditText when "Other" is selected
        reasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == REASON_OPTIONS.length - 1) { // "Other"
                    etOtherReason.setVisibility(View.VISIBLE);
                } else {
                    etOtherReason.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        layout.addView(reasonSpinner);
        layout.addView(etOtherReason);
        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedPosition = reasonSpinner.getSelectedItemPosition();
            if (selectedPosition == 0) {
                // "Select Reason" - clear reason
                dayReasons[dayIndex] = null;
            } else if (selectedPosition == REASON_OPTIONS.length - 1) {
                // "Other" - use typed text
                String otherText = etOtherReason.getText().toString().trim();
                if (!otherText.isEmpty()) {
                    dayReasons[dayIndex] = "Other: " + otherText;
                } else {
                    dayReasons[dayIndex] = null;
                }
            } else {
                dayReasons[dayIndex] = REASON_OPTIONS[selectedPosition];
            }

            // Update note button tint
            if (dayIndex < noteButtons.size()) {
                updateNoteButtonTint(noteButtons.get(dayIndex), dayReasons[dayIndex]);
            }

            // Mark as unsaved
            dataSaved = false;
            btnSend.setEnabled(false);
            btnSend.setAlpha(0.5f);
            tvDataStatus.setVisibility(View.GONE);
        });

        builder.setNegativeButton("CANCEL", null);

        // Add clear button
        builder.setNeutralButton("CLEAR", (dialog, which) -> {
            dayReasons[dayIndex] = null;
            if (dayIndex < noteButtons.size()) {
                updateNoteButtonTint(noteButtons.get(dayIndex), null);
            }
        });

        builder.show();
    }

    private void updateNoteButtonTint(ImageButton btnNote, String reason) {
        if (reason != null && !reason.isEmpty()) {
            // Orange tint when reason is set
            btnNote.setColorFilter(0xFFFF9800);
        } else {
            // Default color (no tint)
            btnNote.clearColorFilter();
        }
    }

    private int getDaysInMonth(int month, int year) {
        int[] daysPerMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int days = daysPerMonth[month - 1];

        // Check for leap year
        if (month == 2 && isLeapYear(year)) {
            days = 29;
        }
        return days;
    }

    private boolean isLeapYear(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    private void loadExistingData() {
        HistoryDbHelper.SavedDataEntry savedData = historyDb.getSavedData(selectedSite, selectedMonth, selectedYear);

        if (savedData != null) {
            try {
                JSONArray jsonArray = new JSONArray(savedData.radialDataJson);
                for (int i = 0; i < jsonArray.length() && i < radialSpinners.size(); i++) {
                    int value = jsonArray.getInt(i);
                    if (value >= 0 && value <= 24) {
                        // Convert value to spinner position (reversed: value 0 = position 24, value 24 = position 0)
                        radialSpinners.get(i).setSelection(24 - value);
                        radialCounts[i] = value;

                        // Show/hide note button
                        if (i < noteButtons.size()) {
                            ImageButton btnNote = noteButtons.get(i);
                            if (value < 24) {
                                btnNote.setVisibility(View.VISIBLE);
                            } else {
                                btnNote.setVisibility(View.GONE);
                            }
                        }
                    }
                }

                // Load reasons data
                if (savedData.reasonsDataJson != null && !savedData.reasonsDataJson.isEmpty()) {
                    try {
                        JSONArray reasonsArray = new JSONArray(savedData.reasonsDataJson);
                        for (int i = 0; i < reasonsArray.length() && i < dayReasons.length; i++) {
                            if (!reasonsArray.isNull(i)) {
                                String reason = reasonsArray.getString(i);
                                if (!reason.isEmpty()) {
                                    dayReasons[i] = reason;
                                    // Update note button tint
                                    if (i < noteButtons.size()) {
                                        updateNoteButtonTint(noteButtons.get(i), reason);
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        // Reasons data may be malformed, ignore
                    }
                }

                dataSaved = true;
                updateTotalRadial();

                if (savedData.isSent) {
                    dataLocked = false;
                    setGridLocked(false);
                    btnSave.setText("SAVE & LOCK");
                    btnSend.setEnabled(false);
                    btnSend.setAlpha(0.5f);
                    tvDataStatus.setText("Sent");
                    tvDataStatus.setTextColor(0xFF4CAF50);
                } else {
                    dataLocked = true;
                    setGridLocked(true);
                    btnSave.setText("UNLOCK & EDIT");
                    btnSend.setEnabled(true);
                    btnSend.setAlpha(1.0f);
                    tvDataStatus.setText("Saved & Locked");
                    tvDataStatus.setTextColor(0xFFFF9800);
                }
                tvDataStatus.setVisibility(View.VISIBLE);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupButtons() {
        btnSave.setText("SAVE & LOCK");
        btnSave.setOnClickListener(v -> toggleLock());
        btnSend.setOnClickListener(v -> sendData());

        // QR Scan button - navigate to scanner
        btnScanQR.setOnClickListener(v -> {
            Fragment frag = new ScannedBarcodeActivity();
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame, frag);
            transaction.commit();
        });

        // Reset button - reset all radial values to 0
        btnReset.setOnClickListener(v -> {
            if (radialSpinners.isEmpty()) {
                Toast.makeText(getActivity(), "No data to reset", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle("Reset All Data")
                    .setMessage("Are you sure you want to reset all radial values to 0?")
                    .setPositiveButton("RESET", (dialog, which) -> {
                        performReset();
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        });
    }

    private void performReset() {
        // Set all spinners to 0 (position 24 since reversed: position 0 = value 24, position 24 = value 0)
        for (int i = 0; i < radialSpinners.size(); i++) {
            radialSpinners.get(i).setSelection(24); // Value 0
            radialCounts[i] = 0;
        }

        // Clear all reasons
        if (dayReasons != null) {
            for (int i = 0; i < dayReasons.length; i++) {
                dayReasons[i] = null;
            }
        }
        // Reset note button tints
        for (ImageButton btnNote : noteButtons) {
            btnNote.clearColorFilter();
        }

        // Update total
        tvTotalRadial.setText("0");

        // Mark as unsaved and unlocked
        dataSaved = false;
        dataLocked = false;
        btnSave.setText("SAVE & LOCK");
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.5f);
        tvDataStatus.setVisibility(View.GONE);
        setGridLocked(false);

        Toast.makeText(getActivity(), "All values reset to 0", Toast.LENGTH_SHORT).show();
    }

    private void setGridLocked(boolean locked) {
        for (Spinner sp : radialSpinners) {
            sp.setEnabled(!locked);
            sp.setAlpha(locked ? 0.5f : 1.0f);
        }
        for (ImageButton nb : noteButtons) {
            nb.setEnabled(!locked);
            nb.setAlpha(locked ? 0.5f : 1.0f);
        }
        llDaysList.setAlpha(locked ? 0.7f : 1.0f);
    }

    private void toggleLock() {
        if (dataLocked) {
            // Unlock
            dataLocked = false;
            dataSaved = false;
            setGridLocked(false);
            btnSave.setText("SAVE & LOCK");
            btnSend.setEnabled(false);
            btnSend.setAlpha(0.5f);
            tvDataStatus.setVisibility(View.GONE);
            Toast.makeText(getActivity(), "Unlocked for editing", Toast.LENGTH_SHORT).show();
        } else {
            // Save & Lock
            saveData();
        }
    }

    private void saveData() {
        if (selectedSite.isEmpty()) {
            Toast.makeText(getActivity(), "Please select a site", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get all radial counts from spinners
        for (int i = 0; i < radialSpinners.size(); i++) {
            radialCounts[i] = getRadialValue(radialSpinners.get(i));
        }

        int total = 0;
        for (int count : radialCounts) {
            total += count;
        }

        // Show confirmation dialog
        final int finalTotal = total;
        new AlertDialog.Builder(getActivity())
                .setTitle("Confirm Save")
                .setMessage("Total Radial Count: " + total + "\n\nIs this correct?")
                .setPositiveButton("OK", (dialog, which) -> {
                    performSave(finalTotal);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performSave(int total) {
        // Convert radial counts to JSON
        JSONArray jsonArray = new JSONArray();
        for (int count : radialCounts) {
            jsonArray.put(count);
        }

        // Convert reasons to JSON
        JSONArray reasonsArray = new JSONArray();
        if (dayReasons != null) {
            for (String reason : dayReasons) {
                reasonsArray.put(reason); // null values become JSONObject.NULL
            }
        }

        historyDb.saveData(selectedSite, selectedMonth, selectedYear, jsonArray.toString(),
                reasonsArray.toString(), total);

        dataSaved = true;
        dataLocked = true;
        setGridLocked(true);
        btnSave.setText("UNLOCK & EDIT");
        btnSend.setEnabled(true);
        btnSend.setAlpha(1.0f);
        tvDataStatus.setText("Saved & Locked");
        tvDataStatus.setTextColor(0xFFFF9800);
        tvDataStatus.setVisibility(View.VISIBLE);

        Toast.makeText(getActivity(), "Data saved & locked", Toast.LENGTH_SHORT).show();
    }

    private void sendData() {
        if (!dataSaved) {
            Toast.makeText(getActivity(), "Please save data first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate total radial count
        int total = 0;
        for (int count : radialCounts) {
            total += count;
        }

        // Show confirmation dialog with details
        String monthName = months[selectedMonth - 1];
        String message = "Site: " + selectedSite + "\n" +
                         "Report Month: " + monthName + " " + selectedYear + "\n\n" +
                         "Total Radial Count: " + total + "\n\n" +
                         "Are you sure you want to send this data to server?";

        new AlertDialog.Builder(getActivity())
                .setTitle("Confirm Data Submission")
                .setMessage(message)
                .setPositiveButton("SEND", (dialog, which) -> {
                    proceedWithSend();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void proceedWithSend() {
        // Check if it's last month's data
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH) + 1;
        int currentYear = now.get(Calendar.YEAR);

        int lastMonth, lastMonthYear;
        if (currentMonth == 1) {
            lastMonth = 12;
            lastMonthYear = currentYear - 1;
        } else {
            lastMonth = currentMonth - 1;
            lastMonthYear = currentYear;
        }

        boolean isLastMonthData = (selectedMonth == lastMonth && selectedYear == lastMonthYear);

        if (isLastMonthData) {
            // Direct send
            performSend();
        } else {
            // Show password dialog
            showPasswordDialog();
        }
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Authorization Required");
        builder.setMessage("Enter admin password to send data for this month:");

        final EditText input = new EditText(getActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.equals(ADMIN_PASSWORD)) {
                performSend();
            } else {
                Toast.makeText(getActivity(), "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performSend() {
        // Get server URL from preferences
        SharedPreferences prefs = getActivity().getSharedPreferences("MyPrefs1", Context.MODE_PRIVATE);
        String baseUrl = prefs.getString("serv_url", "https://hfradarsite.pythonanywhere.com");
        String serverUrl = baseUrl + "/upldTsuData";

        new SendDataTask().execute(serverUrl);
    }

    private class SendDataTask extends AsyncTask<String, Void, Boolean> {
        private String errorMessage = "";

        @Override
        protected void onPreExecute() {
            btnSend.setEnabled(false);
            btnSend.setText("Sending...");
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String serverUrl = params[0];

                // Build JSON data in the format expected by the server
                // Format: { "source_type": "QR"/"Manual", "cred1": [ { "id": "Site", "dt": "YYYY-MM-DD", "rc": "count", "rchex": "", "reason": "..." }, ... ] }
                JSONObject parent = new JSONObject();
                JSONArray cred1 = new JSONArray();

                // Add source type
                parent.put("source_type", isFromQRScan ? "QR" : "Manual");

                int daysInMonth = radialCounts.length;
                String curmon = selectedMonth < 10 ? "0" + selectedMonth : String.valueOf(selectedMonth);

                for (int day = 1; day <= daysInMonth; day++) {
                    String dayStr = day < 10 ? "0" + day : String.valueOf(day);
                    String dateStr = selectedYear + "-" + curmon + "-" + dayStr;

                    JSONObject cred = new JSONObject();
                    cred.put("id", selectedSite);
                    cred.put("dt", dateStr);
                    cred.put("rc", String.valueOf(radialCounts[day - 1]));
                    cred.put("rchex", ""); // Empty for manual entry

                    // Add reason if present
                    if (dayReasons != null && (day - 1) < dayReasons.length
                            && dayReasons[day - 1] != null && !dayReasons[day - 1].isEmpty()) {
                        cred.put("reason", dayReasons[day - 1]);
                    }

                    cred1.put(cred);
                }

                parent.put("cred1", cred1);

                HttpClientExample httpClient = new HttpClientExample();
                String response = httpClient.sendPost(serverUrl, parent);

                return response != null && !response.isEmpty();

            } catch (Exception e) {
                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            btnSend.setEnabled(true);
            btnSend.setText("SEND");

            int total = 0;
            for (int count : radialCounts) total += count;

            String method = isFromQRScan ? "QR Code Scan" : "Add Data";

            if (success) {
                // Mark as sent in database
                historyDb.markAsSent(selectedSite, selectedMonth, selectedYear);

                // Add to history
                historyDb.addHistoryEntry(selectedSite, selectedMonth, selectedYear, total, method, "Success");

                // Unlock after send
                dataLocked = false;
                setGridLocked(false);
                btnSave.setText("SAVE & LOCK");
                btnSend.setEnabled(false);
                btnSend.setAlpha(0.5f);

                tvDataStatus.setText("Sent");
                tvDataStatus.setTextColor(0xFF4CAF50);

                Toast.makeText(getActivity(), "Data sent successfully!", Toast.LENGTH_SHORT).show();
            } else {
                // Add failed entry to history
                historyDb.addHistoryEntry(selectedSite, selectedMonth, selectedYear, total, method, "Failed");

                Toast.makeText(getActivity(), "Failed to send data. " + errorMessage, Toast.LENGTH_LONG).show();
            }

            isFromQRScan = false;
        }
    }

    private void checkForQRData() {
        SharedPreferences scanPrefs = getActivity().getSharedPreferences("ScanPrefs", Context.MODE_PRIVATE);
        String scanData = scanPrefs.getString("scan_data", "");

        // Clear scan cancelled flag if set (no longer needed since we don't load historical data)
        scanPrefs.edit().putString("scan_cancelled", "0").apply();

        // Handle both String and Int types for scan_data1 (legacy compatibility)
        String scanFlagStr = "0";
        try {
            scanFlagStr = scanPrefs.getString("scan_data1", "0");
        } catch (ClassCastException e) {
            // It was stored as int, read as int
            int scanFlagInt = scanPrefs.getInt("scan_data1", 0);
            scanFlagStr = String.valueOf(scanFlagInt);
        }

        if (scanFlagStr.equals("1") && !scanData.isEmpty()) {
            parseQRData(scanData);

            // Clear scan data - use String for consistency
            scanPrefs.edit()
                    .putString("scan_data", "")
                    .putString("scan_data1", "0")
                    .apply();
        }
    }

    private void parseQRData(String qrData) {
        try {
            // Format: Site:Month:Year:Day1:Day2:Day3:...:DayN:Total
            String[] parts = qrData.split(":");

            if (parts.length >= 4) {
                String site = parts[0];
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);

                // Parse radial data FIRST before touching UI
                // Format: Site:Month:Year:Day1:Day2:...:DayN:Total:Hex1:Hex2:...
                int daysInMonth = getDaysInMonth(month, year);
                int[] tempRadialData = new int[daysInMonth];

                int calculatedTotal = 0;

                for (int day = 0; day < daysInMonth; day++) {
                    int partIndex = 3 + day;
                    if (partIndex < parts.length) {
                        try {
                            if (!parts[partIndex].startsWith("0x")) {
                                int value = Integer.parseInt(parts[partIndex]);
                                if (value >= 0 && value <= 24) {
                                    tempRadialData[day] = value;
                                    calculatedTotal += value;
                                }
                            }
                        } catch (NumberFormatException e) {
                            tempRadialData[day] = 0;
                        }
                    }
                }

                // Verify total BEFORE updating any UI
                int totalIndex = 3 + daysInMonth;
                if (totalIndex < parts.length) {
                    try {
                        int qrTotal = Integer.parseInt(parts[totalIndex]);
                        if (qrTotal != calculatedTotal) {
                            Toast.makeText(getActivity(),
                                "QR validation failed! Total mismatch: QR shows " + qrTotal +
                                " but calculated " + calculatedTotal, Toast.LENGTH_LONG).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        // Could not parse total, skip validation
                    }
                }

                // Validation passed — now update UI
                isParsingQRData = true;
                qrRadialData = tempRadialData;

                // Set spinners
                for (int i = 0; i < sites.length; i++) {
                    if (sites[i].equalsIgnoreCase(site)) {
                        spinnerSite.setSelection(i);
                        selectedSite = sites[i];
                        break;
                    }
                }

                spinnerMonth.setSelection(month - 1);
                selectedMonth = month;

                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                int yearIndex = currentYear - year;
                if (yearIndex >= 0 && yearIndex < 6) {
                    spinnerYear.setSelection(yearIndex);
                    selectedYear = year;
                }

                // Keep flag true during generateDaysList to prevent spinner listeners from interfering
                isFromQRScan = true;
                generateDaysList();

                final int finalTotal = calculatedTotal;

                // Mark that QR data was just loaded - this provides an extra layer of protection
                // against any spinner callbacks that might fire after isParsingQRData is cleared
                qrDataJustLoaded = true;

                // Clear flag AFTER all pending UI events have been processed
                // Use postDelayed to ensure all spinner callbacks have definitely completed
                // 200ms gives ample time for all queued callbacks to process
                view.postDelayed(() -> {
                    isParsingQRData = false;
                    qrRadialData = null;
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "QR data loaded (Total: " + finalTotal + "). You can edit before saving.", Toast.LENGTH_LONG).show();
                    }
                }, 200);
            }

        } catch (Exception e) {
            isParsingQRData = false; // Make sure flag is cleared on error
            Toast.makeText(getActivity(), "Error parsing QR data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (historyDb != null) {
            historyDb.close();
        }
    }
}
