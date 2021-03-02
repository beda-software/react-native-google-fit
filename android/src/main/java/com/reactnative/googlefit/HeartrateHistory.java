/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package com.reactnative.googlefit;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.data.HealthDataTypes;
import com.google.android.gms.fitness.data.HealthFields;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class HeartrateHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;
    private DataSet Dataset;
    private DataType dataType;

    private static final String TAG = "Heart Rate History";

    public HeartrateHistory(ReactContext reactContext, GoogleFitManager googleFitManager, DataType dataType){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
        this.dataType = dataType;
    }

    public HeartrateHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this(reactContext, googleFitManager, DataType.TYPE_HEART_RATE_BPM);
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public ReadableArray getHistory(long startTime, long endTime, int bucketInterval, String bucketUnit) {
        DataReadRequest.Builder readRequestBuilder = new DataReadRequest.Builder()
                .read(this.dataType)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS);
        if (this.dataType == HealthDataTypes.TYPE_BLOOD_PRESSURE) {
            readRequestBuilder.bucketByTime(bucketInterval, HelperUtil.processBucketUnit(bucketUnit));
        }

        DataReadRequest readRequest = readRequestBuilder.build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(
                googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);

        WritableArray map = Arguments.createArray();

        //Used for aggregated data
        if (dataReadResult.getBuckets().size() > 0) {
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    processDataSet(dataSet, map);
                }
            }
        }
        //Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                processDataSet(dataSet, map);
            }
        }
        return map;
    }

    private void processDataSet(DataSet dataSet, WritableArray map) {
        Format formatter = new SimpleDateFormat("EEE");

        for (DataPoint dp : dataSet.getDataPoints()) {
            WritableMap stepMap = Arguments.createMap();
            String day = formatter.format(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
            int i = 0;

            for(Field field : dp.getDataType().getFields()) {
                i++;
                if (i > 1) continue;
                stepMap.putString("day", day);
                stepMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
                if (this.dataType == HealthDataTypes.TYPE_BLOOD_PRESSURE) {
                    stepMap.putDouble("value2", dp.getValue(HealthFields.FIELD_BLOOD_PRESSURE_DIASTOLIC).asFloat());
                    stepMap.putDouble("value", dp.getValue(HealthFields.FIELD_BLOOD_PRESSURE_SYSTOLIC).asFloat());
                } else {
                  stepMap.putDouble("value", dp.getValue(field).asFloat());
                }


                map.pushMap(stepMap);
            }
        }
    }

}
