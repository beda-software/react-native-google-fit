package com.reactnative.googlefit;

import android.util.Log;
import androidx.annotation.NonNull;
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
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorkoutHistory {
    private ReactContext mReactContext;

    private static final String TAG = "WorkoutHistory";

    public WorkoutHistory(ReactContext reactContext, GoogleFitManager googleFitManager) {
        this.mReactContext = reactContext;
    }

    public void aggregateDataByDate(long startTime,
                                    long endTime,
                                    int bucketInterval,
                                    String bucketUnit,
                                    final Callback successCallback) {
        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_WORKOUT_EXERCISE)
                .readSessionsFromAllApps()
                .build();

        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_WORKOUT_EXERCISE)
                        .build();

        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions);

        Fitness.getSessionsClient(this.mReactContext, googleSignInAccount)
                .readSession(readRequest)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse sessionReadResponse) {
                        List<Session> sessions = sessionReadResponse.getSessions();
                        Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                                + sessions.size());
                        WritableArray workouts = Arguments.createArray();

                        for (Session session : sessions) {
                            WritableMap workoutMap = Arguments.createMap();
                            if (session.hasActiveTime()) {
                                workoutMap.putString("activityType", session.getActivity());
                                workoutMap.putDouble("value", session.getActiveTime(TimeUnit.MINUTES));
                                workoutMap.putDouble("startDate", session.getStartTime(TimeUnit.MILLISECONDS));
                                workoutMap.putDouble("endDate", session.getStartTime(TimeUnit.MILLISECONDS));
                                workouts.pushMap(workoutMap);
                            }
                        }
                        successCallback.invoke(workouts);

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
}
