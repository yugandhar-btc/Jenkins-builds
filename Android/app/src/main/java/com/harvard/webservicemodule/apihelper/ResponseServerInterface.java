package com.harvard.webservicemodule.apihelper;

import com.google.gson.JsonObject;
import com.harvard.usermodule.webservicemodel.ActivityData;
import com.harvard.usermodule.webservicemodel.LoginData;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface ResponseServerInterface {
    @POST("participant/process-response")
    Observable<LoginData> getProcessResponse(@HeaderMap Map<String, String> headers,
                                             @Body HashMap<String,Object> jsonObject);

    @GET("participant/getresponse")
    Call<ResponseBody> getParticipantData(@HeaderMap Map<String, String> headers,
                                          @Query("appId") String appId,
                                          @Query("participantId") String participantId,
                                          @Query("tokenId") String tokenId,
                                          @Query("siteId") String siteId,
                                          @Query("studyId") String studyId,
                                          @Query("activityId") String activityId,
                                          @Query("questionKey") String questionKey,
                                          @Query("activityVersion") String activityVersion,
                                          @Query("activityRunId") String activityRunId);

    @POST("participant/update-activity-state")
    Observable<LoginData> getUpdateActivityState(@HeaderMap Map<String, String> headers,
                                                 @Body HashMap<String,Object> jsonObject);

    @GET("participant/get-activity-state")
    Observable<ActivityData> getActivityState(@HeaderMap Map<String, String> headers,
                                              @Query("studyId") String studyId,
                                              @Query("participantId") String participantId);
    @POST()
    Observable<LoginData>getRequest(@Url String url, @HeaderMap HashMap<String,String> headers, @Body RequestBody jsonObject);


}
