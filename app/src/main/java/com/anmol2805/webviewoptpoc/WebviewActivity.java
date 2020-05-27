package com.anmol2805.webviewoptpoc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tomclaw.cache.DiskLruCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.LinkedHashMap;

public class WebviewActivity extends AppCompatActivity {

    Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        WebView webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        boolean loadFromCache = getIntent().getBooleanExtra("loadFromCache",true);
        long CACHE_SIZE = 10000 * 1024; // Size in bytes
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        SharedPreferences sharedPreferences = getSharedPreferences("cacheData", MODE_PRIVATE);
        long cacheSize = sharedPreferences.getLong("cacheSize", 0);
        LinkedHashMap<String, String> cache;
        String cacheString = sharedPreferences.getString("cache", "");
        if (cacheString.isEmpty()) {
            cache = new LinkedHashMap<>();
        } else {
            Type type = new TypeToken<LinkedHashMap<String, String>>() {
            }.getType();
            cache = gson.fromJson(cacheString, type);
        }
        Log.e("cacheSize", String.valueOf(cacheSize));
        Log.e("cache", String.valueOf(cache.size()));

        final LruCache lruCache = new LruCache(cache, cacheSize, this);

        if(loadFromCache) {
            //Log.d("cache from webview", String.valueOf(cache.keySet()));
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(final WebView view, WebResourceRequest request) {
                    String url = request.getUrl().toString();
                    Log.d("resourceUrls:", url);
                    try {
                        return new WebResourceResponse(getMimeType(getFileExt(url)), "UTF-8", new FileInputStream(lruCache.get(url)));
                    } catch (FileNotFoundException | NullPointerException e) {
                        e.printStackTrace();
                        return super.shouldInterceptRequest(view, request);
                    }

                }

            });
        }

        webView.loadUrl("https://lit-peak-37103.herokuapp.com");




    }

    public String getFileExt(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf(".") + 1);
    }

    public String getMimeType(String fileExtension){
        String mimeType = "";
        switch (fileExtension){
            case "css" :
                mimeType = "text/css";
                break;
            case "js" :
                mimeType = "text/javascript";
                break;
            case "png" :
                mimeType = "image/png";
                break;
            case "jpg" :
                mimeType = "image/jpeg";
                break;
            case "svg" :
                mimeType = "image/svg+xml";
                break;
            case "ico" :
                mimeType = "image/x-icon";
                break;
            case "woff" :
            case "ttf" :
            case "eot" :
                mimeType = "application/x-font-opentype";
                break;
        }
        return mimeType;
    }
}
