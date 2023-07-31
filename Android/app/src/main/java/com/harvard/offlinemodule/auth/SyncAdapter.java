/*
 * Copyright © 2017-2019 Harvard Pilgrim Health Care Institute (HPHCI) and its Contributors.
 * Copyright 2020 Google LLC
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * Funding Source: Food and Drug Administration (“Funding Agency”) effective 18 September 2014 as Contract no. HHSF22320140030I/HHSF22301006T (the “Prime Contract”).
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.harvard.offlinemodule.auth;

import android.accounts.Account;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.harvard.R;
import com.harvard.ServiceManager;
import com.harvard.offlinemodule.model.OfflineData;
//import com.harvard.offlinemodule.model.RequestInput;
import com.harvard.storagemodule.DbServiceSubscriber;
import com.harvard.studyappmodule.events.ProcessResponseEvent;
import com.harvard.utils.AppController;
import com.harvard.utils.Logger;
import com.harvard.webservicemodule.apihelper.ApiCall;
import com.harvard.webservicemodule.apihelper.EnrollmentDataStoreInterface;
import com.harvard.webservicemodule.apihelper.NetworkRequest;
import com.harvard.webservicemodule.apihelper.ResponseServerInterface;
import com.harvard.webservicemodule.apihelper.UrlTypeConstants;

import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.RequestBody;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

public class SyncAdapter extends AbstractThreadedSyncAdapter
    implements ApiCall.OnAsyncRequestComplete {

  private Context context;
  private static final int UPDATE_USERPREFERENCE_RESPONSECODE = 102;
  private DbServiceSubscriber dbServiceSubscriber;
  public static final String TAG_MY_WORK = "BACKUP_WORKER_TAG";
  private EnrollmentDataStoreInterface enrollmentDataStoreInterface;
  private ResponseServerInterface responseServerInterface;
  private JsonObject json;
  private RequestBody body;
  RealmResults<OfflineData> results = null;
  Realm realm;

  public SyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    this.context = context;
    dbServiceSubscriber = new DbServiceSubscriber();
  }

  @Override
  public void onPerformSync(
      Account account,
      Bundle extras,
      String authority,
      ContentProviderClient contentProviderClient,
      SyncResult syncResult) {
// check if your work is not already scheduled
    Log.e("check","sync data in background");
    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ActiveTaskWorkManger.class).addTag(TAG_MY_WORK).build();
    WorkManager.getInstance(context).enqueue(request);
//    if(!isWorkScheduled(TAG_MY_WORK)) {
//      // schedule your work
//      scheduleWork(TAG_MY_WORK,context);
//    }
//    if (!isMyServiceRunning(ActiveTaskService.class)) {
//      OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ActiveTaskWorkManger.class).addTag("BACKUP_WORKER_TAG").build();
//      WorkManager.getInstance(context).enqueue(request);
//      Intent intent = new Intent(context, ActiveTaskService.class);
//      intent.putExtra("SyncAdapter", "yes");
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        context.startForegroundService(intent);
//      } else {
//        context.startService(intent);
//      }
//    }
  }

    public static void scheduleWork(String tag, Context context) {
    Log.e("check","worker manger is not-up");

    }



  private boolean isMyServiceRunning(Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service :
        manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  private boolean isWorkScheduled(String tag) {
    WorkManager instance = WorkManager.getInstance(context);
    ListenableFuture<List<WorkInfo>> statuses = instance.getWorkInfosByTag(tag);
    try {
      boolean running = false;
      List<WorkInfo> workInfoList = statuses.get();
      for (WorkInfo workInfo : workInfoList) {
        WorkInfo.State state = workInfo.getState();
        running = state == WorkInfo.State.RUNNING | state == WorkInfo.State.ENQUEUED;
      }
      Log.e("check","status of workermanger "+running);
      return running;
    } catch (ExecutionException e) {
      e.printStackTrace();
      return false;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
  }

  private void getPendingData() {
    try {
      ((Activity) context).runOnUiThread(new Runnable() {
        @Override
        public void run() {
          realm = AppController.getRealmobj(context);
          dbServiceSubscriber = new DbServiceSubscriber();
          results = dbServiceSubscriber.getOfflineData(realm);
          if (!results.isEmpty() || results != null) {
            for (int i = 0; i < results.size(); i++) {
              String httpMethod = results.get(i).getHttpMethod();
              String url = results.get(i).getUrl();
              String jsonObject = results.get(i).getJsonParam();
              String serverType = results.get(i).getServerType();
              updateServer(httpMethod, url, jsonObject, serverType);
              break;
            }
          } else {
            dbServiceSubscriber.closeRealmObj(realm);
          }
        }
      });
    } catch (Exception e) {
      Logger.log(e);
    }
  }

  private void updateServer(
      String httpMethod, String url, String jsonObjectString, String serverType) {

    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(jsonObjectString);
      JsonParser jsonParser = new JsonParser();
      json = (JsonObject)jsonParser.parse(jsonObjectString);

    } catch (JSONException e) {
      Logger.log(e);
    }

    if (serverType.equalsIgnoreCase("ParticipantEnrollmentDatastore")) {
     sendParticipantDetails(url);
    } else if (serverType.equalsIgnoreCase("ResponseDatastore")) {
     sendResponseData(url);
    }
  }

  private void sendResponseData(String url) {
    HashMap<String, String> header = new HashMap();
    header.put(
            "Authorization",
            "Bearer "+AppController.getHelperSharedPreference()
                    .readPreference(context, context.getResources().getString(R.string.auth), ""));
    header.put(
            "userId",
            AppController.getHelperSharedPreference()
                    .readPreference(context, context.getResources().getString(R.string.userid), ""));
//    RequestInput requestInput = new RequestInput();
//    requestInput.setJsonObject(json);

    responseServerInterface = new ServiceManager()
            .createService(ResponseServerInterface.class, UrlTypeConstants.ResponseDataStore);
    NetworkRequest.performAsyncRequest(responseServerInterface.getRequest(url, header,body),
            (data) -> {
              Log.d("ResponseDataStore:", ""+data);


                  dbServiceSubscriber.removeOfflineData(context);
                  getPendingData();

            }, (error) -> {
              Log.d("getRequests:", ""+error.toString());
              int code = AppController.getErrorCode(error);
              String errormsg = AppController.getErrorMessage(error);
              if (code == 401 && errormsg.equalsIgnoreCase("Unauthorized or Invalid token")) {
                AppController.checkRefreshToken(context, new AppController.RefreshTokenListener() {
                  @Override
                  public void onRefreshTokenCompleted(String result) {
                    if (result.equalsIgnoreCase("sucess")) {
                      sendResponseData(url);
                    } else {
                      AppController.getHelperProgressDialog().dismissDialog();
                      AppController.getHelperSessionExpired(context, "");
                    }
                  }
                }, UrlTypeConstants.AuthServer);
              }

            });
//    ProcessResponseEvent processResponseEvent = new ProcessResponseEvent();
    /*  ResponseDatastoreConfigEvent responseDatastoreConfigEvent =
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
      studyModulePresenter.performProcessResponse(processResponseEvent);*/
  }

  private void sendParticipantDetails(String url) {
    HashMap<String, String> header = new HashMap();
    header.put(
        "Authorization",
        "Bearer "+ AppController.getHelperSharedPreference()
                    .readPreference(context, context.getResources().getString(R.string.auth), ""));
    header.put(
            "userId",
            AppController.getHelperSharedPreference()
                    .readPreference(context, context.getResources().getString(R.string.userid), ""));
//    RequestInput requestInput = new RequestInput();
//    requestInput.setJsonObject(json);
    enrollmentDataStoreInterface = new ServiceManager()
            .createService(EnrollmentDataStoreInterface.class, UrlTypeConstants.EnrollmentDataStore);
    NetworkRequest.performAsyncRequest(enrollmentDataStoreInterface.getRequest(url, header,body),
            (data) -> {
              Log.d("getRequests:", ""+data);

              ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  dbServiceSubscriber.removeOfflineData(context);
                  getPendingData();
                }
              });
            }, (error) -> {
              Log.d("getRequests:", ""+error.toString());
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
              }
            });

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
    Log.d("asyncResponseFailure:", ""+responseCode);

  }
}
