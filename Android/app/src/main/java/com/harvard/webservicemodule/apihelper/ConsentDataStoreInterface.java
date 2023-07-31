package com.harvard.webservicemodule.apihelper;

import com.harvard.studyappmodule.studymodel.ConsentPDF;
import com.harvard.usermodule.webservicemodel.LoginData;

import java.util.HashMap;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

public interface ConsentDataStoreInterface {
  @POST("updateEligibilityConsentStatus")
  Observable<LoginData> updateEligibilityConsentStatus(@HeaderMap HashMap<String, String> headerparams,
                                                       @Body HashMap<String, Object> body);

  @GET("consentDocument")
  Observable<ConsentPDF> getConsentDocument(@HeaderMap HashMap<String, String> header,
                                            @Query("studyId") String studyId,
                                            @Query("consentVersion") String consentVersion);

}
