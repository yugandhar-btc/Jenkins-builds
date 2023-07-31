/*
 * Copyright © 2017-2019 Harvard Pilgrim Health Care Institute (HPHCI) and its Contributors.
 * Copyright 2020-2021 Google LLC
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
 *
 */

package com.harvard.studyappmodule;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.harvard.AppConfig;
import com.harvard.AppFirebaseMessagingService;
import com.harvard.R;
import com.harvard.ServiceManager;
import com.harvard.notificationmodule.AlarmReceiver;
import com.harvard.storagemodule.DbServiceSubscriber;
import com.harvard.storagemodule.events.DatabaseEvent;
import com.harvard.studyappmodule.consent.ConsentBuilder;
import com.harvard.studyappmodule.consent.CustomConsentViewTaskActivity;
import com.harvard.studyappmodule.consent.model.Consent;
import com.harvard.studyappmodule.consent.model.CorrectAnswerString;
import com.harvard.studyappmodule.consent.model.EligibilityConsent;
import com.harvard.studyappmodule.events.ConsentPdfEvent;
import com.harvard.studyappmodule.studymodel.ConsentDocumentData;
import com.harvard.studyappmodule.studymodel.ConsentPDF;
import com.harvard.studyappmodule.studymodel.Study;
import com.harvard.studyappmodule.studymodel.StudyList;
import com.harvard.studyappmodule.studymodel.StudyUpdate;
import com.harvard.studyappmodule.studymodel.StudyUpdateListdata;
import com.harvard.studyappmodule.surveyscheduler.SurveyScheduler;
import com.harvard.studyappmodule.surveyscheduler.model.CompletionAdherence;
import com.harvard.usermodule.webservicemodel.Studies;
import com.harvard.usermodule.webservicemodel.StudyData;
import com.harvard.utils.AppController;
import com.harvard.utils.Logger;
import com.harvard.utils.Urls;
import com.harvard.webservicemodule.apihelper.ApiCall;
import com.harvard.webservicemodule.apihelper.ConnectionDetector;
import com.harvard.webservicemodule.apihelper.ConsentDataStoreInterface;
import com.harvard.webservicemodule.apihelper.EnrollmentDataStoreInterface;
import com.harvard.webservicemodule.apihelper.HttpRequest;
import com.harvard.webservicemodule.apihelper.NetworkRequest;
import com.harvard.webservicemodule.apihelper.Responsemodel;
import com.harvard.webservicemodule.apihelper.StudyDataStoreAPIInterface;
import com.harvard.webservicemodule.apihelper.UrlTypeConstants;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Subscription;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.researchstack.backbone.step.Step;
import org.researchstack.backbone.task.OrderedTask;
import org.researchstack.backbone.task.Task;

public class StandaloneActivity extends AppCompatActivity
        implements ApiCall.OnAsyncRequestComplete {

    private static final int SPECIFIC_STUDY = 10;
    private static final int GET_PREFERENCES = 11;
    private static final int GET_CONSENT_DOC = 204;
    private static final int STUDY_UPDATES = 201;
    private static final int CONSENTPDF = 206;
    private static final int CONSENT_RESPONSECODE = 203;
    private static final String CONSENT = "consent";
    private DbServiceSubscriber dbServiceSubscriber;
    private Realm realm;
    private RealmList<StudyList> studyListArrayList;
    private static final String YET_TO_JOIN = "yetToEnroll";
    private static final String IN_PROGRESS = "enrolled";
    private static final String ACTIVE = "active";
    private static final String PAUSED = "paused";
    private static final String CLOSED = "closed";
    private String eligibilityType = "";
    private String calledFor = "";
    private String from = "";
    private String activityId;
    private String localNotification;
    private String latestConsentVersion = "0";
    private boolean enrollAgain;
    private RealmList<Studies> studiesArrayList = new RealmList<>();
    private ArrayList<CompletionAdherence> completionAdherenceCalcs = new ArrayList<>();

    private static final String FROM = "from";

    private String title;
    private String studyId;
    private Study study;
    private String intentFrom = "";
    private StudyDataStoreAPIInterface apiInterface;
    private Subscription mSBNetworkSubscriptionl;
    private StudyUpdate studyUpdate;
    private EnrollmentDataStoreInterface enrollmentDataStoreInterface;
    private StudyData studies;
    private int code = 0;
    private String errormsg = null;
    private ConsentDataStoreInterface consentDataStoreInterface;
    private ConsentPDF consentPdfData;
    private ConsentDocumentData consentDocumentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_standalone);
        studyId = AppConfig.StudyId;

        if (getIntent().getStringExtra(FROM) != null) {
            intentFrom = getIntent().getStringExtra(FROM);
        } else {
            intentFrom = "";
        }

        if (AppConfig.AppType.equalsIgnoreCase(getString(R.string.app_standalone))) {
            if (!AppController.getHelperSharedPreference()
                    .readPreference(StandaloneActivity.this, getResources().getString(R.string.userid), "")
                    .equalsIgnoreCase("")) {

                dbServiceSubscriber = new DbServiceSubscriber();
                realm = AppController.getRealmobj(this);
                studyListArrayList = new RealmList<>();

                AppController.getHelperProgressDialog()
                        .showProgress(StandaloneActivity.this, "", "", false);
       /* GetUserStudyListEvent getUserStudyListEvent = new GetUserStudyListEvent();
        HashMap<String, String> header = new HashMap();
        HashMap<String, String> params = new HashMap();
        params.put("studyId", AppConfig.StudyId);
        StudyDatastoreConfigEvent studyDatastoreConfigEvent =
            new StudyDatastoreConfigEvent(
                "get",
                Urls.SPECIFIC_STUDY + "?studyId=" + AppConfig.StudyId,
                SPECIFIC_STUDY,
                StandaloneActivity.this,
                Study.class,
                params,
                header,
                null,
                false,
                this);

        getUserStudyListEvent.setStudyDatastoreConfigEvent(studyDatastoreConfigEvent);
        StudyModulePresenter studyModulePresenter = new StudyModulePresenter();
        studyModulePresenter.performGetGateWayStudyList(getUserStudyListEvent);*/
        specialStudyApiCall(AppConfig.StudyId);
      } else {
        Intent intent = new Intent(StandaloneActivity.this, StandaloneStudyInfoActivity.class);
        ComponentName cn = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(cn);
        mainIntent.putExtra("flow", getIntent().getStringExtra("flow"));
        startActivity(mainIntent);
        finish();
      }
    } else {
      Intent intent = new Intent(StandaloneActivity.this, StudyActivity.class);
      ComponentName cn = intent.getComponent();
      Intent mainIntent = Intent.makeRestartActivityTask(cn);
      startActivity(mainIntent);
      finish();
    }
  }

  private void specialStudyApiCall(String studyId) {
    apiInterface = new ServiceManager().createService(StudyDataStoreAPIInterface.class, UrlTypeConstants.StudyDataStore);
    mSBNetworkSubscriptionl = NetworkRequest.performAsyncRequest(apiInterface.getSpecificStudy(studyId), (data) -> {
      try {
        setSpecificStudy(data);
      }catch (Exception e){
        Log.e("TAG", "error: " + e.getMessage());
      }
    }, (error) -> {
      code = AppController.getErrorCode(error);
      errormsg = AppController.getErrorMessage(error);
      if (code == 401 && errormsg.equalsIgnoreCase("Unauthorized or Invalid token")) {
        AppController.checkRefreshToken(StandaloneActivity.this, new AppController.RefreshTokenListener() {
          @Override
          public void onRefreshTokenCompleted(String result) {
            if (result.equalsIgnoreCase("sucess")) {
              specialStudyApiCall(studyId);
            } else {
              AppController.getHelperProgressDialog().dismissDialog();
              Toast.makeText(StandaloneActivity.this, "session expired", Toast.LENGTH_LONG).show();
              AppController.getHelperSessionExpired(StandaloneActivity.this, "");
            }
          }
        }, UrlTypeConstants.StudyDataStore);
      } else {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            AppController.getHelperProgressDialog().dismissDialog();
            // offline handling
            study = dbServiceSubscriber.getStudyListFromDB(realm);
            if (study != null && study.getStudies() != null && !study.getStudies().isEmpty()) {
              studyListArrayList = study.getStudies();
              studyListArrayList =
                  dbServiceSubscriber.saveStudyStatusToStudyList(studyListArrayList, realm);
              setStudyList(true);

              // like click
              if (studyListArrayList.get(0).getStudyStatus().equalsIgnoreCase(IN_PROGRESS)) {
                getStudyUpdate(
                    studyListArrayList.get(0).getStudyId(),
                    studyListArrayList.get(0).getStudyVersion(),
                    studyListArrayList.get(0).getTitle(),
                    "",
                    "",
                    "",
                    "");
              } else {
                Intent intent = new Intent(StandaloneActivity.this, StandaloneStudyInfoActivity.class);
                intent.putExtra("flow", getIntent().getStringExtra("flow"));
                intent.putExtra("studyId", studyListArrayList.get(0).getStudyId());
                intent.putExtra("title", studyListArrayList.get(0).getTitle());
                intent.putExtra("status", studyListArrayList.get(0).getStatus());
                intent.putExtra("studyStatus", studyListArrayList.get(0).getStudyStatus());
                intent.putExtra("position", "0");
                intent.putExtra("enroll", "" + studyListArrayList.get(0).getSetting().isEnrolling());
                startActivity(intent);
                finish();
              }
            } else {
              Toast.makeText(StandaloneActivity.this, errormsg, Toast.LENGTH_LONG).show();
              finish();
            }
          }
        });
      }
    });
  }

    private void setSpecificStudy(Study data) {
        if (data != null) {
            study = data;
            for (int i = 0; i < study.getStudies().size(); i++) {
                if (study.getStudies().get(i).getStudyId().equalsIgnoreCase(AppConfig.StudyId)) {
                    studyListArrayList.add(study.getStudies().get(i));
                    study.setStudies(studyListArrayList);
                }
            }
            AppController.getHelperProgressDialog().dismissDialog();
            if (!studyListArrayList.isEmpty()) {
                if (studyListArrayList.get(0).getStatus().equalsIgnoreCase(ACTIVE)) {
                    AppController.getHelperProgressDialog()
                            .showProgress(StandaloneActivity.this, "", "", false);

                    dbServiceSubscriber.saveStudyListToDB(this, study);

                    HashMap<String, String> header = new HashMap();
                    header.put(
                            "Authorization",
                            "Bearer "
                                    + AppController.getHelperSharedPreference()
                                    .readPreference(
                                            StandaloneActivity.this, getResources().getString(R.string.auth), ""));
                    header.put(
                            "userId",
                            AppController.getHelperSharedPreference()
                                    .readPreference(
                                            StandaloneActivity.this, getResources().getString(R.string.userid), ""));
                    header.put("deviceType", android.os.Build.MODEL);
                    header.put("deviceOS", Build.VERSION.RELEASE);
                    header.put("mobilePlatform", "ANDROID");

         /* ParticipantEnrollmentDatastoreConfigEvent participantEnrollmentDatastoreConfigEvent =
                  new ParticipantEnrollmentDatastoreConfigEvent(
                          "get",
                          Urls.STUDY_STATE,
                          GET_PREFERENCES,
                          StandaloneActivity.this,
                          StudyData.class,
                          null,
                          header,
                          null,
                          false,
                          this);
          GetPreferenceEvent getPreferenceEvent = new GetPreferenceEvent();
          getPreferenceEvent.setParticipantEnrollmentDatastoreConfigEvent(
                  participantEnrollmentDatastoreConfigEvent);
          UserModulePresenter userModulePresenter = new UserModulePresenter();
          userModulePresenter.performGetUserPreference(getPreferenceEvent);*/
          studyStateApiCall(header);
        } else {
          Toast.makeText(
                          this,
                          "This study is " + studyListArrayList.get(0).getStatus(),
                          Toast.LENGTH_SHORT)
                  .show();
          finish();
        }
      } else {
        Toast.makeText(this, "Study not found", Toast.LENGTH_SHORT).show();
        finish();
      }
    } else {
      AppController.getHelperProgressDialog().dismissDialog();
      Toast.makeText(StandaloneActivity.this, R.string.error_retriving_data, Toast.LENGTH_SHORT)
              .show();
      finish();
    }
  }

  private void studyStateApiCall(HashMap<String, String> header) {
    enrollmentDataStoreInterface = new ServiceManager()
        .createService(EnrollmentDataStoreInterface.class, UrlTypeConstants.EnrollmentDataStore);
    NetworkRequest.performAsyncRequest(enrollmentDataStoreInterface.getStudyState(header),
        (dataResponse) -> {
          try {
            setStudyState(dataResponse);
          } catch (Exception e) {
            Log.e("TAG", e.getMessage());
          }
        }, (error) -> {
          code = AppController.getErrorCode(error);
          errormsg = AppController.getErrorMessage(error);
          AppController.getHelperProgressDialog().dismissDialog();
          if (code == 401 && errormsg.equalsIgnoreCase("Unauthorized or Invalid token")) {
            AppController.checkRefreshToken(StandaloneActivity.this, new AppController.RefreshTokenListener() {
              @Override
              public void onRefreshTokenCompleted(String result) {
                if (result.equalsIgnoreCase("sucess")) {
                  header.put(
                      "Authorization",
                      "Bearer "
                          + AppController.getHelperSharedPreference()
                          .readPreference(StandaloneActivity.this, getResources().getString(R.string.auth), ""));
                  studyStateApiCall(header);
                } else {
                  AppController.getHelperProgressDialog().dismissDialog();
                  Toast.makeText(StandaloneActivity.this, "session expired", Toast.LENGTH_LONG).show();
                  AppController.getHelperSessionExpired(StandaloneActivity.this, "");
                }
              }
            }, UrlTypeConstants.EnrollmentDataStore);
          }
        });
  }

  private void setStudyState(StudyData dataResponse) {
    this.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        studies = dataResponse;
        AppController.getHelperProgressDialog().dismissDialog();
        boolean userAlreadyJoined = false;
        studiesArrayList = studies.getStudies();
        if (studies != null) {
          studies.setUserId(
              AppController.getHelperSharedPreference()
                  .readPreference(StandaloneActivity.this, getString(R.string.userid), ""));
          dbServiceSubscriber.saveStudyPreferencesToDB(StandaloneActivity.this, studies);

          AppController.getHelperSharedPreference()
              .writePreference(
                  StandaloneActivity.this,
                  getString(R.string.title),
                  "" + studyListArrayList.get(0).getTitle());
          AppController.getHelperSharedPreference()
              .writePreference(
                  StandaloneActivity.this,
                  getString(R.string.status),
                  "" + studyListArrayList.get(0).getStatus());
          if (!studies.getStudies().isEmpty()) {
            AppController.getHelperSharedPreference()
                .writePreference(
                    StandaloneActivity.this,
                    getString(R.string.studyStatus),
                    "" + studies.getStudies().get(0).getStatus());
          } else {
            AppController.getHelperSharedPreference()
                .writePreference(
                    StandaloneActivity.this, getString(R.string.studyStatus), YET_TO_JOIN);
          }
          AppController.getHelperSharedPreference()
              .writePreference(StandaloneActivity.this, getString(R.string.position), "" + 0);
          AppController.getHelperSharedPreference()
              .writePreference(
                  StandaloneActivity.this,
                  getString(R.string.enroll),
                  "" + studyListArrayList.get(0).getSetting().isEnrolling());
          AppController.getHelperSharedPreference()
              .writePreference(
                  StandaloneActivity.this,
                  getString(R.string.studyVersion),
                  "" + studyListArrayList.get(0).getStudyVersion());

          RealmList<Studies> userPreferenceStudies = studies.getStudies();
          if (userPreferenceStudies != null) {
            for (int i = 0; i < userPreferenceStudies.size(); i++) {
              for (int j = 0; j < studyListArrayList.size(); j++) {
                if (userPreferenceStudies
                    .get(i)
                    .getStudyId()
                    .equalsIgnoreCase(studyListArrayList.get(j).getStudyId())) {
                  studyListArrayList.get(j).setStudyStatus(userPreferenceStudies.get(i).getStatus());
                  userAlreadyJoined = true;

                  if (studyListArrayList.get(j).getStudyStatus().equalsIgnoreCase(IN_PROGRESS)) {
                    getStudyUpdate(
                        studyListArrayList.get(j).getStudyId(),
                        studyListArrayList.get(j).getStudyVersion(),
                        studyListArrayList.get(j).getTitle(),
                        "",
                        "",
                        "",
                        "");
                  } else {
                    Intent intent =
                        new Intent(StandaloneActivity.this, StandaloneStudyInfoActivity.class);
                    intent.putExtra("flow", getIntent().getStringExtra("flow"));
                    intent.putExtra("studyId", studyListArrayList.get(j).getStudyId());
                    intent.putExtra("title", studyListArrayList.get(j).getTitle());
                    intent.putExtra("status", studyListArrayList.get(j).getStatus());
                    intent.putExtra("studyStatus", studyListArrayList.get(j).getStudyStatus());
                    intent.putExtra("position", "0");
                    intent.putExtra(
                        "enroll", "" + studyListArrayList.get(j).getSetting().isEnrolling());
                    startActivity(intent);
                    finish();
                  }
                  break;
                }
              }
            }
            if (!userAlreadyJoined && !studyListArrayList.isEmpty()) {

              studyListArrayList.get(0).setStudyStatus(YET_TO_JOIN);

              Intent intent = new Intent(StandaloneActivity.this, StandaloneStudyInfoActivity.class);
              intent.putExtra("flow", getIntent().getStringExtra("flow"));
              intent.putExtra("studyId", studyListArrayList.get(0).getStudyId());
              intent.putExtra("title", studyListArrayList.get(0).getTitle());
              intent.putExtra("status", studyListArrayList.get(0).getStatus());
              intent.putExtra("studyStatus", studyListArrayList.get(0).getStudyStatus());
              intent.putExtra("position", "0");
              intent.putExtra("enroll", "" + studyListArrayList.get(0).getSetting().isEnrolling());
              startActivity(intent);
              finish();
            }
          } else {
            Toast.makeText(StandaloneActivity.this, R.string.error_retriving_data, Toast.LENGTH_SHORT)
                .show();
            finish();
          }
          setStudyList(false);
          checkForNotification(getIntent());
          AppController.getHelperProgressDialog().dismissDialog();
        }
      }
    });
  }


  @Override
  public <T> void asyncResponse(T response, int responseCode) {
   /* if (responseCode == SPECIFIC_STUDY) {
      if (response != null) {
        study = (Study) response;
        for (int i = 0; i < study.getStudies().size(); i++) {
          if (study.getStudies().get(i).getStudyId().equalsIgnoreCase(AppConfig.StudyId)) {
            studyListArrayList.add(study.getStudies().get(i));
            study.setStudies(studyListArrayList);
          }
        }
        AppController.getHelperProgressDialog().dismissDialog();
        if (!studyListArrayList.isEmpty()) {
          if (studyListArrayList.get(0).getStatus().equalsIgnoreCase(ACTIVE)) {
            AppController.getHelperProgressDialog()
                .showProgress(StandaloneActivity.this, "", "", false);

            dbServiceSubscriber.saveStudyListToDB(this, study);

            HashMap<String, String> header = new HashMap();
            header.put(
                "Authorization",
                "Bearer "
                    + AppController.getHelperSharedPreference()
                        .readPreference(
                            StandaloneActivity.this, getResources().getString(R.string.auth), ""));
            header.put(
                "userId",
                AppController.getHelperSharedPreference()
                    .readPreference(
                        StandaloneActivity.this, getResources().getString(R.string.userid), ""));
            header.put("deviceType", android.os.Build.MODEL);
            header.put("deviceOS", Build.VERSION.RELEASE);
            header.put("mobilePlatform", "ANDROID");

            ParticipantEnrollmentDatastoreConfigEvent participantEnrollmentDatastoreConfigEvent =
                new ParticipantEnrollmentDatastoreConfigEvent(
                    "get",
                    Urls.STUDY_STATE,
                    GET_PREFERENCES,
                    StandaloneActivity.this,
                    StudyData.class,
                    null,
                    header,
                    null,
                    false,
                    this);
            GetPreferenceEvent getPreferenceEvent = new GetPreferenceEvent();
            getPreferenceEvent.setParticipantEnrollmentDatastoreConfigEvent(
                participantEnrollmentDatastoreConfigEvent);
            UserModulePresenter userModulePresenter = new UserModulePresenter();
            userModulePresenter.performGetUserPreference(getPreferenceEvent);
          } else {
            Toast.makeText(
                    this,
                    "This study is " + studyListArrayList.get(0).getStatus(),
                    Toast.LENGTH_SHORT)
                .show();
            finish();
          }
        } else {
          Toast.makeText(this, "Study not found", Toast.LENGTH_SHORT).show();
          finish();
        }
      } else {
        AppController.getHelperProgressDialog().dismissDialog();
        Toast.makeText(StandaloneActivity.this, R.string.error_retriving_data, Toast.LENGTH_SHORT)
            .show();
        finish();
      }
    } else */
    if (responseCode == GET_PREFERENCES) {
      StudyData studies = (StudyData) response;
      boolean userAlreadyJoined = false;
      studiesArrayList = studies.getStudies();
      if (studies != null) {
        studies.setUserId(
            AppController.getHelperSharedPreference()
                .readPreference(StandaloneActivity.this, getString(R.string.userid), ""));
        dbServiceSubscriber.saveStudyPreferencesToDB(StandaloneActivity.this, studies);

        AppController.getHelperSharedPreference()
            .writePreference(
                StandaloneActivity.this,
                getString(R.string.title),
                "" + studyListArrayList.get(0).getTitle());
        AppController.getHelperSharedPreference()
            .writePreference(
                StandaloneActivity.this,
                getString(R.string.status),
                "" + studyListArrayList.get(0).getStatus());
        if (!studies.getStudies().isEmpty()) {
          AppController.getHelperSharedPreference()
              .writePreference(
                  StandaloneActivity.this,
                  getString(R.string.studyStatus),
                  "" + studies.getStudies().get(0).getStatus());
        } else {
          AppController.getHelperSharedPreference()
              .writePreference(
                  StandaloneActivity.this, getString(R.string.studyStatus), YET_TO_JOIN);
        }
        AppController.getHelperSharedPreference()
            .writePreference(StandaloneActivity.this, getString(R.string.position), "" + 0);
        AppController.getHelperSharedPreference()
            .writePreference(
                StandaloneActivity.this,
                getString(R.string.enroll),
                "" + studyListArrayList.get(0).getSetting().isEnrolling());
        AppController.getHelperSharedPreference()
            .writePreference(
                StandaloneActivity.this,
                getString(R.string.studyVersion),
                "" + studyListArrayList.get(0).getStudyVersion());

        RealmList<Studies> userPreferenceStudies = studies.getStudies();
        if (userPreferenceStudies != null) {
          for (int i = 0; i < userPreferenceStudies.size(); i++) {
            for (int j = 0; j < studyListArrayList.size(); j++) {
              if (userPreferenceStudies
                  .get(i)
                  .getStudyId()
                  .equalsIgnoreCase(studyListArrayList.get(j).getStudyId())) {
                studyListArrayList.get(j).setStudyStatus(userPreferenceStudies.get(i).getStatus());
                userAlreadyJoined = true;

                if (studyListArrayList.get(j).getStudyStatus().equalsIgnoreCase(IN_PROGRESS)) {
                  getStudyUpdate(
                      studyListArrayList.get(j).getStudyId(),
                      studyListArrayList.get(j).getStudyVersion(),
                      studyListArrayList.get(j).getTitle(),
                      "",
                      "",
                      "",
                      "");
                } else {
                  Intent intent =
                      new Intent(StandaloneActivity.this, StandaloneStudyInfoActivity.class);
                  intent.putExtra("flow", getIntent().getStringExtra("flow"));
                  intent.putExtra("studyId", studyListArrayList.get(j).getStudyId());
                  intent.putExtra("title", studyListArrayList.get(j).getTitle());
                  intent.putExtra("status", studyListArrayList.get(j).getStatus());
                  intent.putExtra("studyStatus", studyListArrayList.get(j).getStudyStatus());
                  intent.putExtra("position", "0");
                  intent.putExtra(
                      "enroll", "" + studyListArrayList.get(j).getSetting().isEnrolling());
                  startActivity(intent);
                  finish();
                }
                break;
              }
            }
          }
          if (!userAlreadyJoined && !studyListArrayList.isEmpty()) {

            studyListArrayList.get(0).setStudyStatus(YET_TO_JOIN);

            Intent intent = new Intent(StandaloneActivity.this, StandaloneStudyInfoActivity.class);
            intent.putExtra("flow", getIntent().getStringExtra("flow"));
            intent.putExtra("studyId", studyListArrayList.get(0).getStudyId());
            intent.putExtra("title", studyListArrayList.get(0).getTitle());
            intent.putExtra("status", studyListArrayList.get(0).getStatus());
            intent.putExtra("studyStatus", studyListArrayList.get(0).getStudyStatus());
            intent.putExtra("position", "0");
            intent.putExtra("enroll", "" + studyListArrayList.get(0).getSetting().isEnrolling());
            startActivity(intent);
            finish();
          }
        } else {
          Toast.makeText(StandaloneActivity.this, R.string.error_retriving_data, Toast.LENGTH_SHORT)
              .show();
          finish();
        }
        setStudyList(false);
        checkForNotification(getIntent());
        AppController.getHelperProgressDialog().dismissDialog();
      } else {
        AppController.getHelperProgressDialog().dismissDialog();
        Toast.makeText(StandaloneActivity.this, R.string.error_retriving_data, Toast.LENGTH_SHORT)
            .show();
        finish();
      }
    } /*else if (responseCode == STUDY_UPDATES) {
      StudyUpdate studyUpdate = (StudyUpdate) response;
      studyUpdate.setStudyId(studyId);
      StudyUpdateListdata studyUpdateListdata = new StudyUpdateListdata();
      RealmList<StudyUpdate> studyUpdates = new RealmList<>();
      studyUpdates.add(studyUpdate);
      studyUpdateListdata.setStudyUpdates(studyUpdates);
      dbServiceSubscriber.saveStudyUpdateListdataToDB(this, studyUpdateListdata);

      if (studyUpdate.getStudyUpdateData().isResources()) {
        dbServiceSubscriber.deleteResourcesFromDb(this, studyId);
      }
      if (studyUpdate.getStudyUpdateData().isInfo()) {
        dbServiceSubscriber.deleteStudyInfoFromDb(this, studyId);
      }
      if (studyUpdate.getStudyUpdateData().isConsent() && studyUpdate.isEnrollAgain()) {
        callConsentMetaDataWebservice();
      } else {
        AppController.getHelperProgressDialog().dismissDialog();
        Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
        addClearTopFlag(intent);
        intent.putExtra("studyId", studyId);
        intent.putExtra("to", calledFor);
        intent.putExtra("from", from);
        intent.putExtra("activityId", activityId);
        intent.putExtra("localNotification", localNotification);
        startActivity(intent);
        finish();
      }
    } */ else if (responseCode == GET_CONSENT_DOC) {
      ConsentDocumentData consentDocumentData = (ConsentDocumentData) response;
      consentDocumentData.setStudyId(studyId);
      dbServiceSubscriber.saveConsentDocumentToDB(StandaloneActivity.this, consentDocumentData);
      latestConsentVersion = consentDocumentData.getConsent().getVersion();
      enrollAgain = consentDocumentData.isEnrollAgain();
      callGetConsentPdfWebservice();

    } else if (responseCode == CONSENTPDF) {
      String version = "";
      ConsentPDF consentPdfData = (ConsentPDF) response;
      StudyData studyData = dbServiceSubscriber.getStudyPreferencesListFromDB(realm);
      for (int i = 0; i < studyData.getStudies().size(); i++) {
        if (studyData.getStudies().get(i).getStudyId().equalsIgnoreCase(studyId)) {
          version = studyData.getStudies().get(i).getUserStudyVersion();
        }
      }
      if (version != null && (!latestConsentVersion.equalsIgnoreCase(version))) {
        callConsentMetaDataWebservice();
      } else if (enrollAgain
          && latestConsentVersion != null
          && consentPdfData != null
          && consentPdfData.getConsent() != null
          && consentPdfData.getConsent().getVersion() != null) {
        if (!consentPdfData.getConsent().getVersion().equalsIgnoreCase(latestConsentVersion)) {
          callConsentMetaDataWebservice();
        } else {
          AppController.getHelperProgressDialog().dismissDialog();
          Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
          addClearTopFlag(intent);
          intent.putExtra("studyId", studyId);
          intent.putExtra("to", calledFor);
          intent.putExtra("from", from);
          intent.putExtra("activityId", activityId);
          intent.putExtra("localNotification", localNotification);
          startActivity(intent);
          finish();
        }
      } else {
        AppController.getHelperProgressDialog().dismissDialog();
        Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
        addClearTopFlag(intent);
        intent.putExtra("studyId", studyId);
        intent.putExtra("to", calledFor);
        intent.putExtra("from", from);
        intent.putExtra("activityId", activityId);
        intent.putExtra("localNotification", localNotification);
        startActivity(intent);
        finish();
      }
    }
  }

  @Override
  public void asyncResponseFailure(int responseCode, String errormsg, String statusCode) {
    AppController.getHelperProgressDialog().dismissDialog();
    if (statusCode.equalsIgnoreCase("401")) {
      Toast.makeText(StandaloneActivity.this, errormsg, Toast.LENGTH_SHORT).show();
      AppController.getHelperSessionExpired(StandaloneActivity.this, errormsg);
    } else if (responseCode == STUDY_UPDATES
        || responseCode == GET_CONSENT_DOC
        || responseCode == CONSENTPDF) {
      Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
      addClearTopFlag(intent);
      intent.putExtra("studyId", studyId);
      intent.putExtra("to", calledFor);
      intent.putExtra("from", from);
      intent.putExtra("activityId", activityId);
      intent.putExtra("localNotification", localNotification);
      startActivity(intent);
      finish();
    } else {

      // offline handling
      study = dbServiceSubscriber.getStudyListFromDB(realm);
      if (study != null && study.getStudies() != null && !study.getStudies().isEmpty()) {
        studyListArrayList = study.getStudies();
        studyListArrayList =
            dbServiceSubscriber.saveStudyStatusToStudyList(studyListArrayList, realm);
        setStudyList(true);

        // like click
        if (studyListArrayList.get(0).getStudyStatus().equalsIgnoreCase(IN_PROGRESS)) {
          getStudyUpdate(
              studyListArrayList.get(0).getStudyId(),
              studyListArrayList.get(0).getStudyVersion(),
              studyListArrayList.get(0).getTitle(),
              "",
              "",
              "",
              "");
        } else {
          Intent intent = new Intent(StandaloneActivity.this, StandaloneStudyInfoActivity.class);
          intent.putExtra("flow", getIntent().getStringExtra("flow"));
          intent.putExtra("studyId", studyListArrayList.get(0).getStudyId());
          intent.putExtra("title", studyListArrayList.get(0).getTitle());
          intent.putExtra("status", studyListArrayList.get(0).getStatus());
          intent.putExtra("studyStatus", studyListArrayList.get(0).getStudyStatus());
          intent.putExtra("position", "0");
          intent.putExtra("enroll", "" + studyListArrayList.get(0).getSetting().isEnrolling());
          startActivity(intent);
          finish();
        }
      } else {
        Toast.makeText(StandaloneActivity.this, errormsg, Toast.LENGTH_LONG).show();
        finish();
      }
    }
  }

  public void checkForNotification(Intent intent1) {
    if (!intentFrom.equalsIgnoreCase("")) {
      intentFrom = "";
      String type = intent1.getStringExtra(AppFirebaseMessagingService.TYPE);
      String subType = intent1.getStringExtra(AppFirebaseMessagingService.SUBTYPE);
      String studyId = intent1.getStringExtra(AppFirebaseMessagingService.STUDYID);
      String audience = intent1.getStringExtra(AppFirebaseMessagingService.AUDIENCE);

      String localNotification = "";
      if (intent1.getStringExtra(AlarmReceiver.LOCAL_NOTIFICATION) != null) {
        localNotification = intent1.getStringExtra(AlarmReceiver.LOCAL_NOTIFICATION);
      }
      String activityIdNotification = "";
      if (intent1.getStringExtra(AlarmReceiver.ACTIVITYID) != null) {
        activityIdNotification = intent1.getStringExtra(AlarmReceiver.ACTIVITYID);
      }

      if (!AppController.getHelperSharedPreference()
          .readPreference(StandaloneActivity.this, getResources().getString(R.string.userid), "")
          .equalsIgnoreCase("")) {
        if (type != null) {
          if (type.equalsIgnoreCase("Gateway")) {
            if (subType.equalsIgnoreCase("Study")) {
              Study study = dbServiceSubscriber.getStudyListFromDB(realm);
              if (study != null) {
                RealmList<StudyList> studyListArrayList = study.getStudies();
                studyListArrayList =
                    dbServiceSubscriber.saveStudyStatusToStudyList(studyListArrayList, realm);
                boolean isStudyAvailable = false;
                for (int i = 0; i < studyListArrayList.size(); i++) {
                  if (studyId.equalsIgnoreCase(studyListArrayList.get(i).getStudyId())) {
                    try {
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this,
                              getString(R.string.title),
                              "" + studyListArrayList.get(i).getTitle());
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this,
                              getString(R.string.status),
                              "" + studyListArrayList.get(i).getStatus());
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this,
                              getString(R.string.studyStatus),
                              "" + studyListArrayList.get(i).getStudyStatus());
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this, getString(R.string.position), "" + i);
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this,
                              getString(R.string.enroll),
                              "" + studyListArrayList.get(i).getSetting().isEnrolling());
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this,
                              getString(R.string.studyVersion),
                              "" + studyListArrayList.get(i).getStudyVersion());
                    } catch (Exception e) {
                      Logger.log(e);
                    }
                    if (studyListArrayList
                        .get(i)
                        .getStatus()
                        .equalsIgnoreCase(getString(R.string.active))
                        && studyListArrayList
                        .get(i)
                        .getStudyStatus()
                        .equalsIgnoreCase(StudyFragment.IN_PROGRESS)) {
                      Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
                      addClearTopFlag(intent);
                      intent.putExtra("studyId", studyId);
                      startActivity(intent);
                      finish();
                    } else if (studyListArrayList
                        .get(i)
                        .getStatus()
                        .equalsIgnoreCase(getString(R.string.paused))) {
                      Toast.makeText(
                              StandaloneActivity.this, R.string.study_paused, Toast.LENGTH_SHORT)
                          .show();
                      finish();
                    } else if (studyListArrayList
                        .get(i)
                        .getStatus()
                        .equalsIgnoreCase(getString(R.string.closed))) {
                      Toast.makeText(
                              StandaloneActivity.this, R.string.study_resume, Toast.LENGTH_SHORT)
                          .show();
                      finish();
                    } else {
                      Intent intent =
                          new Intent(getApplicationContext(), StandaloneStudyInfoActivity.class);
                      intent.putExtra("flow", getIntent().getStringExtra("flow"));
                      intent.putExtra("studyId", studyListArrayList.get(i).getStudyId());
                      intent.putExtra("title", studyListArrayList.get(i).getTitle());
                      intent.putExtra("status", studyListArrayList.get(i).getStatus());
                      intent.putExtra("studyStatus", studyListArrayList.get(i).getStudyStatus());
                      intent.putExtra("position", "" + i);
                      intent.putExtra(
                          "enroll", "" + studyListArrayList.get(i).getSetting().isEnrolling());
                      startActivity(intent);
                      finish();
                    }
                    isStudyAvailable = true;
                    break;
                  }
                }
                if (!isStudyAvailable) {
                  Toast.makeText(
                          StandaloneActivity.this, R.string.studyNotAvailable, Toast.LENGTH_SHORT)
                      .show();
                  finish();
                }
              } else {
                Toast.makeText(
                        StandaloneActivity.this, R.string.studyNotAvailable, Toast.LENGTH_SHORT)
                    .show();
                finish();
              }
            }
          } else if (type.equalsIgnoreCase("Study")) {
            if (subType.equalsIgnoreCase("Activity") || subType.equalsIgnoreCase("Resource")) {
              Study study = dbServiceSubscriber.getStudyListFromDB(realm);
              if (study != null) {
                RealmList<StudyList> studyListArrayList = study.getStudies();
                studyListArrayList =
                    dbServiceSubscriber.saveStudyStatusToStudyList(studyListArrayList, realm);
                boolean isStudyAvailable = false;
                boolean isStudyJoined = false;
                for (int i = 0; i < studyListArrayList.size(); i++) {
                  if (studyId.equalsIgnoreCase(studyListArrayList.get(i).getStudyId())) {
                    isStudyAvailable = true;
                    try {
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this,
                              getString(R.string.title),
                              "" + studyListArrayList.get(i).getTitle());
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this,
                              getString(R.string.status),
                              "" + studyListArrayList.get(i).getStatus());
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this,
                              getString(R.string.studyStatus),
                              "" + studyListArrayList.get(i).getStudyStatus());
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this, getString(R.string.position), "" + i);
                      AppController.getHelperSharedPreference()
                          .writePreference(
                              StandaloneActivity.this,
                              getString(R.string.enroll),
                              "" + studyListArrayList.get(i).getSetting().isEnrolling());
                    } catch (Exception e) {
                      Logger.log(e);
                    }
                    if (studyListArrayList
                        .get(i)
                        .getStatus()
                        .equalsIgnoreCase(getString(R.string.active))
                        && studyListArrayList
                        .get(i)
                        .getStudyStatus()
                        .equalsIgnoreCase(StudyFragment.IN_PROGRESS)) {
                      if (subType.equalsIgnoreCase("Resource")) {
                        getStudyUpdate(
                            studyListArrayList.get(i).getStudyId(),
                            studyListArrayList.get(i).getStudyVersion(),
                            studyListArrayList.get(i).getTitle(),
                            "Resource",
                            "NotificationActivity",
                            activityIdNotification,
                            localNotification);
                      } else {
                        getStudyUpdate(
                            studyListArrayList.get(i).getStudyId(),
                            studyListArrayList.get(i).getStudyVersion(),
                            studyListArrayList.get(i).getTitle(),
                            "",
                            "NotificationActivity",
                            activityIdNotification,
                            localNotification);
                      }
                      isStudyJoined = true;
                      break;
                    } else {
                      isStudyJoined = false;
                      break;
                    }
                  }
                }
                if (!isStudyAvailable) {
                  Toast.makeText(
                          StandaloneActivity.this, R.string.studyNotAvailable, Toast.LENGTH_SHORT)
                      .show();
                  finish();
                } else if (!isStudyJoined) {
                  Toast.makeText(
                          StandaloneActivity.this, R.string.studyNotJoined, Toast.LENGTH_SHORT)
                      .show();
                  finish();
                }
              } else {
                Toast.makeText(
                        StandaloneActivity.this, R.string.studyNotAvailable, Toast.LENGTH_SHORT)
                    .show();
                finish();
              }
            }
          }
        }
      } else {
        Toast.makeText(StandaloneActivity.this, R.string.studyNotAvailable, Toast.LENGTH_SHORT)
            .show();
        finish();
      }
    }
  }

  public void getStudyUpdate(
      String studyId,
      String studyVersion,
      String title,
      String calledFor,
      String from,
      String activityId,
      String localNotification) {

    this.from = from;
    this.title = title;
    this.studyId = studyId;
    this.activityId = activityId;
    this.localNotification = localNotification;
    this.calledFor = calledFor;

    StudyData studyData = dbServiceSubscriber.getStudyPreferences(realm);
    Studies studies = null;
    if (studyData != null && studyData.getStudies() != null) {
      for (int i = 0; i < studyData.getStudies().size(); i++) {
        if (studyData.getStudies().get(i).getStudyId().equalsIgnoreCase(studyId)) {
          studies = studyData.getStudies().get(i);
        }
      }
    }
    if (studies != null
        && studies.getVersion() != null
        && !studies.getVersion().equalsIgnoreCase(studyVersion)) {
      getStudyUpdateFomWS(studyId, studies.getVersion());
    } else {
      getCurrentConsentDocument(studyId);
    }
  }

  private void getStudyUpdateFomWS(String studyId, String studyVersion) {
    AppController.getHelperProgressDialog().showProgress(StandaloneActivity.this, "", "", false);
    /*GetUserStudyListEvent getUserStudyListEvent = new GetUserStudyListEvent();
    HashMap<String, String> header = new HashMap();
    String url = Urls.STUDY_UPDATES + "?studyId=" + studyId + "&studyVersion=" + studyVersion;
    StudyDatastoreConfigEvent studyDatastoreConfigEvent =
        new StudyDatastoreConfigEvent(
            "get",
            url,
            STUDY_UPDATES,
            StandaloneActivity.this,
            StudyUpdate.class,
            null,
            header,
            null,
            false,
            this);

    getUserStudyListEvent.setStudyDatastoreConfigEvent(studyDatastoreConfigEvent);
    StudyModulePresenter studyModulePresenter = new StudyModulePresenter();
    studyModulePresenter.performGetGateWayStudyList(getUserStudyListEvent);*/
    apiInterface = new ServiceManager().createService(StudyDataStoreAPIInterface.class, UrlTypeConstants.StudyDataStore);
    mSBNetworkSubscriptionl = NetworkRequest.performAsyncRequest(apiInterface.getStudyUpdates(studyId, studyVersion), (data) -> {
      try {
        setStudyUpdates(data);
      } catch (Exception e) {
        Log.e("TAG", "error: " + e.getMessage());
      }
    }, (error) -> {
      code = AppController.getErrorCode(error);
      errormsg = AppController.getErrorMessage(error);
      if (code == 401 && errormsg.equalsIgnoreCase("Unauthorized or Invalid token")) {
        AppController.checkRefreshToken(StandaloneActivity.this, new AppController.RefreshTokenListener() {
          @Override
          public void onRefreshTokenCompleted(String result) {
            if (result.equalsIgnoreCase("sucess")) {
              getStudyUpdateFomWS(studyId, studyVersion);
            } else {
              AppController.getHelperProgressDialog().dismissDialog();
              Toast.makeText(StandaloneActivity.this, "session expired", Toast.LENGTH_LONG).show();
              AppController.getHelperSessionExpired(StandaloneActivity.this, "");
            }
          }
        }, UrlTypeConstants.StudyDataStore);
      } else {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            AppController.getHelperProgressDialog().dismissDialog();
            // offline handling
            study = dbServiceSubscriber.getStudyListFromDB(realm);
            if (study != null && study.getStudies() != null && !study.getStudies().isEmpty()) {
              studyListArrayList = study.getStudies();
              studyListArrayList =
                  dbServiceSubscriber.saveStudyStatusToStudyList(studyListArrayList, realm);
              setStudyList(true);

              // like click
              if (studyListArrayList.get(0).getStudyStatus().equalsIgnoreCase(IN_PROGRESS)) {
                getStudyUpdate(
                    studyListArrayList.get(0).getStudyId(),
                    studyListArrayList.get(0).getStudyVersion(),
                    studyListArrayList.get(0).getTitle(),
                    "",
                    "",
                    "",
                    "");
              } else {
                Intent intent = new Intent(StandaloneActivity.this, StandaloneStudyInfoActivity.class);
                intent.putExtra("flow", getIntent().getStringExtra("flow"));
                intent.putExtra("studyId", studyListArrayList.get(0).getStudyId());
                intent.putExtra("title", studyListArrayList.get(0).getTitle());
                intent.putExtra("status", studyListArrayList.get(0).getStatus());
                intent.putExtra("studyStatus", studyListArrayList.get(0).getStudyStatus());
                intent.putExtra("position", "0");
                intent.putExtra("enroll", "" + studyListArrayList.get(0).getSetting().isEnrolling());
                startActivity(intent);
                finish();
              }
            } else {
              Toast.makeText(StandaloneActivity.this, errormsg, Toast.LENGTH_LONG).show();
              finish();
            }

          }
        });
      }
    });
  }

  private void setStudyUpdates(StudyUpdate studyUpdateResponse) {
    studyUpdate = studyUpdateResponse;
    studyUpdate.setStudyId(studyId);
    StudyUpdateListdata studyUpdateListdata = new StudyUpdateListdata();
    RealmList<StudyUpdate> studyUpdates = new RealmList<>();
    studyUpdates.add(studyUpdate);
    studyUpdateListdata.setStudyUpdates(studyUpdates);
    dbServiceSubscriber.saveStudyUpdateListdataToDB(StandaloneActivity.this, studyUpdateListdata);

    if (studyUpdate.getStudyUpdateData().isResources()) {
      dbServiceSubscriber.deleteResourcesFromDb(StandaloneActivity.this, studyId);
    }
    if (studyUpdate.getStudyUpdateData().isInfo()) {
      dbServiceSubscriber.deleteStudyInfoFromDb(StandaloneActivity.this, studyId);
    }
    if (studyUpdate.getStudyUpdateData().isConsent() && studyUpdate.isEnrollAgain()) {
      callConsentMetaDataWebservice();
    } else {
      AppController.getHelperProgressDialog().dismissDialog();
      Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
      addClearTopFlag(intent);
      intent.putExtra("studyId", studyId);
      intent.putExtra("to", calledFor);
      intent.putExtra("from", from);
      intent.putExtra("activityId", activityId);
      intent.putExtra("localNotification", localNotification);
      startActivity(intent);
      finish();
    }
  }

  private void getCurrentConsentDocument(String studyId) {
    HashMap<String, String> header = new HashMap<>();
       /* String url =
                Urls.GET_CONSENT_DOC
                        + "?studyId="
                        + studyId
                        + "&consentVersion=&activityId=&activityVersion=";
        AppController.getHelperProgressDialog().showProgress(StandaloneActivity.this, "", "", false);
    GetUserStudyInfoEvent getUserStudyInfoEvent = new GetUserStudyInfoEvent();
    StudyDatastoreConfigEvent studyDatastoreConfigEvent =
        new StudyDatastoreConfigEvent(
            "get",
            url,
            GET_CONSENT_DOC,
            StandaloneActivity.this,
            ConsentDocumentData.class,
            null,
            header,
            null,
            false,
            StandaloneActivity.this);

    getUserStudyInfoEvent.setStudyDatastoreConfigEvent(studyDatastoreConfigEvent);
    StudyModulePresenter studyModulePresenter = new StudyModulePresenter();
    studyModulePresenter.performGetGateWayStudyInfo(getUserStudyInfoEvent);*/
    apiInterface = new ServiceManager().createService(StudyDataStoreAPIInterface.class, UrlTypeConstants.StudyDataStore);
    mSBNetworkSubscriptionl = NetworkRequest.performAsyncRequest(apiInterface
            .getConsentDocument(studyId, "", "", ""),
        (data) -> {
          setConsentDocument(data);
        }, (error) -> {
          this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              AppController.getHelperProgressDialog().dismissDialog();

              Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
              addClearTopFlag(intent);
              intent.putExtra("studyId", studyId);
              intent.putExtra("to", calledFor);
              intent.putExtra("from", from);
              intent.putExtra("activityId", activityId);
              intent.putExtra("localNotification", localNotification);
              startActivity(intent);
              finish();
            }
          });
        });
  }

  private void setConsentDocument(ConsentDocumentData consentDocumentDataResponse) {
    this.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AppController.getHelperProgressDialog().dismissDialog();

        consentDocumentData = consentDocumentDataResponse;
        consentDocumentData.setStudyId(studyId);
        dbServiceSubscriber.saveConsentDocumentToDB(StandaloneActivity.this, consentDocumentData);
        latestConsentVersion = consentDocumentData.getConsent().getVersion();
        enrollAgain = consentDocumentData.isEnrollAgain();
        callGetConsentPdfWebservice();
      }
    });
  }

  private void callGetConsentPdfWebservice() {
    ConsentPdfEvent consentPdfEvent = new ConsentPdfEvent();
    HashMap<String, String> header = new HashMap<>();
    header.put(
        "Authorization",
        "Bearer "
            + AppController.getHelperSharedPreference()
            .readPreference(
                StandaloneActivity.this, getResources().getString(R.string.auth), ""));
    header.put(
        "userId",
        AppController.getHelperSharedPreference()
            .readPreference(
                StandaloneActivity.this, getResources().getString(R.string.userid), ""));
    /*String url = Urls.CONSENTPDF + "?studyId=" + studyId + "&consentVersion=";
    ParticipantConsentDatastoreConfigEvent participantConsentDatastoreConfigEvent =
        new ParticipantConsentDatastoreConfigEvent(
            "get",
            url,
            CONSENTPDF,
            StandaloneActivity.this,
            ConsentPDF.class,
            null,
            header,
            null,
            false,
            StandaloneActivity.this);
    consentPdfEvent.setParticipantConsentDatastoreConfigEvent(
        participantConsentDatastoreConfigEvent);
    UserModulePresenter userModulePresenter = new UserModulePresenter();
    userModulePresenter.performConsentPdf(consentPdfEvent);*/
    consentPdfApiCall(header, studyId, "");
  }

  private void consentPdfApiCall(HashMap<String, String> header, String
      studyId, String s) {
    consentDataStoreInterface = new ServiceManager().createService(ConsentDataStoreInterface.class, UrlTypeConstants.ConsentDataStore);
    NetworkRequest.performAsyncRequest(consentDataStoreInterface.getConsentDocument(header, studyId, ""),
        (data) -> {
          try {
            getConsentDocument(data);
          } catch (Exception e) {
            Log.e("TAG", e.getMessage());
          }

        }, (error) -> {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              code = AppController.getErrorCode(error);
              errormsg = AppController.getErrorMessage(error);
              if (code == 401 && errormsg.equalsIgnoreCase("Unauthorized or Invalid token")) {
                AppController.checkRefreshToken(StandaloneActivity.this, new AppController.RefreshTokenListener() {
                  @Override
                  public void onRefreshTokenCompleted(String result) {
                    if (result.equalsIgnoreCase("sucess")) {
                      header.put(
                          "Authorization",
                          "Bearer "
                              + AppController.getHelperSharedPreference()
                              .readPreference(StandaloneActivity.this, getResources().getString(R.string.auth), ""));
                      consentPdfApiCall(header, studyId, "");
                    } else {
                      AppController.getHelperProgressDialog().dismissDialog();
                      Toast.makeText(StandaloneActivity.this, "session expired", Toast.LENGTH_LONG).show();
                      AppController.getHelperSessionExpired(StandaloneActivity.this, "");
                    }
                  }
                }, UrlTypeConstants.ConsentDataStore);
              } else {
                AppController.getHelperProgressDialog().dismissDialog();
                Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
                addClearTopFlag(intent);
                intent.putExtra("studyId", studyId);
                intent.putExtra("to", calledFor);
                intent.putExtra("from", from);
                intent.putExtra("activityId", activityId);
                intent.putExtra("localNotification", localNotification);
                startActivity(intent);
                finish();
              }
            }
          });
        });
  }

  private void getConsentDocument(ConsentPDF consentPDFResponse) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        String version = "";
        consentPdfData = consentPDFResponse;
        StudyData studyData = dbServiceSubscriber.getStudyPreferencesListFromDB(realm);
        for (int i = 0; i < studyData.getStudies().size(); i++) {
          if (studyData.getStudies().get(i).getStudyId().equalsIgnoreCase(studyId)) {
            version = studyData.getStudies().get(i).getUserStudyVersion();
          }
        }
        if (version != null && (!latestConsentVersion.equalsIgnoreCase(version))) {
          callConsentMetaDataWebservice();
        } else if (enrollAgain
            && latestConsentVersion != null
            && consentPdfData != null
            && consentPdfData.getConsent() != null
            && consentPdfData.getConsent().getVersion() != null) {
          if (!consentPdfData.getConsent().getVersion().equalsIgnoreCase(latestConsentVersion)) {
            callConsentMetaDataWebservice();
          } else {
            AppController.getHelperProgressDialog().dismissDialog();
            Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
            addClearTopFlag(intent);
            intent.putExtra("studyId", studyId);
            intent.putExtra("to", calledFor);
            intent.putExtra("from", from);
            intent.putExtra("activityId", activityId);
            intent.putExtra("localNotification", localNotification);
            startActivity(intent);
            finish();
          }
        } else {
          AppController.getHelperProgressDialog().dismissDialog();
          Intent intent = new Intent(StandaloneActivity.this, SurveyActivity.class);
          addClearTopFlag(intent);
          intent.putExtra("studyId", studyId);
          intent.putExtra("to", calledFor);
          intent.putExtra("from", from);
          intent.putExtra("activityId", activityId);
          intent.putExtra("localNotification", localNotification);
          startActivity(intent);
          finish();
        }
      }
    });
  }

  private void callConsentMetaDataWebservice() {

    new CallConsentMetaData().execute();
  }

  private class CallConsentMetaData
//      extends AsyncTask<String, Void, String>
  {
    String response = null;
    String responseCode = null;
    Responsemodel responseModel;

    private void execute() {

      Executor executor = Executors.newSingleThreadExecutor();
      Handler handler = new Handler(Looper.myLooper());
      AppController.getHelperProgressDialog().showProgress(StandaloneActivity.this, "", "", false);

      executor.execute(new Runnable() {
        @Override
        public void run() {

          ConnectionDetector connectionDetector = new ConnectionDetector(StandaloneActivity.this);

//         String url = Urls.BASE_URL_STUDY_DATASTORE + Urls.CONSENT_METADATA + "?studyId=" + studyId;
          if (connectionDetector.isConnectingToInternet()) {
            apiInterface.getEligibilityConsnet(studyId).enqueue(new Callback<EligibilityConsent>() {
              @Override
              public void onResponse(Call<EligibilityConsent> call, Response<EligibilityConsent> responseData) {
                responseCode = String.valueOf(responseData.code());
                if (responseCode.equalsIgnoreCase("0") &&
                    responseData.message().equalsIgnoreCase("timeout")) {
                  AppController.getHelperProgressDialog().dismissDialog();
                  Toast.makeText(
                          StandaloneActivity.this,
                          getResources().getString(R.string.connection_timeout),
                          Toast.LENGTH_SHORT)
                      .show();
                } else if (responseCode.equalsIgnoreCase("0") &&
                    responseData.message().equalsIgnoreCase("")) {
                  response = "error";
                } else if (Integer.parseInt(responseCode) >= 201
                    && Integer.parseInt(responseCode) < 300
                    && responseData.message().equalsIgnoreCase("")) {
                  response = "No data";
                } else if (Integer.parseInt(responseCode) >= 400
                    && Integer.parseInt(responseCode) < 500
                    && responseData.message().equalsIgnoreCase("http_not_ok")) {
                  response = "client error";
                } else if (Integer.parseInt(responseCode) >= 500
                    && Integer.parseInt(responseCode) < 600
                    && responseData.message().equalsIgnoreCase("http_not_ok")) {
                  response = "server error";
                } else if (responseData.code() == 401) {
                  AppController.getHelperProgressDialog().dismissDialog();
                  Toast.makeText(StandaloneActivity.this, "session expired", Toast.LENGTH_LONG).show();
                  AppController.getHelperSessionExpired(StandaloneActivity.this, "session expired");
                } else if (responseData.code() == 200
                    && responseData.body() != null) {
                  AppController.getHelperProgressDialog().dismissDialog();
                  EligibilityConsent eligibilityConsent = responseData.body();
                  if (eligibilityConsent != null) {
                    eligibilityConsent.setStudyId(studyId);
                    saveConsentToDB(StandaloneActivity.this, eligibilityConsent);
                    startConsent(
                        eligibilityConsent.getConsent(), eligibilityConsent.getEligibility().getType());
                  } else {
                    Toast.makeText(StandaloneActivity.this, R.string.unable_to_parse, Toast.LENGTH_SHORT).show();
                  }
                } else {
                  AppController.getHelperProgressDialog().dismissDialog();
                  response = getString(R.string.unknown_error);
                  Toast.makeText(StandaloneActivity.this, response, Toast.LENGTH_SHORT).show();
                }
              }

              @Override
              public void onFailure(Call<EligibilityConsent> call, Throwable t) {
              }
            });
          }
             /* apiInterface = new ServiceManager().createService(StudyDataStoreAPIInterface.class, UrlTypeConstants.StudyDataStore);
              mSBNetworkSubscriptionl = NetworkRequest.performAsyncRequest(apiInterface
                              .getEligibilityConsnet(studyId),
                      (data) -> {
                      }, (error) -> {
                          (StandaloneActivity.this).runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  AppController.getHelperProgressDialog().dismissDialog();
                                  setErrorMessage(error,response,responseCode);


                              }
                          });
                      });*/
//            responseModel =
//                HttpRequest.getRequest(url, new HashMap<String, String>(), "STUDY_DATASTORE");
//
//                  responseCode = responseModel.getResponseCode();
//                  response = responseModel.getResponseData();
//                  if (responseCode.equalsIgnoreCase("0") && response.equalsIgnoreCase("timeout")) {
//                      response = "timeout";
//                  } else if (responseCode.equalsIgnoreCase("0") && response.equalsIgnoreCase("")) {
//                      response = "error";
//                  } else if (Integer.parseInt(responseCode) >= 201
//                          && Integer.parseInt(responseCode) < 300
//                          && response.equalsIgnoreCase("")) {
//                      response = "No data";
//                  } else if (Integer.parseInt(responseCode) >= 400
//                          && Integer.parseInt(responseCode) < 500
//                          && response.equalsIgnoreCase("http_not_ok")) {
//                      response = "client error";
//                  } else if (Integer.parseInt(responseCode) >= 500
//                          && Integer.parseInt(responseCode) < 600
//                          && response.equalsIgnoreCase("http_not_ok")) {
//                      response = "server error";
//                  } else if (response.equalsIgnoreCase("http_not_ok")) {
//                      response = "Unknown error";
//                  } else if (Integer.parseInt(responseCode) == HttpURLConnection.HTTP_UNAUTHORIZED) {
//                      response = "session expired";
//                  } else if (Integer.parseInt(responseCode) == HttpURLConnection.HTTP_OK
//                          && !response.equalsIgnoreCase("")) {
//                      response = response;
//                  } else {
//                      response = getString(R.string.unknown_error);
//                  }


//          }
//          return response;
//        }
//          handler.post(new Runnable() {
//            @Override
//            public void run() {
//              AppController.getHelperProgressDialog().dismissDialog();
//
//              if (response != null) {
//                if (response.equalsIgnoreCase("session expired")) {
//                  AppController.getHelperProgressDialog().dismissDialog();
//                  AppController.getHelperSessionExpired(StandaloneActivity.this, "session expired");
//                } else if (response.equalsIgnoreCase("timeout")) {
//                  AppController.getHelperProgressDialog().dismissDialog();
//                  Toast.makeText(
//                          StandaloneActivity.this,
//                          getResources().getString(R.string.connection_timeout),
//                          Toast.LENGTH_SHORT)
//                      .show();
//                } else if (Integer.parseInt(responseCode) == HttpURLConnection.HTTP_OK) {
//
//                  Gson gson =
//                      new GsonBuilder()
//                          .setExclusionStrategies(
//                              new ExclusionStrategy() {
//                                @Override
//                                public boolean shouldSkipField(FieldAttributes f) {
//                                  return f.getDeclaringClass().equals(RealmObject.class);
//                                }
//
//                                @Override
//                                public boolean shouldSkipClass(Class<?> clazz) {
//                                  return false;
//                                }
//                              })
//                          .registerTypeAdapter(
//                              new TypeToken<RealmList<CorrectAnswerString>>() {
//                              }.getType(),
//                              new TypeAdapter<RealmList<CorrectAnswerString>>() {
//
//                                @Override
//                                public void write(JsonWriter out, RealmList<CorrectAnswerString> value)
//                                    throws IOException {
//                                  // Ignore
//                                }
//
//                                @Override
//                                public RealmList<CorrectAnswerString> read(JsonReader in)
//                                    throws IOException {
//                                  RealmList<CorrectAnswerString> list =
//                                      new RealmList<CorrectAnswerString>();
//                                  in.beginArray();
//                                  while (in.hasNext()) {
//                                    CorrectAnswerString surveyObjectString = new CorrectAnswerString();
//                                    surveyObjectString.setAnswer(in.nextString());
//                                    list.add(surveyObjectString);
//                                  }
//                                  in.endArray();
//                                  return list;
//                                }
//                              })
//                          .create();
//                  EligibilityConsent eligibilityConsent = gson.fromJson(response, EligibilityConsent.class);
//                  if (eligibilityConsent != null) {
//                    eligibilityConsent.setStudyId(studyId);
//                    saveConsentToDB(StandaloneActivity.this, eligibilityConsent);
//                    startConsent(
//                        eligibilityConsent.getConsent(), eligibilityConsent.getEligibility().getType());
//                  } else {
//                    Toast.makeText(StandaloneActivity.this, R.string.unable_to_parse, Toast.LENGTH_SHORT)
//                        .show();
//                    finish();
//                  }
//                } else {
//                  AppController.getHelperProgressDialog().dismissDialog();
//                  Toast.makeText(
//                          StandaloneActivity.this,
//                          getResources().getString(R.string.unable_to_retrieve_data),
//                          Toast.LENGTH_SHORT)
//                      .show();
//                  finish();
//                }
//              } else {
//                AppController.getHelperProgressDialog().dismissDialog();
//                Toast.makeText(
//                        StandaloneActivity.this, getString(R.string.unknown_error), Toast.LENGTH_SHORT)
//                    .show();
//                finish();
//              }
            }
          });
        }
//      });
    }
//    @Override
//    protected String doInBackground(String... params) {
//      ConnectionDetector connectionDetector = new ConnectionDetector(StandaloneActivity.this);
//
//      String url = Urls.BASE_URL_STUDY_DATASTORE + Urls.CONSENT_METADATA + "?studyId=" + studyId;
//      if (connectionDetector.isConnectingToInternet()) {
//        responseModel =
//            HttpRequest.getRequest(url, new HashMap<String, String>(), "STUDY_DATASTORE");
//        responseCode = responseModel.getResponseCode();
//        response = responseModel.getResponseData();
//        if (responseCode.equalsIgnoreCase("0") && response.equalsIgnoreCase("timeout")) {
//          response = "timeout";
//        } else if (responseCode.equalsIgnoreCase("0") && response.equalsIgnoreCase("")) {
//          response = "error";
//        } else if (Integer.parseInt(responseCode) >= 201
//            && Integer.parseInt(responseCode) < 300
//            && response.equalsIgnoreCase("")) {
//          response = "No data";
//        } else if (Integer.parseInt(responseCode) >= 400
//            && Integer.parseInt(responseCode) < 500
//            && response.equalsIgnoreCase("http_not_ok")) {
//          response = "client error";
//        } else if (Integer.parseInt(responseCode) >= 500
//            && Integer.parseInt(responseCode) < 600
//            && response.equalsIgnoreCase("http_not_ok")) {
//          response = "server error";
//        } else if (response.equalsIgnoreCase("http_not_ok")) {
//          response = "Unknown error";
//        } else if (Integer.parseInt(responseCode) == HttpURLConnection.HTTP_UNAUTHORIZED) {
//          response = "session expired";
//        } else if (Integer.parseInt(responseCode) == HttpURLConnection.HTTP_OK
//            && !response.equalsIgnoreCase("")) {
//          response = response;
//        } else {
//          response = getString(R.string.unknown_error);
//        }
//      }
//      return response;
//    }

//    @Override
//    protected void onPostExecute(String result) {
//      AppController.getHelperProgressDialog().dismissDialog();
//
//      if (response != null) {
//        if (response.equalsIgnoreCase("session expired")) {
//          AppController.getHelperProgressDialog().dismissDialog();
//          AppController.getHelperSessionExpired(StandaloneActivity.this, "session expired");
//        } else if (response.equalsIgnoreCase("timeout")) {
//          AppController.getHelperProgressDialog().dismissDialog();
//          Toast.makeText(
//                  StandaloneActivity.this,
//                  getResources().getString(R.string.connection_timeout),
//                  Toast.LENGTH_SHORT)
//              .show();
//        } else if (Integer.parseInt(responseCode) == HttpURLConnection.HTTP_OK) {
//
//          Gson gson =
//              new GsonBuilder()
//                  .setExclusionStrategies(
//                      new ExclusionStrategy() {
//                        @Override
//                        public boolean shouldSkipField(FieldAttributes f) {
//                          return f.getDeclaringClass().equals(RealmObject.class);
//                        }
//
//                        @Override
//                        public boolean shouldSkipClass(Class<?> clazz) {
//                          return false;
//                        }
//                      })
//                  .registerTypeAdapter(
//                      new TypeToken<RealmList<CorrectAnswerString>>() {}.getType(),
//                      new TypeAdapter<RealmList<CorrectAnswerString>>() {
//
//                        @Override
//                        public void write(JsonWriter out, RealmList<CorrectAnswerString> value)
//                            throws IOException {
//                          // Ignore
//                        }
//
//                        @Override
//                        public RealmList<CorrectAnswerString> read(JsonReader in)
//                            throws IOException {
//                          RealmList<CorrectAnswerString> list =
//                              new RealmList<CorrectAnswerString>();
//                          in.beginArray();
//                          while (in.hasNext()) {
//                            CorrectAnswerString surveyObjectString = new CorrectAnswerString();
//                            surveyObjectString.setAnswer(in.nextString());
//                            list.add(surveyObjectString);
//                          }
//                          in.endArray();
//                          return list;
//                        }
//                      })
//                  .create();
//          EligibilityConsent eligibilityConsent = gson.fromJson(response, EligibilityConsent.class);
//          if (eligibilityConsent != null) {
//            eligibilityConsent.setStudyId(studyId);
//            saveConsentToDB(StandaloneActivity.this, eligibilityConsent);
//            startConsent(
//                eligibilityConsent.getConsent(), eligibilityConsent.getEligibility().getType());
//          } else {
//            Toast.makeText(StandaloneActivity.this, R.string.unable_to_parse, Toast.LENGTH_SHORT)
//                .show();
//            finish();
//          }
//        } else {
//          AppController.getHelperProgressDialog().dismissDialog();
//          Toast.makeText(
//                  StandaloneActivity.this,
//                  getResources().getString(R.string.unable_to_retrieve_data),
//                  Toast.LENGTH_SHORT)
//              .show();
//          finish();
//        }
//      } else {
//        AppController.getHelperProgressDialog().dismissDialog();
//        Toast.makeText(
//                StandaloneActivity.this, getString(R.string.unknown_error), Toast.LENGTH_SHORT)
//            .show();
//        finish();
//      }
//    }

//    @Override
//    protected void onPreExecute() {
//      AppController.getHelperProgressDialog().showProgress(StandaloneActivity.this, "", "", false);
//    }
//  }

    private void setErrorMessage(Throwable error, String response, String responseCode) {
        responseCode = String.valueOf(AppController.getErrorCode(error));
        response = AppController.getErrorMessage(error);
        if (responseCode.equalsIgnoreCase("0") && response.equalsIgnoreCase("timeout")) {
            response = "timeout";
        } else if (responseCode.equalsIgnoreCase("0") && response.equalsIgnoreCase("")) {
            response = "error";
        } else if (Integer.parseInt(responseCode) >= 201
                && Integer.parseInt(responseCode) < 300
                && response.equalsIgnoreCase("")) {
            response = "No data";
        } else if (Integer.parseInt(responseCode) >= 400
                && Integer.parseInt(responseCode) < 500
                && response.equalsIgnoreCase("http_not_ok")) {
            response = "client error";
        } else if (Integer.parseInt(responseCode) >= 500
                && Integer.parseInt(responseCode) < 600
                && response.equalsIgnoreCase("http_not_ok")) {
            response = "server error";
        } else if (response.equalsIgnoreCase("http_not_ok")) {
            response = "Unknown error";
        } else if (Integer.parseInt(responseCode) == HttpURLConnection.HTTP_UNAUTHORIZED) {
            response = "session expired";
        } else if (Integer.parseInt(responseCode) == HttpURLConnection.HTTP_OK
                && !response.equalsIgnoreCase("")) {
            response = response;
        } else {
            response = getString(R.string.unknown_error);
        }
    }

    private void saveConsentToDB(Context context, EligibilityConsent eligibilityConsent) {
    DatabaseEvent databaseEvent = new DatabaseEvent();
    databaseEvent.setE(eligibilityConsent);
    databaseEvent.setType(DbServiceSubscriber.TYPE_COPY_UPDATE);
    databaseEvent.setaClass(EligibilityConsent.class);
    databaseEvent.setOperation(DbServiceSubscriber.INSERT_AND_UPDATE_OPERATION);
    dbServiceSubscriber.insert(context, databaseEvent);
  }

  private void startConsent(Consent consent, String type) {
    eligibilityType = type;
    AppController.getHelperSharedPreference()
        .writePreference(StandaloneActivity.this, "DataSharingScreen" + title, "false");
    Toast.makeText(
            StandaloneActivity.this,
            getResources().getString(R.string.please_review_the_updated_consent),
            Toast.LENGTH_SHORT)
        .show();
    ConsentBuilder consentBuilder = new ConsentBuilder();
    List<Step> consentstep =
        consentBuilder.createsurveyquestion(StandaloneActivity.this, consent, title);
    Task consentTask = new OrderedTask(CONSENT, consentstep);
    Intent intent =
        CustomConsentViewTaskActivity.newIntent(
            StandaloneActivity.this, consentTask, studyId, "", title, eligibilityType, "update");
    startActivityForResult(intent, CONSENT_RESPONSECODE);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == CONSENT_RESPONSECODE) {
      if (resultCode == RESULT_OK) {
        Intent intent = new Intent(StandaloneActivity.this, ConsentCompletedActivity.class);
        intent.putExtra("studyId", studyId);
        intent.putExtra("title", title);
        intent.putExtra("eligibility", eligibilityType);
        intent.putExtra("type", data.getStringExtra(CustomConsentViewTaskActivity.TYPE));
        // get the encrypted file path
        intent.putExtra("PdfPath", data.getStringExtra("PdfPath"));
        startActivity(intent);
        finish();
      } else {
        Toast.makeText(this, "Please complete the consent to continue", Toast.LENGTH_SHORT).show();
        finish();
      }
    }
  }

  private void setStudyList(boolean offline) {
    if (!offline) {
      dbServiceSubscriber.saveStudyListToDB(StandaloneActivity.this, study);
    }

    ArrayList<StudyList> activeInprogress = new ArrayList<>();
    ArrayList<StudyList> activeYetToJoin = new ArrayList<>();
    ArrayList<StudyList> activeOthers = new ArrayList<>();
    ArrayList<StudyList> paused = new ArrayList<>();
    ArrayList<StudyList> closed = new ArrayList<>();
    ArrayList<StudyList> others = new ArrayList<>();

    ArrayList<CompletionAdherence> activeInprogressCompletionAdherenceCalc = new ArrayList<>();
    ArrayList<CompletionAdherence> activeYetToJoinCompletionAdherenceCalc = new ArrayList<>();
    ArrayList<CompletionAdherence> activeOthersCompletionAdherenceCalc = new ArrayList<>();
    ArrayList<CompletionAdherence> pausedCompletionAdherenceCalc = new ArrayList<>();
    ArrayList<CompletionAdherence> closedCompletionAdherenceCalc = new ArrayList<>();
    ArrayList<CompletionAdherence> othersCompletionAdherenceCalc = new ArrayList<>();

    CompletionAdherence completionAdherenceCalc;
    CompletionAdherence completionAdherenceCalcSort = null;

    SurveyScheduler survayScheduler = new SurveyScheduler(dbServiceSubscriber, realm);
    for (int i = 0; i < studyListArrayList.size(); i++) {
      if (!AppController.getHelperSharedPreference()
          .readPreference(StandaloneActivity.this, getResources().getString(R.string.userid), "")
          .equalsIgnoreCase("")) {
        completionAdherenceCalc =
            survayScheduler.completionAndAdherenceCalculation(
                studyListArrayList.get(i).getStudyId(), StandaloneActivity.this);
        if (completionAdherenceCalc.isActivityAvailable()) {
          completionAdherenceCalcSort = completionAdherenceCalc;
        } else {
          Studies studies =
              dbServiceSubscriber.getStudies(studyListArrayList.get(i).getStudyId(), realm);
          if (studies != null) {
            try {
              CompletionAdherence completionAdherenceCalculation = new CompletionAdherence();
              completionAdherenceCalculation.setCompletion(studies.getCompletion());
              completionAdherenceCalculation.setAdherence(studies.getAdherence());
              completionAdherenceCalculation.setActivityAvailable(false);
              completionAdherenceCalcSort = completionAdherenceCalculation;
            } catch (Exception e) {
              CompletionAdherence completionAdherenceCalculation = new CompletionAdherence();
              completionAdherenceCalculation.setAdherence(0);
              completionAdherenceCalculation.setCompletion(0);
              completionAdherenceCalculation.setActivityAvailable(false);
              completionAdherenceCalcSort = completionAdherenceCalculation;
              Logger.log(e);
            }
          } else {
            CompletionAdherence completionAdherenceCalculation = new CompletionAdherence();
            completionAdherenceCalculation.setAdherence(0);
            completionAdherenceCalculation.setCompletion(0);
            completionAdherenceCalculation.setActivityAvailable(false);
            completionAdherenceCalcs.add(completionAdherenceCalculation);
            completionAdherenceCalcSort = completionAdherenceCalculation;
          }
        }
      }
      if (studyListArrayList.get(i).getStatus().equalsIgnoreCase(ACTIVE)
          && studyListArrayList.get(i).getStudyStatus().equalsIgnoreCase(IN_PROGRESS)) {
        activeInprogress.add(studyListArrayList.get(i));
        try {
          activeInprogressCompletionAdherenceCalc.add(completionAdherenceCalcSort);
        } catch (Exception e) {
          Logger.log(e);
        }
      } else if (studyListArrayList.get(i).getStatus().equalsIgnoreCase(ACTIVE)
          && studyListArrayList.get(i).getStudyStatus().equalsIgnoreCase(YET_TO_JOIN)) {
        activeYetToJoin.add(studyListArrayList.get(i));
        try {
          activeYetToJoinCompletionAdherenceCalc.add(completionAdherenceCalcSort);
        } catch (Exception e) {
          Logger.log(e);
        }
      } else if (studyListArrayList.get(i).getStatus().equalsIgnoreCase(ACTIVE)) {
        activeOthers.add(studyListArrayList.get(i));
        try {
          activeOthersCompletionAdherenceCalc.add(completionAdherenceCalcSort);
        } catch (Exception e) {
          Logger.log(e);
        }
      } else if (studyListArrayList.get(i).getStatus().equalsIgnoreCase(PAUSED)) {
        paused.add(studyListArrayList.get(i));
        try {
          pausedCompletionAdherenceCalc.add(completionAdherenceCalcSort);
        } catch (Exception e) {
          Logger.log(e);
        }
      } else if (studyListArrayList.get(i).getStatus().equalsIgnoreCase(CLOSED)) {
        closed.add(studyListArrayList.get(i));
        try {
          closedCompletionAdherenceCalc.add(completionAdherenceCalcSort);
        } catch (Exception e) {
          Logger.log(e);
        }
      } else {
        others.add(studyListArrayList.get(i));
        try {
          othersCompletionAdherenceCalc.add(completionAdherenceCalcSort);
        } catch (Exception e) {
          Logger.log(e);
        }
      }
    }

    if (offline) {
      try {
        studyListArrayList = dbServiceSubscriber.clearStudyList(studyListArrayList, realm);
      } catch (Exception e) {
        Logger.log(e);
      }
      try {
        studyListArrayList =
            dbServiceSubscriber.updateStudyList(studyListArrayList, activeInprogress, realm);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList =
            dbServiceSubscriber.updateStudyList(studyListArrayList, activeYetToJoin, realm);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList =
            dbServiceSubscriber.updateStudyList(studyListArrayList, activeOthers, realm);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList = dbServiceSubscriber.updateStudyList(studyListArrayList, paused, realm);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList = dbServiceSubscriber.updateStudyList(studyListArrayList, closed, realm);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList = dbServiceSubscriber.updateStudyList(studyListArrayList, others, realm);
      } catch (Exception e) {
        Logger.log(e);
      }
    } else {
      try {
        studyListArrayList.clear();
      } catch (Exception e) {
        Logger.log(e);
      }
      try {
        studyListArrayList.addAll(activeInprogress);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList.addAll(activeYetToJoin);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList.addAll(activeOthers);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList.addAll(paused);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList.addAll(closed);
      } catch (Exception e) {
        Logger.log(e);
      }

      try {
        studyListArrayList.addAll(others);
      } catch (Exception e) {
        Logger.log(e);
      }
    }

    try {
      completionAdherenceCalcs.clear();
    } catch (Exception e) {
      Logger.log(e);
    }
    try {
      completionAdherenceCalcs.addAll(activeInprogressCompletionAdherenceCalc);
    } catch (Exception e) {
      Logger.log(e);
    }

    try {
      completionAdherenceCalcs.addAll(activeYetToJoinCompletionAdherenceCalc);
    } catch (Exception e) {
      Logger.log(e);
    }

    try {
      completionAdherenceCalcs.addAll(activeOthersCompletionAdherenceCalc);
    } catch (Exception e) {
      Logger.log(e);
    }

    try {
      completionAdherenceCalcs.addAll(pausedCompletionAdherenceCalc);
    } catch (Exception e) {
      Logger.log(e);
    }

    try {
      completionAdherenceCalcs.addAll(closedCompletionAdherenceCalc);
    } catch (Exception e) {
      Logger.log(e);
    }

    try {
      completionAdherenceCalcs.addAll(othersCompletionAdherenceCalc);
    } catch (Exception e) {
      Logger.log(e);
    }

    activeInprogress.clear();
    activeInprogress = null;
    activeInprogressCompletionAdherenceCalc.clear();
    activeInprogressCompletionAdherenceCalc = null;

    activeYetToJoin.clear();
    activeYetToJoin = null;
    activeYetToJoinCompletionAdherenceCalc.clear();
    activeYetToJoinCompletionAdherenceCalc = null;

    activeOthers.clear();
    activeOthers = null;
    activeOthersCompletionAdherenceCalc.clear();
    activeOthersCompletionAdherenceCalc = null;

    paused.clear();
    paused = null;
    pausedCompletionAdherenceCalc.clear();
    pausedCompletionAdherenceCalc = null;

    closed.clear();
    closed = null;
    closedCompletionAdherenceCalc.clear();
    closedCompletionAdherenceCalc = null;

    others.clear();
    others = null;
    othersCompletionAdherenceCalc.clear();
    othersCompletionAdherenceCalc = null;
  }

  @Override
  protected void onDestroy() {
    if (dbServiceSubscriber != null && realm != null) {
      dbServiceSubscriber.closeRealmObj(realm);
    }
    super.onDestroy();
  }

  private void addClearTopFlag(Intent intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }
}
