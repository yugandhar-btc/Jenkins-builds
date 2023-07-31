package com.harvard;

import android.content.Context;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.harvard.webservicemodule.VCInterceptor;
import com.harvard.webservicemodule.apihelper.UrlTypeConstants;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ServiceManager {

    private final static String TAG = ServiceManager.class.getName();
    private String BASE_URL = "";
    private UrlTypeConstants urlTypeConstants;
    private Retrofit.Builder builder;
    private Context context;
    private Handler mMainHandler;
    private String appVersion;

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    private static ServiceManager instance = null;

    public ServiceManager() {

    }

    private Retrofit.Builder getRetrofitBuilder(String datastore) {

        switch (datastore) {
            case "BASE_URL_STUDY_DATASTORE":
                BASE_URL = "https://studies.vc-prod.validcare.com/study-datastore/";
                break;
            case "BASE_URL_PARTICIPANT_DATASTORE":
                BASE_URL = "https://participants.vc-prod.validcare.com/participant-user-datastore/";
                break;
            case "BASE_URL_RESPONSE_DATASTORE":
                BASE_URL ="https://participants.vc-prod.validcare.com/response-datastore/";
                break;
            case "BASE_URL_AUTH_SERVER":
                BASE_URL ="https://participants.vc-prod.validcare.com/auth-server/";
                break;
            case "BASE_URL_HYDRA_SERVER":
                BASE_URL ="https://participants.vc-prod.validcare.com/";
                break;
           case "BASE_URL_PARTICIPANT_ENROLLMENT_DATASTORE":
                BASE_URL ="https://participants.vc-prod.validcare.com/participant-enroll-datastore/";
                break;
           case "BASE_URL_PARTICIPANT_CONSENT_DATASTORE":
                BASE_URL ="https://participants.vc-prod.validcare.com/participant-consent-datastore/";
                break;

        }
        if (builder == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            Log.e("check","Base urls is1 "+BASE_URL);
            Gson gson = new GsonBuilder().setLenient().create();
            builder = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okClient()
                            .addInterceptor(loggingInterceptor)
                            .addInterceptor(new VCInterceptor(BASE_URL))
                            .connectTimeout(2, TimeUnit.MINUTES)
                            .readTimeout(2, TimeUnit.MINUTES)
                            .build())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson));// Json to Object
        }
        return builder;
    }



    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void setBaseUrl(String baseUrl) {
        BASE_URL = baseUrl;
    }


    public <S> S createService(final Class<S> serviceClass, String dataStore) {
        return getRetrofitBuilder(dataStore).build().create(serviceClass);

    }



    public void executeOnMainThread(Runnable runnable) {
        if (mMainHandler == null) {
            mMainHandler = new Handler(getContext().getMainLooper());
        }
        mMainHandler.post(runnable);
    }


    public static ServiceManager getInstance() {
        if (instance == null) {
            instance = new ServiceManager();
        }
        return instance;
    }


    private static OkHttpClient.Builder okClient() {
        return (new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS));
    }

}
