package org.totschnig.myexpenses.task;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.licence.Licence;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.retrofit.ValidationService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.totschnig.myexpenses.preference.PrefKey.LICENCE_EMAIL;
import static org.totschnig.myexpenses.preference.PrefKey.NEW_LICENCE;

public class LicenceApiTask extends AsyncTask<Void, Void, Result> {
  public static final String BASE_URL = BuildConfig.DEBUG ?
      "https://myexpenses-licencedb-staging.herokuapp.com"  : "https://licencedb.myexpenses.mobi/";
  private final TaskExecutionFragment taskExecutionFragment;
  private final int taskId;

  @Inject
  LicenceHandler licenceHandler;

  @Inject
  HttpLoggingInterceptor loggingInterceptor;

  @Inject
  @Named("deviceId")
  String deviceId;

  LicenceApiTask(TaskExecutionFragment tTaskExecutionFragment, int taskId) {
    this.taskExecutionFragment = tTaskExecutionFragment;
    this.taskId = taskId;
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
  }

  @Override
  protected Result doInBackground(Void... voids) {
    String licenceEmail = LICENCE_EMAIL.getString("");
    String licenceKey = NEW_LICENCE.getString("");
    if ("".equals(licenceKey) || "".equals(licenceEmail)) {
      return Result.FAILURE;
    }

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build();

    ValidationService service = retrofit.create(ValidationService.class);

    if (taskId == TaskExecutionFragment.TASK_VALIDATE_LICENCE) {
      Call<Licence> licenceCall = service.validateLicence(licenceEmail, licenceKey, deviceId);
      try {
        Response<Licence> licenceResponse = licenceCall.execute();
        Licence licence = licenceResponse.body();
        if (licenceResponse.isSuccessful() && licence != null && licence.getType() != null) {
          licenceHandler.updateLicenceStatus(licence);
          return new Result(true, Utils.concatResStrings(MyApplication.getInstance(), " ",
              R.string.licence_validation_success, licence.getType().getResId()));
        } else {
          switch (licenceResponse.code()) {
            case 452:
              licenceHandler.updateLicenceStatus(null);
              return new Result(false, R.string.licence_validation_error_expired);
            case 453:
              licenceHandler.updateLicenceStatus(null);
              return new Result(false, R.string.licence_validation_error_device_limit_exceeded);
            case 404:
              licenceHandler.updateLicenceStatus(null);
              return new Result(false, R.string.licence_validation_failure);
            default:
              return buildFailureResult(String.valueOf(licenceResponse.code()));
          }
        }
      } catch (IOException e) {
        return buildFailureResult(e.getMessage());
      }
    } else if (taskId == TaskExecutionFragment.TASK_REMOVE_LICENCE) {
      Call<Void> licenceCall = service.removeLicence(licenceEmail, licenceKey, deviceId);
      try {
        Response<Void> licenceResponse = licenceCall.execute();
        if (licenceResponse.isSuccessful() || licenceResponse.code() == 404) {
          NEW_LICENCE.remove();
          LICENCE_EMAIL.remove();
          licenceHandler.updateLicenceStatus(null);
          return new Result(true, R.string.licence_removal_success);
        } else {
          return buildFailureResult(String.valueOf(licenceResponse.code()));
        }
      } catch (IOException e) {
        return buildFailureResult(e.getMessage());
      }
    }
    return Result.FAILURE;
  }

  @NonNull
  private Result buildFailureResult(String s) {
    return new Result(false, R.string.error, s);
  }

  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(taskId, result);
    }
  }
}
