package com.example.hfradar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver that reschedules the monthly reminder after device reboot.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule the monthly reminder worker
            if (MonthlyReminderWorker.isEnabled(context)) {
                MonthlyReminderWorker.schedule(context);
            }
        }
    }
}
