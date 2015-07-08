package com.example.android.wearable.watchface.calendar;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.wearable.provider.WearableCalendarContract;
import android.text.format.DateUtils;
import android.util.Log;

/**
 * Created by tvo on 7/8/15.
 */
public class WearableCalendarUtil {
    private static final String TAG = WearableCalendarUtil.class.getSimpleName();

    //  ...from sample code
    static public int getMeetings(Context context, int hourRange) {
        long begin = System.currentTimeMillis();
        Uri.Builder builder =
                WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, begin);
        ContentUris.appendId(builder, begin + DateUtils.HOUR_IN_MILLIS * hourRange);
        final Cursor cursor = context.getContentResolver().query(builder.build(),
                null, null, null, null);
        int numMeetings = cursor.getCount();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Num meetings: " + numMeetings);
        }
        return numMeetings;
    }

}
