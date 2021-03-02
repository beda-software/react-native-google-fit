/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package com.reactnative.googlefit;

import android.os.AsyncTask;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;


public class BodyHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;
    private DataSet Dataset;
    private DataType dataType;

    private static final String TAG = "Body History";

    public BodyHistory(ReactContext reactContext, GoogleFitManager googleFitManager, DataType dataType){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
        this.dataType = dataType;
    }

    public BodyHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this(reactContext, googleFitManager, DataType.TYPE_WEIGHT);
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @Nullable
    public WritableMap getMostRecent() {
        Calendar cal = java.util.Calendar.getInstance();

        DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                .read(this.dataType)
                .setTimeRange(1, cal.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .setLimit(1)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(
                googleFitManager.getGoogleApiClient(), dataReadRequest).await(1, TimeUnit.MINUTES);

        WritableArray map = Arguments.createArray();
        if (dataReadResult.getDataSets().size() > 0) {
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                processDataSet(dataSet, map, false);
            }
        }
        WritableMap result = Arguments.createMap();
        result.merge(map.getMap(0));

        return result;
    }

    private void processDataSet(DataSet dataSet, WritableArray map, boolean aggregated) {
        //Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        Format formatter = new SimpleDateFormat("EEE");

        WritableMap bodyMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            String day = formatter.format(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));

            bodyMap.putString("day", day);
            bodyMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
            bodyMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
            bodyMap.putString("addedBy", dp.getOriginalDataSource().getAppPackageName());

            // When there is a short interval between weight readings (< 1 hour or so), some phones e.g.
            // Galaxy S5 use the average of the readings, whereas other phones e.g. Huawei P9 Lite use the
            // most recent of the bunch (this might be related to Android versions - 6.0.1 vs 7.0 in this
            // example for former and latter)
            //
            // For aggregated weight summary, only the min, max and average values are available (i.e. the
            // most recent sample is not an option), so use average value to maximise the match between values
            // returned here and values as reported by Google Fit app
            if (this.dataType == DataType.TYPE_WEIGHT) {
                bodyMap.putDouble("value", dp.getValue(aggregated ? Field.FIELD_AVERAGE : Field.FIELD_WEIGHT).asFloat());
            } else {
                bodyMap.putDouble("value", dp.getValue(Field.FIELD_HEIGHT).asFloat());
            }
        }
        map.pushMap(bodyMap);
    }
}
