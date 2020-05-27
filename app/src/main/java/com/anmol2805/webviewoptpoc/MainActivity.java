package com.anmol2805.webviewoptpoc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.anmol2805.webviewoptpoc.api.service.FileDownloadClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    long CACHE_SIZE = 10000 * 1024; // Size in bytes
    //DiskLruCache cache = null;
    boolean loadFromCache = false;
    LruCache lruCache = null;

    SharedPreferences sharedPreferences;
    Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);
        Switch toggleCache = findViewById(R.id.toggleCache);
        toggleCache.setChecked(false);
        toggleCache.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                loadFromCache = isChecked;
            }
        });
        //cache = DiskLruCache.create(getApplicationContext().getFilesDir(), CACHE_SIZE);
        sharedPreferences = getSharedPreferences("cacheData", MODE_PRIVATE);
        long cacheSize = sharedPreferences.getLong("cacheSize", 0);
        LinkedHashMap<String, String> cache;
        String cacheString = sharedPreferences.getString("cache", "");
        if (cacheString.isEmpty()) {
            cache = new LinkedHashMap<>();
            Log.e("cache", "check " + cacheString);
        } else {
            Type type = new TypeToken<LinkedHashMap<String, String>>() {
            }.getType();
            cache = gson.fromJson(cacheString, type);
            Log.e("cache", "check " + cacheString);
        }
        Log.e("cacheSize", String.valueOf(cacheSize));

        lruCache = new LruCache(cache, cacheSize, this);

        Retrofit.Builder builder = new Retrofit.Builder().baseUrl("https://lit-peak-37103.herokuapp.com/").addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        final FileDownloadClient fileDownloadClient = retrofit.create(FileDownloadClient.class);
        Call<Set<String>> call = fileDownloadClient.getResUrls();
        call.enqueue(new Callback<Set<String>>() {
            @Override
            public void onResponse(Call<Set<String>> call, Response<Set<String>> response) {
                assert response.body() != null;
                Set<String> resUrls = new HashSet<>(response.body());
                int count = 0;
                for (String res : resUrls) {
                    count++;
                    if (res.contains("https")) {
                        final StringBuilder urlbuild = new StringBuilder(res);
                        urlbuild.replace(res.lastIndexOf('_'), res.lastIndexOf('.'), "");

                        downloadFile(fileDownloadClient, urlbuild.toString(), count == 151, lruCache.hasFile(urlbuild.toString()));
                        if (count == 151) {
                            break;
                        }
                    }
                }

            }

            @Override
            public void onFailure(Call<Set<String>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "unable to fetch resource urls", Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, WebviewActivity.class);
                i.putExtra("loadFromCache", loadFromCache);
                startActivity(i);
            }
        });

    }

    private void downloadFile(FileDownloadClient fileDownloadClient, final String url, final boolean isLast, boolean hasFile) {
        if (!hasFile) {
            Log.e("download url", url);
            //final String downloadUrl = url;
            //final StringBuilder urlbuild = new StringBuilder(url);
            //urlbuild.replace(url.lastIndexOf('_'), url.lastIndexOf('.'), "");


            Call<ResponseBody> call = fileDownloadClient.downloadFile(url);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                    if (response.body() != null) {
                        writeResponseBodyToDisk(response.body(), url, isLast, url);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(MainActivity.this, "unable to download file", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String url, boolean isLast, String downloadUrl) {
        try {
            // todo change the file location/name according to your needs
            //File file = new File(getExternalFilesDir(null) + File.separator + url.substring(url.lastIndexOf('/')+1));
            //File file = File.createTempFile(getApplicationContext().getFilesDir() + File.separator + url.substring(url.lastIndexOf('/')+1), " ");

            File file = new File(getApplicationContext().getFilesDir(), url.substring(url.lastIndexOf('/') + 1));

            FileOutputStream fileOutputStream = null;


            InputStream inputStream = null;


            try {
                byte[] fileReader = new byte[4096];


                inputStream = body.byteStream();
                fileOutputStream = new FileOutputStream(file);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    fileOutputStream.write(fileReader, 0, read);


                }

                fileOutputStream.flush();

                boolean written = lruCache.put(downloadUrl, file);
                if (written) {
                    String cacheString = gson.toJson(lruCache.getCache());
                    Log.e("cacheSize", String.valueOf(lruCache.getCacheSize()));
                    Log.e("cache", String.valueOf(lruCache.getCache().size()));
                    Log.e("cache element", String.valueOf(lruCache.getCache().entrySet().iterator().next().getValue()));
                    sharedPreferences.edit().putString("cache", cacheString).apply();
                    sharedPreferences.edit().putLong("cacheSize", lruCache.getCacheSize()).apply();
                }
                return true;
            } catch (IOException | NullPointerException e) {
                Log.d("error", url);
                e.printStackTrace();
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            }
        } catch (IOException e) {
            Log.e("error", e.toString());
            return false;
        }
    }


}
