/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Based on Asim Malik android source code, copyright (c) 2015
 *
 **/

package com.reactnative.googlefit;

import androidx.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.TimeZone;

public class StepHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String TAG = "RNGoogleFit";

    public StepHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public void aggregateDataByDate(long startTime,
                                    long endTime,
                                    int bucketInterval,
                                    String bucketUnit,
                                    final Callback successCallback) {
        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .setAppPackageName("com.google.android.gms")
                .build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(bucketInterval, HelperUtil.processBucketUnit(bucketUnit))
                .setTimeRange((long) startTime, (long) endTime, TimeUnit.MILLISECONDS)
                .build();

        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                        .build();

        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions);

        Fitness.getHistoryClient(this.mReactContext, googleSignInAccount)
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        if (dataReadResponse.getBuckets().size() > 0) {
                            WritableArray steps = Arguments.createArray();
                            for (Bucket bucket : dataReadResponse.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    processDataSet(dataSet, steps);
                                }
                            }
                            successCallback.invoke(steps);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "onFailure()");
                        Log.i(TAG, "Error" + e);
                    }
                });
    }

    private void processDataSet(DataSet dataSet, WritableArray map) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());

        WritableMap stepMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            for(Field field : dp.getDataType().getFields()) {
                stepMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("value", dp.getValue(field).asInt());
                map.pushMap(stepMap);
            }
        }
    }
}
