package com.example.hfradar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.List;

public class HistoryFragment extends Fragment {

    private View view;
    private LinearLayout llHistoryList;
    private TextView tvEmptyState;
    private HistoryDbHelper historyDb;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_history, container, false);

        historyDb = new HistoryDbHelper(getActivity());

        llHistoryList = view.findViewById(R.id.llHistoryList);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        loadHistory();

        return view;
    }

    private void loadHistory() {
        llHistoryList.removeAllViews();

        List<HistoryDbHelper.HistoryEntry> historyList = historyDb.getAllHistory();

        if (historyList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            llHistoryList.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            llHistoryList.setVisibility(View.VISIBLE);

            for (HistoryDbHelper.HistoryEntry entry : historyList) {
                View itemView = LayoutInflater.from(getActivity()).inflate(R.layout.item_history, llHistoryList, false);

                TextView tvStatusIcon = itemView.findViewById(R.id.tvStatusIcon);
                TextView tvSiteMonth = itemView.findViewById(R.id.tvSiteMonth);
                TextView tvDateTime = itemView.findViewById(R.id.tvDateTime);
                TextView tvMethod = itemView.findViewById(R.id.tvMethod);
                TextView tvTotal = itemView.findViewById(R.id.tvTotal);

                // Set status icon
                if (entry.status.equals("Success")) {
                    tvStatusIcon.setText("✓");
                    tvStatusIcon.setTextColor(0xFF4CAF50);
                } else {
                    tvStatusIcon.setText("✗");
                    tvStatusIcon.setTextColor(0xFFF44336);
                }

                // Set site and month
                tvSiteMonth.setText(entry.site + " - " + entry.getMonthName() + " " + entry.year);

                // Set date and time
                tvDateTime.setText("Sent: " + entry.sentDate + ", " + entry.sentTime);

                // Set method
                tvMethod.setText("Method: " + entry.method);

                // Set total
                tvTotal.setText(String.valueOf(entry.totalRadial));

                llHistoryList.addView(itemView);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (historyDb != null) {
            historyDb.close();
        }
    }
}
