package com.harvard.webservicemodule.apihelper;
import com.harvard.studyappmodule.activitybuilder.model.servicemodel.ActivityInfoData;
import com.harvard.studyappmodule.activitylistmodel.ActivityListData;
import com.harvard.studyappmodule.consent.model.EligibilityConsent;
import com.harvard.studyappmodule.studymodel.ConsentDocumentData;
import com.harvard.studyappmodule.studymodel.DashboardData;
import com.harvard.studyappmodule.studymodel.NotificationData;
import com.harvard.studyappmodule.studymodel.Study;
import com.harvard.studyappmodule.studymodel.StudyHome;
import com.harvard.studyappmodule.studymodel.StudyResource;
import com.harvard.studyappmodule.studymodel.StudyUpdate;
import com.harvard.utils.version.VersionModel;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

import retrofit2.http.Query;
import rx.Observable;

public interface StudyDataStoreAPIInterface {

    @GET("studyList")
    Observable<Study> getStudyList();

    @GET("studyInfo")
    Observable<StudyHome> getStudyInfo(@Query("studyId") String id);

    @GET("activity")
    Observable<ActivityInfoData> getActivityInfo(@Query("studyId") String sid,
                                              @Query("activityId") String aid,
                                              @Query("activityVersion") String version);

    @GET("study")
    Observable<Study> getSpecificStudy(@Query("studyId") String id);

    @GET("studyUpdates")
    Observable<StudyUpdate> getStudyUpdates(@Query("studyId") String id,
                                            @Query("studyVersion") String version);

    @GET("activityList")
    Observable<ActivityListData> getActivityList(@Query("studyId") String id);

    @GET("resources")
    Observable<StudyResource> getResources(@Header("studyId") String studyId, @Query("studyId") String id);

    @GET("notifications")
    Observable<NotificationData> getNotifications(@Query("skip") int skip,
                                                  @Query("verificationTime") String verificationTime);
    @GET("studyDashboard")
    Observable<DashboardData> getStudyDashboard(@Query("studyId") String id);
    @GET("consentDocument")
    Observable<ConsentDocumentData> getConsentDocument(@Query("studyId") String id,
                                                       @Query("consentVersion") String consentVersion,
                                                       @Query("activityId") String activityId,
                                                       @Query("activityVersion") String activityVersion);

    @GET("versionInfo")
    Observable<VersionModel> getVersionInfo();
    @GET("eligibilityConsent")
    Call<EligibilityConsent> getEligibilityConsnet(@Query("studyId") String id);
}
