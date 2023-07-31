package com.harvard.webservicemodule.apihelper;

import com.google.gson.JsonObject;
import com.harvard.studyappmodule.enroll.EnrollData;
import com.harvard.usermodule.webservicemodel.LoginData;
import com.harvard.usermodule.webservicemodel.StudyData;

import org.json.JSONObject;

import java.util.HashMap;

import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;
import rx.Observable;

public interface EnrollmentDataStoreInterface {

  @POST("updateStudyState")
  Observable<LoginData> updateStudyState(@HeaderMap HashMap<String, String> headers,
                                         @Body HashMap<String, Object> jsonObject);

  @GET("studyState")
  Observable<StudyData> getStudyState(@HeaderMap HashMap<String, String> headers);

  @POST("validateEnrollmentToken")
  Observable<EnrollData> validateEnrollmentToken(@HeaderMap HashMap<String, String> headers,
                                                 @Body HashMap<String, String> params);

  @POST("enroll")
  Observable<EnrollData> sendEnrollData(@HeaderMap HashMap<String, String> headers,
                                        @Body HashMap<String, String> params);

  @POST("withdrawfromstudy")
  Observable<LoginData> withdrawfromstudy(@HeaderMap HashMap<String, String> headers,
                                          @Body HashMap<String, String> object);
  @POST
  Observable<LoginData>getRequest(@Url String url, @HeaderMap HashMap<String,String> headers, @Body RequestBody jsonObject);

}
