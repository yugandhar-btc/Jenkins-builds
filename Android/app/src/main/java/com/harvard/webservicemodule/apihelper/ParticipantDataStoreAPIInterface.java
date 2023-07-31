package com.harvard.webservicemodule.apihelper;
import com.harvard.studyappmodule.studymodel.ReachOut;
import com.harvard.usermodule.model.Apps;
import com.harvard.usermodule.webservicemodel.LoginData;
import com.harvard.usermodule.webservicemodel.RegistrationData;
import com.harvard.usermodule.webservicemodel.UpdateProfileRequestData;
import com.harvard.usermodule.webservicemodel.UpdateUserProfileData;
import com.harvard.usermodule.webservicemodel.UserProfileData;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface ParticipantDataStoreAPIInterface {
    @POST("register")
  Observable<RegistrationData> postRegistrationData(@Body HashMap object);

    @POST("updateUserProfile")
    Observable<UpdateUserProfileData> updateUserProfile(@HeaderMap Map<String, String> headers,
                                                        @Body UpdateProfileRequestData updateProfileRequestData);

    @GET("userProfile")
    Observable<UserProfileData> getUserProfile(@HeaderMap Map<String, String> headers);

    @POST("verifyEmailId")
    Observable<LoginData> verifyEmailId(@Body HashMap<String,String> object);

    @POST("resendConfirmation")
    Observable<LoginData> resendConfirmation(@Body HashMap<String, String> emaild);

    @HTTP(method = "DELETE", path = "deactivate", hasBody = true)
    Observable<LoginData> deactivate(@HeaderMap HashMap<String, String> headers,
                                     @Body HashMap<String,Object> object);

    @POST("contactUs")
    Observable<ReachOut> contactUs(@Body HashMap<String, String> hashMap);

    @POST("feedback")
    Observable<ReachOut> sendFeedback(@Body HashMap<String, String> hashMap);

    @GET("apps")
    Observable<Apps> getApps(@Query("appId") String id);


}
