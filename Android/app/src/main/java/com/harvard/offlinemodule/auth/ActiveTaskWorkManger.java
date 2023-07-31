package com.harvard.offlinemodule.auth;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.harvard.FdaApplication;
import com.harvard.R;
import com.harvard.ServiceManager;
import com.harvard.offlinemodule.model.OfflineData;
//import com.harvard.offlinemodule.model.RequestInput;
import com.harvard.storagemodule.DbServiceSubscriber;
import com.harvard.utils.AppController;
import com.harvard.utils.Logger;
import com.harvard.webservicemodule.apihelper.ApiCall;
import com.harvard.webservicemodule.apihelper.EnrollmentDataStoreInterface;
import com.harvard.webservicemodule.apihelper.NetworkRequest;
import com.harvard.webservicemodule.apihelper.ResponseServerInterface;
import com.harvard.webservicemodule.apihelper.UrlTypeConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.RequestBody;

public class ActiveTaskWorkManger extends Worker implements ApiCall.OnAsyncRequestComplete {

    private Context context;
    private static final int UPDATE_USERPREFERENCE_RESPONSECODE = 102;
    private DbServiceSubscriber dbServiceSubscriber;
    private Realm realm;
    private RealmResults<OfflineData> results = null;
    private EnrollmentDataStoreInterface enrollmentDataStoreInterface;
    private ResponseServerInterface responseServerInterface;
    private JsonObject json;
    private RequestBody body;

    public ActiveTaskWorkManger(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            dbServiceSubscriber = new DbServiceSubscriber();
            realm = AppController.getRealmobj(context);
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            Notification notification =
                    new NotificationCompat.Builder(context, "")
                            .setContentTitle(context.getResources().getString(R.string.app_name))
                            .setTicker("Sync adapter")
                            .setContentText("Syncing offline data")
                            .setChannelId(FdaApplication.NOTIFICATION_CHANNEL_ID_SERVICE)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                            .setOngoing(true)
                            .build();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(102, notification);
            results = dbServiceSubscriber.getOfflineData(realm);
            if (results == null || results.size() == 0) {
                onStopped();
            }
            getPendingData();
        } catch (Exception e) {
            Log.e("check", "error is " + e);
            onStopped();
            Logger.log(e);
        }

        return Result.success();
    }

  @Override
  public void onStopped() {
    Log.e("check","data is calling stop");
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancel(102);
    super.onStopped();
  }

  private void getPendingData() {
    try {
      realm = AppController.getRealmobj(context);
      dbServiceSubscriber = new DbServiceSubscriber();
      results = dbServiceSubscriber.getOfflineData(realm);
      Log.e("check", "result is " + results);
      if (!results.isEmpty()) {
        for (int i = 0; i < results.size(); i++) {
          String httpMethod = results.get(i).getHttpMethod();
          String url = results.get(i).getUrl();
          String normalParam = results.get(i).getNormalParam();
          String jsonObject = results.get(i).getJsonParam();
          String serverType = results.get(i).getServerType();
          updateServer(httpMethod, url, normalParam, jsonObject, serverType);
          break;
        }
      } else {
        dbServiceSubscriber.closeRealmObj(realm);
        Log.e("check", "data is getPendingData else ");
        onStopped();
      }
    } catch (Exception e) {
      Log.e("check", "data is got exception " + e.getMessage());
      onStopped();
      Logger.log(e);
    }
  }


    public void updateServer(
            String httpMethod,
            String url,
            String normalParam,
            String jsonObjectString,
            String serverType) {

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonObjectString);
            JsonParser jsonParser = new JsonParser();
            json = (JsonObject)jsonParser.parse(jsonObjectString);
          try {
            body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(json.toString())).toString());
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }

        } catch (JSONException e) {
            Logger.log(e);
        }

        if (serverType.equalsIgnoreCase("ParticipantEnrollmentDatastore")) {
            sendParticipantDetails(url);
        } else if (serverType.equalsIgnoreCase("ResponseDatastore")) {
            sendResponseDetails(url);
        }
    }

  private void sendResponseDetails(String url) {
    HashMap<String, String> header = new HashMap();
    header.put(
        "Authorization",
        "Bearer " + AppController.getHelperSharedPreference()
            .readPreference(context, context.getResources().getString(R.string.auth), ""));
    header.put(
        "userId",
        AppController.getHelperSharedPreference()
            .readPreference(
                context, context.getResources().getString(R.string.userid), ""));

          /*  ProcessResponseEvent processResponseEvent = new ProcessResponseEvent();
            ResponseDatastoreConfigEvent responseDatastoreConfigEvent =
                    new ResponseDatastoreConfigEvent(
                            httpMethod,
                            url,
                            UPDATE_USERPREFERENCE_RESPONSECODE,
                            context,
                            LoginData.class,
                            null,
                            header,
                            jsonObject,
                            false,
                            this);

            processResponseEvent.setResponseDatastoreConfigEvent(responseDatastoreConfigEvent);
            StudyModulePresenter studyModulePresenter = new StudyModulePresenter();
            studyModulePresenter.performProcessResponse(processResponseEvent);
*/
//        RequestInput requestInput = new RequestInput();
//        requestInput.setJsonObject(json);
    responseServerInterface = new ServiceManager()
        .createService(ResponseServerInterface.class, UrlTypeConstants.ResponseDataStore);
    NetworkRequest.performAsyncRequest(responseServerInterface.getRequest(url, header, body),
        (data) -> {
          Log.e("ResponseDataStore:", "" + data.getMessage());
          dbServiceSubscriber.removeOfflineData(context);
          getPendingData();
        }, (error) -> {
          Log.e("ResponseDataStoreerror:", "" + error);
          int code = AppController.getErrorCode(error);
          String errormsg = AppController.getErrorMessage(error);
          if (code == 401 && errormsg.equalsIgnoreCase("Unauthorized or Invalid token")) {
            AppController.checkRefreshToken(context, new AppController.RefreshTokenListener() {
              @Override
              public void onRefreshTokenCompleted(String result) {
                if (result.equalsIgnoreCase("sucess")) {
                  header.put(
                      "Authorization",
                      "Bearer "
                          + AppController.getHelperSharedPreference()
                          .readPreference(context, context.getResources().getString(R.string.auth), ""));
                  sendResponseDetails(url);

                } else {
                  AppController.getHelperProgressDialog().dismissDialog();
                  AppController.getHelperSessionExpired(context, "");
                }
              }
            }, UrlTypeConstants.AuthServer);
          } else {
            onStopped();
          }
        });
  }

  private void sendParticipantDetails(String url) {
    HashMap<String, String> header = new HashMap();
    header.put(
        "Authorization",
        "Bearer " + AppController.getHelperSharedPreference()
            .readPreference(context, context.getResources().getString(R.string.auth), ""));
    header.put(
        "userId",
        AppController.getHelperSharedPreference()
            .readPreference(context, context.getResources().getString(R.string.userid), ""));

 /* UpdatePreferenceEvent updatePreferenceEvent = new UpdatePreferenceEvent();
      ParticipantEnrollmentDatastoreConfigEvent participantEnrollmentDatastoreConfigEvent =
          new ParticipantEnrollmentDatastoreConfigEvent(
              httpMethod,
              url,
              UPDATE_USERPREFERENCE_RESPONSECODE,
              context,
              LoginData.class,
              null,
              header,
              jsonObject,
              false,
              this);
      updatePreferenceEvent.setParticipantEnrollmentDatastoreConfigEvent(
          participantEnrollmentDatastoreConfigEvent);
      UserModulePresenter userModulePresenter = new UserModulePresenter();
      userModulePresenter.performUpdateUserPreference(updatePreferenceEvent);*/

    enrollmentDataStoreInterface = new ServiceManager()
        .createService(EnrollmentDataStoreInterface.class, UrlTypeConstants.EnrollmentDataStore);
    NetworkRequest.performAsyncRequest(enrollmentDataStoreInterface.getRequest(url, header, body),
        (data) -> {
          Log.e("getRequests:", "" + data);

          dbServiceSubscriber.removeOfflineData(context);
          getPendingData();
        }, (error) -> {

          Log.e("Enrollerror:", "" + error);
          int code = AppController.getErrorCode(error);
          String errormsg = AppController.getErrorMessage(error);
          if (code == 401 && errormsg.equalsIgnoreCase("Unauthorized or Invalid token")) {
            AppController.checkRefreshToken(context, new AppController.RefreshTokenListener() {
              @Override
              public void onRefreshTokenCompleted(String result) {
                if (result.equalsIgnoreCase("sucess")) {
                  sendParticipantDetails(url);

                } else {
                  AppController.getHelperProgressDialog().dismissDialog();
                  AppController.getHelperSessionExpired(context, "");
                }
              }
            }, UrlTypeConstants.AuthServer);
          } else {
            onStopped();
          }


        });
  }

    @Override
    public <T> void asyncResponse(T response, int responseCode) {
        if (responseCode == UPDATE_USERPREFERENCE_RESPONSECODE) {
            dbServiceSubscriber.removeOfflineData(context);
            getPendingData();

        }
    }

    @Override
    public void asyncResponseFailure(int responseCode, String errormsg, String statusCode) {
        onStopped();
    }
}
