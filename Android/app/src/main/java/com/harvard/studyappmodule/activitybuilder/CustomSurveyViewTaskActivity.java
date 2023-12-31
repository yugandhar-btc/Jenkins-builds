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

package com.harvard.studyappmodule.activitybuilder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.harvard.R;
import com.harvard.notificationmodule.NotificationModuleSubscriber;
import com.harvard.storagemodule.DbServiceSubscriber;
import com.harvard.studyappmodule.SurveyCompleteActivity;
import com.harvard.studyappmodule.activitybuilder.model.Choices;
import com.harvard.studyappmodule.activitybuilder.model.servicemodel.ActivityObj;
import com.harvard.studyappmodule.activitybuilder.model.servicemodel.Steps;
import com.harvard.studyappmodule.custom.ChoiceAnswerFormatCustom;
import com.harvard.studyappmodule.custom.QuestionStepCustom;
import com.harvard.studyappmodule.custom.StepSwitcherCustom;
import com.harvard.studyappmodule.custom.result.StepRecordCustom;
import com.harvard.studyappmodule.custom.result.TaskRecordCustom;
import com.harvard.studyappmodule.studymodel.NotificationDbResources;
import com.harvard.studyappmodule.studymodel.Resource;
import com.harvard.studyappmodule.studymodel.StudyHome;
import com.harvard.utils.ActiveTaskService;
import com.harvard.utils.AppController;
import com.harvard.utils.CustomFirebaseAnalytics;
import com.harvard.utils.Logger;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import org.researchstack.backbone.answerformat.ChoiceAnswerFormat;
import org.researchstack.backbone.result.StepResult;
import org.researchstack.backbone.result.TaskResult;
import org.researchstack.backbone.step.QuestionStep;
import org.researchstack.backbone.step.Step;
import org.researchstack.backbone.storage.database.TaskRecord;
import org.researchstack.backbone.task.Task;
import org.researchstack.backbone.ui.callbacks.StepCallbacks;
import org.researchstack.backbone.ui.step.layout.StepLayout;
import org.researchstack.backbone.utils.FormatHelper;

public class CustomSurveyViewTaskActivity<T> extends AppCompatActivity implements StepCallbacks {
  public static final String EXTRA_STUDYID = "ViewTaskActivity.ExtraStudyId";
  private static final String ACTIVITYID = "ViewTaskActivity.ActivityId";
  public static final String STUDYID = "ViewTaskActivity.StudyId";
  public static final String RUNID = "ViewTaskActivity.RunId";
  private static final String ACTIVITY_STATUS = "ViewTaskActivity.Status";
  private static final String MISSED_RUN = "ViewTaskActivity.MissedRun";
  public static final String COMPLETED_RUN = "ViewTaskActivity.CompletedRun";
  public static final String TOTAL_RUN = "ViewTaskActivity.TotalRun";
  private static final String EXTRA_TASK_RESULT = "ViewTaskActivity.ExtraTaskResult";
  private static final String EXTRA_STEP = "ViewTaskActivity.ExtraStep";
  public static final String ACTIVITY_VERSION = "ViewTaskActivity.Activity_Version";
  private static final String RUN_START_DATE = "ViewTaskActivity.RunStartDate";
  private static final String RUN_END_DATE = "ViewTaskActivity.RunEndDate";
  private static final String BRANCHING = "ViewTaskActivity.branching";
  private CustomFirebaseAnalytics analyticsInstance;

  private StepSwitcherCustom root;

  private Step currentStep;
  private Task task;
  private String studyId;
  private TaskResult taskResult;
  ActivityObj activityObject;
  private Date currentRunStartDate;
  private Date currentRunEndDate;
  int currentStepPosition;
  Intent serviceintent;
  BroadcastReceiver receiver;
  DbServiceSubscriber dbServiceSubscriber;
  String activityId;
  Realm realm;
  public static String RESOURCES = "resources";

  public static Intent newIntent(
      Context context,
      String surveyId,
      String studyId,
      int currentRunId,
      String activityStatus,
      int missedRun,
      int completedRun,
      int totalRun,
      String activityVersion,
      Date currentRunStartDate,
      Date currentRunEndDate,
      String activityId,
      boolean branching) {
    Intent intent = new Intent(context, CustomSurveyViewTaskActivity.class);
    intent.putExtra(EXTRA_STUDYID, surveyId);
    intent.putExtra(STUDYID, studyId);
    intent.putExtra(RUNID, currentRunId);
    intent.putExtra(ACTIVITY_STATUS, activityStatus);
    intent.putExtra(MISSED_RUN, missedRun);
    intent.putExtra(COMPLETED_RUN, completedRun);
    intent.putExtra(TOTAL_RUN, totalRun);
    intent.putExtra(TOTAL_RUN, totalRun);
    intent.putExtra(ACTIVITY_VERSION, activityVersion);
    intent.putExtra(RUN_START_DATE, currentRunStartDate);
    intent.putExtra(RUN_END_DATE, currentRunEndDate);
    intent.putExtra(ACTIVITY_VERSION, activityVersion);
    intent.putExtra(ACTIVITYID, activityId);
    intent.putExtra(BRANCHING, branching);
    return intent;
  }

  @SuppressLint("WrongViewCast")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    analyticsInstance = CustomFirebaseAnalytics.getInstance(this);
    super.setResult(RESULT_CANCELED);
    super.setContentView(R.layout.stepswitchercustom);
    Toolbar toolbar = (Toolbar) findViewById(org.researchstack.backbone.R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    dbServiceSubscriber = new DbServiceSubscriber();
    realm = AppController.getRealmobj(this);
    activityId = getIntent().getStringExtra(ACTIVITYID);
    currentRunStartDate = (Date) getIntent().getSerializableExtra(RUN_START_DATE);
    currentRunEndDate = (Date) getIntent().getSerializableExtra(RUN_END_DATE);
    root = (StepSwitcherCustom) findViewById(R.id.container);

    activityObject =
        dbServiceSubscriber.getActivityBySurveyId(
            (String) getIntent().getSerializableExtra(STUDYID), activityId, realm);
    StepsBuilder stepsBuilder =
        new StepsBuilder(
            CustomSurveyViewTaskActivity.this,
            activityObject,
            getIntent().getBooleanExtra(BRANCHING, false),
            realm);
    task =
        ActivityBuilder.create(
            this,
            ((String) getIntent().getSerializableExtra(EXTRA_STUDYID)),
            stepsBuilder.getsteps(),
            activityObject,
            getIntent().getBooleanExtra(BRANCHING, false),
            dbServiceSubscriber);

    studyId = (String) getIntent().getSerializableExtra(EXTRA_STUDYID);
    StepRecordCustom savedsteps = dbServiceSubscriber.getSavedSteps(task, realm);
    if (savedsteps != null) {
      currentStep = task.getStepWithIdentifier(savedsteps.getStepId());
      if (currentStep != null) {
        RealmResults<StepRecordCustom> stepRecordCustoms =
            dbServiceSubscriber.getStepRecordCustom(task, realm);
        for (int i = 0; i < stepRecordCustoms.size(); i++) {
          if (currentStep.getIdentifier().equalsIgnoreCase(stepRecordCustoms.get(i).getStepId())) {
            TaskRecord taskRecord = new TaskRecord();
            taskRecord.taskId = stepRecordCustoms.get(i).getTaskId();
            taskRecord.started = stepRecordCustoms.get(i).getStarted();
            taskRecord.completed = stepRecordCustoms.get(i).getCompleted();
            taskResult = TaskRecordCustom.toTaskResult(taskRecord, stepRecordCustoms);
          }
        }
      } else {
        taskResult = new TaskResult(task.getIdentifier());
        taskResult.setStartDate(new Date());
      }
    } else {
      taskResult = new TaskResult(task.getIdentifier());
      taskResult.setStartDate(new Date());
    }

    task.validateParameters();

    if (currentStep == null) {
      currentStep = task.getStepAfterStep(null, taskResult);
    }

    showStep(currentStep);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.survey_menu, menu);
    MenuItem item = menu.findItem(R.id.action_settings);
    SpannableString s = new SpannableString(item.getTitle());
    s.setSpan(new ForegroundColorSpan(Color.parseColor("#ffffff")), 0, s.length(), 0);
    item.setTitle(s);
    return true;
  }

  protected Step getCurrentStep() {
    return currentStep;
  }

  protected void showNextStep() {
    Bundle eventProperties = new Bundle();
    eventProperties.putString(
        CustomFirebaseAnalytics.Param.BUTTON_CLICK_REASON,
        getString(R.string.custom_survey_task_next));
    analyticsInstance.logEvent(CustomFirebaseAnalytics.Event.ADD_BUTTON_CLICK, eventProperties);
    savestepresult(currentStep, true);
    Step nextStep = task.getStepAfterStep(currentStep, taskResult);
    if (nextStep == null) {
      saveAndFinish();
    } else {
      showStep(nextStep);
    }
  }

  private void savestepresult(Step currentStep, boolean savecurrent) {

    TaskRecordCustom taskRecord = new TaskRecordCustom();
    taskRecord.taskId = taskResult.getIdentifier();
    taskRecord.started = taskResult.getStartDate();
    taskRecord.completed = taskResult.getEndDate();

    for (StepResult stepResult : taskResult.getResults().values()) {
      if (stepResult != null
          && stepResult.getIdentifier().equalsIgnoreCase(currentStep.getIdentifier())) {
        StepRecordCustom stepRecord = new StepRecordCustom();

        int nextId;
        StepRecordCustom stepRecordCustom =
            dbServiceSubscriber.getStepRecordCustomById(
                taskResult.getIdentifier(), stepResult.getIdentifier(), realm);
        if (stepRecordCustom == null) {
          Number currentIdNum = dbServiceSubscriber.getStepRecordCustomId(realm);
          if (currentIdNum == null) {
            nextId = 1;
          } else {
            nextId = currentIdNum.intValue() + 1;
          }
        } else {
          nextId = stepRecordCustom.id;
        }

        stepRecord.setId(nextId);
        stepRecord.taskRecordId = taskRecord.id;
        stepRecord.taskId = taskResult.getIdentifier();
        stepRecord.stepId = stepResult.getIdentifier();
        // includes studyId
        stepRecord.activityID =
            taskResult.getIdentifier().substring(0, taskResult.getIdentifier().lastIndexOf("_"));
        stepRecord.started = stepResult.getStartDate();
        stepRecord.completed = stepResult.getEndDate();
        stepRecord.runStartDate = currentRunStartDate;
        stepRecord.runEndDate = currentRunEndDate;
        stepRecord.studyId = "" + getIntent().getStringExtra(STUDYID);

        try {
          if (stepResult.getAnswerFormat() == null) {
            QuestionStepCustom step = (QuestionStepCustom) currentStep;
            ChoiceAnswerFormatCustom format = (ChoiceAnswerFormatCustom) step.getAnswerFormat1();
            RealmList<Choices> textChoices = new RealmList<>();
            for (int i = 0; i < format.getChoices().length; i++) {
              Choices choices = new Choices();
              choices.setValue(format.getChoices()[i].getText());
              textChoices.add(choices);
            }
            stepRecord.setTextChoices(textChoices);
          } else {
            QuestionStep step = (QuestionStep) currentStep;
            ChoiceAnswerFormat format = (ChoiceAnswerFormat) step.getAnswerFormat();
            RealmList<Choices> textChoices = new RealmList<>();
            for (int i = 0; i < format.getChoices().length; i++) {
              Choices choices = new Choices();
              choices.setValue(format.getChoices()[i].getText());
              textChoices.add(choices);
            }
            stepRecord.setTextChoices(textChoices);
          }
        } catch (Exception e) {
          stepRecord.setTextChoices(null);
        }

        stepRecord.taskStepID = taskResult.getIdentifier() + "_" + stepResult.getIdentifier();
        if (!stepResult.getResults().isEmpty()) {
          Gson gson = new GsonBuilder().setDateFormat(FormatHelper.DATE_FORMAT_ISO_8601).create();
          stepRecord.result = gson.toJson(stepResult.getResults());
        }

        dbServiceSubscriber.updateStepRecord(this, stepRecord);

        if (dbServiceSubscriber.getStudyResource(getIntent().getStringExtra(STUDYID), realm)
            == null) {
        } else if (dbServiceSubscriber
                .getStudyResource(getIntent().getStringExtra(STUDYID), realm)
                .getResources()
            == null) {
        } else {
          StudyHome studyHome = null;
          try {
            studyHome =
                dbServiceSubscriber.getWithdrawalType(getIntent().getStringExtra(STUDYID), realm);
          } catch (Exception e) {
            Logger.log(e);
          }
          RealmList<Resource> resourceArrayList =
              dbServiceSubscriber
                  .getStudyResource(getIntent().getStringExtra(STUDYID), realm)
                  .getResources();

          if (studyHome != null && resourceArrayList != null) {
            getResourceNotification(resourceArrayList, studyHome);
          }
        }
      }
    }
  }

  private void getResourceNotification(RealmList<Resource> resourceArrayList, StudyHome studyHome) {
    for (int i = 0; i < resourceArrayList.size(); i++) {
      if (resourceArrayList.get(i).getAudience() != null
          && resourceArrayList.get(i).getAudience().equalsIgnoreCase("Limited")) {
        if (resourceArrayList.get(i).getAvailability().getAvailableDate().equalsIgnoreCase("")) {
          StepRecordCustom stepRecordCustom =
              dbServiceSubscriber.getSurveyResponseFromDB(
                  getIntent().getStringExtra(STUDYID)
                      + "_STUDYID_"
                      + AppController.getSourceActivityId(resourceArrayList.get(i)),
                  AppController.getSourceKey(resourceArrayList.get(i)),
                  realm);
          if (stepRecordCustom != null) {
            Calendar startCalender = Calendar.getInstance();

            Calendar endCalender = Calendar.getInstance();

            JSONObject jsonObject = null;
            try {
              jsonObject = new JSONObject(stepRecordCustom.getResult());
              startCalender.setTime(
                  AppController.getDateFormatForApi().parse("" + jsonObject.get("answer")));
              startCalender.add(
                  Calendar.DATE, resourceArrayList.get(i).getAvailability().getStartDays());
              startCalender.set(Calendar.HOUR, 0);
              startCalender.set(Calendar.MINUTE, 0);
              startCalender.set(Calendar.SECOND, 0);
              startCalender.set(Calendar.AM_PM, Calendar.AM);
              NotificationDbResources notificationsDb = null;
              RealmResults<NotificationDbResources> notificationsDbs =
                  dbServiceSubscriber.getNotificationDbResources(
                      activityId, getIntent().getStringExtra(STUDYID), RESOURCES, realm);
              if (notificationsDbs != null && notificationsDbs.size() > 0) {
                for (int j = 0; j < notificationsDbs.size(); j++) {
                  if (notificationsDbs
                      .get(j)
                      .getResourceId()
                      .equalsIgnoreCase(resourceArrayList.get(i).getResourcesId())) {
                    notificationsDb = notificationsDbs.get(j);
                    break;
                  }
                }
              }
              if (notificationsDb == null) {
                setRemainder(
                    startCalender,
                    activityId,
                    getIntent().getStringExtra(STUDYID),
                    resourceArrayList.get(i).getNotificationText(),
                    resourceArrayList.get(i).getResourcesId());
              }

              endCalender.setTime(
                  AppController.getDateFormatForApi().parse("" + jsonObject.get("answer")));
              endCalender.add(
                  Calendar.DATE, resourceArrayList.get(i).getAvailability().getEndDays());
              endCalender.set(Calendar.HOUR, 11);
              endCalender.set(Calendar.MINUTE, 59);
              endCalender.set(Calendar.SECOND, 59);
              endCalender.set(Calendar.AM_PM, Calendar.PM);

              Calendar currentday = Calendar.getInstance();
              currentday.set(Calendar.HOUR, 0);
              currentday.set(Calendar.MINUTE, 0);
              currentday.set(Calendar.SECOND, 0);
              currentday.set(Calendar.AM_PM, Calendar.AM);

            } catch (JSONException | ParseException e) {
              Logger.log(e);
            }
          }
        }
      }
    }
  }

  private void setRemainder(
      Calendar startCalender,
      String activityId,
      String studyId,
      String notificationText,
      String resourceId) {
    RealmResults<NotificationDbResources> notificationsDbs =
        dbServiceSubscriber.getNotificationDbResources(
            activityId, getIntent().getStringExtra(STUDYID), RESOURCES, realm);
    boolean status = false;
    for (int i = 0; i < notificationsDbs.size(); i++) {
      if (notificationsDbs.get(i).getDescription().equalsIgnoreCase(notificationText)) {
        status = true;
        break;
      }
    }
    if (!status && notificationText != null && !notificationText.isEmpty()) {
      NotificationModuleSubscriber notificationModuleSubscriber =
          new NotificationModuleSubscriber(dbServiceSubscriber, realm);
      notificationModuleSubscriber.generateAnchorDateLocalNotification(
          startCalender.getTime(),
          activityId,
          studyId,
          CustomSurveyViewTaskActivity.this,
          notificationText,
          resourceId);
    }
  }

  protected void showPreviousStep() {
    Bundle eventProperties = new Bundle();
    eventProperties.putString(
        CustomFirebaseAnalytics.Param.BUTTON_CLICK_REASON,
        getString(R.string.custom_survey_task_back));
    analyticsInstance.logEvent(CustomFirebaseAnalytics.Event.ADD_BUTTON_CLICK, eventProperties);
    Step previousStep = task.getStepBeforeStep(currentStep, taskResult);
    if (previousStep == null) {
      finish();
    } else {
      savestepresult(previousStep, false);
      showStep(previousStep);
    }
  }

  private void showStep(Step step) {
    // branching logic here
    currentStepPosition = task.getProgressOfCurrentStep(currentStep, taskResult).getCurrent();
    int newStepPosition = task.getProgressOfCurrentStep(step, taskResult).getCurrent();

    StepLayout stepLayout = getLayoutForStep(step);
    stepLayout
        .getLayout()
        .setTag(org.researchstack.backbone.R.id.rsb_step_layout_id, step.getIdentifier());
    root.show(
        stepLayout,
        newStepPosition >= currentStepPosition
            ? StepSwitcherCustom.SHIFT_LEFT
            : StepSwitcherCustom.SHIFT_RIGHT);
    currentStep = step;
    AppController.getHelperHideKeyboard(this);
  }

  protected StepLayout getLayoutForStep(Step step) {
    // Change the title on the activity
    String title = task.getTitleForStep(this, step);
    setActionBarTitle(title);

    // Get result from the TaskResult, can be null
    StepResult result = taskResult.getStepResult(step.getIdentifier());

    // Return the Class & constructor
    StepLayout stepLayout = createLayoutFromStep(step);
    stepLayout.initialize(step, result);
    stepLayout.setCallbacks(this);

    return stepLayout;
  }

  @NonNull
  private StepLayout createLayoutFromStep(Step step) {
    try {
      Class cls = step.getStepLayoutClass();
      Constructor constructor = cls.getConstructor(Context.class);
      return (StepLayout) constructor.newInstance(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void saveAndFinish() {
    taskResult.setEndDate(new Date());

    Intent intent = new Intent(CustomSurveyViewTaskActivity.this, SurveyCompleteActivity.class);
    intent.putExtra(EXTRA_TASK_RESULT, taskResult);
    intent.putExtra(STUDYID, getIntent().getStringExtra(STUDYID));
    intent.putExtra(EXTRA_STUDYID, getIntent().getStringExtra(EXTRA_STUDYID));
    intent.putExtra(RUNID, getIntent().getIntExtra(RUNID, 0));
    intent.putExtra(MISSED_RUN, getIntent().getStringExtra(MISSED_RUN));
    intent.putExtra(COMPLETED_RUN, getIntent().getIntExtra(COMPLETED_RUN, 0));
    intent.putExtra(TOTAL_RUN, getIntent().getIntExtra(TOTAL_RUN, 0));
    intent.putExtra(ACTIVITY_VERSION, getIntent().getStringExtra(ACTIVITY_VERSION));
    startActivityForResult(intent, 123);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 123 && resultCode == RESULT_OK) {
      finish();
    } else if (requestCode == 123 && resultCode == RESULT_CANCELED) {
      this.recreate();
    }
  }

  @Override
  protected void onPause() {
    AppController.getHelperHideKeyboard(this);
    super.onPause();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      notifyStepOfBackPress();
      return true;
    } else if (item.getItemId() == R.id.action_settings) {
      showConfirmExitDialog();
      Bundle eventProperties = new Bundle();
      eventProperties.putString(
          CustomFirebaseAnalytics.Param.BUTTON_CLICK_REASON,
          getString(R.string.custom_survey_task_exit));
      analyticsInstance.logEvent(CustomFirebaseAnalytics.Event.ADD_BUTTON_CLICK, eventProperties);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {
    notifyStepOfBackPress();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(EXTRA_TASK_RESULT, taskResult);
    outState.putSerializable(EXTRA_STEP, currentStep);
  }

  private void notifyStepOfBackPress() {
    StepLayout currentStepLayout =
        (StepLayout) findViewById(org.researchstack.backbone.R.id.rsb_current_step);
    currentStepLayout.isBackEventConsumed();

    if (isMyServiceRunning(ActiveTaskService.class)) {
      try {
        if (serviceintent != null && receiver != null) {
          stopService(serviceintent);
          unregisterReceiver(receiver);
        }
      } catch (Exception e) {
        Logger.log(e);
      }
    }
  }

  @Override
  protected void onDestroy() {
    dbServiceSubscriber.closeRealmObj(realm);
    if (isMyServiceRunning(ActiveTaskService.class)) {
      try {
        if (serviceintent != null && receiver != null) {
          stopService(serviceintent);
          unregisterReceiver(receiver);
        }
      } catch (Exception e) {
        Logger.log(e);
      }
    }
    super.onDestroy();
  }

  public void serviceStarted(BroadcastReceiver receiver, Intent serviceintent) {
    this.receiver = receiver;
    this.serviceintent = serviceintent;
  }

  private boolean isMyServiceRunning(Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service :
        manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onSaveStep(int action, Step step, StepResult result) {
    onSaveStepResult(step.getIdentifier(), result);

    onExecuteStepAction(action);
  }

  protected void onSaveStepResult(String id, StepResult result) {
    taskResult.setStepResultForStepIdentifier(id, result);
  }

  protected void onExecuteStepAction(int action) {
    if (action == StepCallbacks.ACTION_NEXT) {
      showNextStep();
    } else if (action == StepCallbacks.ACTION_PREV) {
      showPreviousStep();
    } else if (action == StepCallbacks.ACTION_END) {
      showConfirmExitDialog();
    } else if (action == StepCallbacks.ACTION_NONE) {
      // Used when onSaveInstanceState is called of a view. No action is taken.
    } else {
      throw new IllegalArgumentException(
          "Action with value "
              + action
              + " is invalid. "
              + "See StepCallbacks for allowable arguments");
    }
  }

  private void hideKeyboard() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
    if (imm.isActive() && imm.isAcceptingText()) {
      imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }
  }

  private void showConfirmExitDialog() {
    AlertDialog alertDialog =
        new AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.exit_activity)
            .setPositiveButton(
                R.string.endtask,
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {
                    Bundle eventProperties = new Bundle();
                    eventProperties.putString(
                        CustomFirebaseAnalytics.Param.BUTTON_CLICK_REASON,
                        getString(R.string.custom_survey_task_end_task));
                    analyticsInstance.logEvent(
                        CustomFirebaseAnalytics.Event.ADD_BUTTON_CLICK, eventProperties);
                    finish();
                  }
                })
            .setNegativeButton(
                R.string.cancel,
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {
                    Bundle eventProperties = new Bundle();
                    eventProperties.putString(
                        CustomFirebaseAnalytics.Param.BUTTON_CLICK_REASON,
                        getString(R.string.custom_survey_task_cancel));
                    analyticsInstance.logEvent(
                        CustomFirebaseAnalytics.Event.ADD_BUTTON_CLICK, eventProperties);
                  }
                })
            .create();
    alertDialog.show();
  }

  @Override
  public void onCancelStep() {
    setResult(Activity.RESULT_CANCELED);
    finish();
  }

  public void setActionBarTitle(String title) {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setTitle(title);
    }
  }

  public void addformquestion(QuestionStep questionStep, String identifier, String stepIdentifier) {
    String[] survayId = studyId.split("_STUDYID_");
    ActivityObj surveyObject =
        dbServiceSubscriber.getActivityBySurveyId(
            getIntent().getStringExtra(STUDYID),
            survayId[1].substring(0, survayId[1].lastIndexOf("_")),
            realm);
    Steps step = dbServiceSubscriber.getSteps(identifier, realm);

    if (surveyObject != null && step != null) {
      Steps steps = dbServiceSubscriber.updateSteps(step, questionStep.getIdentifier(), realm);
      RealmList<Steps> formstep = surveyObject.getSteps();
      realm.beginTransaction();
      for (int i = 0; i < formstep.size(); i++) {
        if (formstep.get(i).getType().equalsIgnoreCase("form")
            && formstep.get(i).getKey().equalsIgnoreCase(stepIdentifier)) {
          formstep.get(i).getSteps().add(steps);
        }
      }
      realm.commitTransaction();
    } else {
      Toast.makeText(this, getResources().getString(R.string.step_couldnt_add), Toast.LENGTH_SHORT)
          .show();
    }
  }
}
