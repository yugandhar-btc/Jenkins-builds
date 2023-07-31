package com.harvard.webservicemodule;

import android.util.Base64;
import android.util.Log;

import com.harvard.AppConfig;
import com.harvard.BuildConfig;
import com.harvard.FdaApplication;
import com.harvard.R;
import com.harvard.storagemodule.DbServiceSubscriber;
import com.harvard.usermodule.model.Apps;
import com.harvard.utils.AppController;
import com.harvard.webservicemodule.apihelper.UrlTypeConstants;

import java.io.IOException;

import io.realm.Realm;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class VCInterceptor implements Interceptor {

  private String BASE_URL = "";
  private static String CONTENT_TYPE_KEY = "Content-Type";
  private static String APPLICATION_JSON = "application/json";
  private static String APP_NAME_KEY = "appName";
  private static String APP_NAME = "";
  public VCInterceptor(String BASE_URL) {
    this.BASE_URL = BASE_URL;
  }
    private static String basicAuth = AppConfig.API_TOKEN;
    String encoding = Base64.encodeToString(basicAuth.getBytes(), Base64.DEFAULT).trim();
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
      DbServiceSubscriber dbServiceSubscriber = new DbServiceSubscriber();
      Realm realm = AppController.getRealmobj(FdaApplication.getInstance());
      Apps apps = dbServiceSubscriber.getApps(realm);
      Request newRequest = null;
       /* if (!"/posts".contains(BASE_URL) ) {
            return chain.proceed(originalRequest);
        }*/
      Log.e("check","BaseUrl is "+BASE_URL);
      if (BASE_URL.equalsIgnoreCase(BuildConfig.BASE_URL_STUDY_DATASTORE+"/")) {
        String token = "Basic " + encoding;
        newRequest = originalRequest.newBuilder()
            .header(CONTENT_TYPE_KEY, APPLICATION_JSON)
            .header("Authorization", token)
            .header(AppConfig.STUDY_DATASTORE_APP_ID_KEY, AppConfig.APP_ID_VALUE)
            .build();
      } else if(BASE_URL.equalsIgnoreCase(BuildConfig.BASE_URL_PARTICIPANT_DATASTORE+"/")) {
        if (apps == null) {
          APP_NAME = FdaApplication.getInstance().getString(R.string.app_name);
        }
        else {
          APP_NAME = apps.getAppName();
        }
        newRequest = originalRequest.newBuilder()
            .header(CONTENT_TYPE_KEY, APPLICATION_JSON)
            .header(AppConfig.APP_ID_KEY, AppConfig.APP_ID_VALUE)
            .header(APP_NAME_KEY,APP_NAME)
            .build();
      } else if(BASE_URL.equalsIgnoreCase(BuildConfig.BASE_URL_RESPONSE_DATASTORE+"/")||
              BASE_URL.equalsIgnoreCase(BuildConfig.BASE_URL_PARTICIPANT_ENROLLMENT_DATASTORE+"/")||
              BASE_URL.equalsIgnoreCase(BuildConfig.BASE_URL_PARTICIPANT_CONSENT_DATASTORE+"/")) {
        Log.e("check","Base Url is2 "+BASE_URL);
        newRequest = originalRequest.newBuilder()
                .header(CONTENT_TYPE_KEY, APPLICATION_JSON)
                .build();
      } else if (BASE_URL.equalsIgnoreCase(BuildConfig.BASE_URL_AUTH_SERVER +"/")) {
        newRequest = originalRequest.newBuilder()
            .header(CONTENT_TYPE_KEY, APPLICATION_JSON).build();
      }

      return chain.proceed(newRequest);
    }
}
