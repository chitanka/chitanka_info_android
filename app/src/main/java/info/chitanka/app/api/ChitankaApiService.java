package info.chitanka.app.api;

import com.google.gson.Gson;

import info.chitanka.app.Constants;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by nmp on 16-3-15.
 */
public class ChitankaApiService {
    private ChitankaApiService() {
    }

    public static ChitankaApi createChitankaApiService(Gson gson) {
        Retrofit.Builder builder = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(Constants.CHITANKA_INFO_API);

        OkHttpClient client = new OkHttpClient.Builder().build();
        builder.client(client);

        return builder.build().create(ChitankaApi.class);
    }
}
