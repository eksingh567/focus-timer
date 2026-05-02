package com.example.focustimersystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.JavascriptInterface;

public class LockScreenActivity extends AppCompatActivity {

    private static final String DEFAULT_HUB_IP = "10.0.2.2";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        String savedIp = getSharedPreferences("FocusPrefs", MODE_PRIVATE).getString("hub_ip", DEFAULT_HUB_IP);
        String serverUrl = "http://" + savedIp + ":8000/mobile";

        webView = findViewById(R.id.webViewLock);
        webView.setBackgroundColor(android.graphics.Color.parseColor("#0F0F12"));
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showError(view, failingUrl, description);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showError(view, request.getUrl().toString(), error.getDescription().toString());
                }
            }

            private void showError(WebView view, String failingUrl, String description) {
                String errorHtml = "<html><body style='background:#0F0F12;color:white;padding:40px;text-align:center;font-family:sans-serif;'>" +
                        "<h2>Mode Locked</h2>" +
                        "<p style='color:#F5A623;'>Communication with Focus Hub failed.</p>" +
                        "<p style='font-size:12px;color:#555;'>Target: " + failingUrl + "</p>" +
                        "<p style='font-size:12px;color:#555;'>Error: " + description + "</p>" +
                        "</body></html>";
                view.loadData(errorHtml, "text/html", "UTF-8");
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("/mobile_celeb")) {
                    finish();
                    return true;
                }
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new LockAppInterface(), "AndroidLock");
        webView.loadUrl(serverUrl);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            startLockTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class LockAppInterface {
        @JavascriptInterface
        public void finishLock() {
            runOnUiThread(() -> {
                try {
                    stopLockTask();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            });
        }
    }

    @Override
    public void onBackPressed() {
        // Intercept back button to prevent escaping the lock screen
    }
}
