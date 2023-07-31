package com.harvard.webservicemodule.apihelper;

import com.harvard.usermodule.webservicemodel.ChangePasswordData;
import com.harvard.usermodule.webservicemodel.ForgotPasswordData;
import com.harvard.usermodule.webservicemodel.LoginData;
import com.harvard.usermodule.webservicemodel.TokenData;

import java.util.HashMap;

import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import rx.Observable;

public interface AuthServerInterface {
  @FormUrlEncoded
  @POST("oauth2/token")
  Observable<TokenData> oauthTokenRequest(@HeaderMap HashMap<String,String> headers,
                                          @FieldMap HashMap<String, String> parms);

  @PUT("users/{userId}/change_password")
  Observable<ChangePasswordData> changePassword(@Path(value = "userId", encoded = true) String userId,
                                                @HeaderMap HashMap<String,String> headers,
                                             @Body HashMap<String, String> parms);
  @POST("user/reset_password")
  Observable<ForgotPasswordData> forgotPassword(@HeaderMap HashMap<String,String> headers,
                                                @Body HashMap<String,String> hashMap);
  @POST("users/{userId}/logout")
  Observable<LoginData> logout(@Path(value = "userId", encoded = true) String userId,
                               @HeaderMap HashMap<String, String> headers);
}
