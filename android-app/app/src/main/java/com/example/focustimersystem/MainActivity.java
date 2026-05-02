package com.example.focustimersystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    // Default hub IP (fallback)
    private static final String DEFAULT_HUB_IP = "10.0.2.2";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String savedIp = getSharedPreferences("FocusPrefs", MODE_PRIVATE).getString("hub_ip", DEFAULT_HUB_IP);
        String serverUrl = "http://" + savedIp + ":8000/mobile";

        webView = findViewById(R.id.webViewMain);
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
                String errorHtml = "<html><body style='background:#0F0F12;color:white;padding:40px;font-family:sans-serif;'>" +
                        "<h2>Connection Error</h2>" +
                        "<p>Could not connect to: " + failingUrl + "</p>" +
                        "<p>Error: " + description + "</p>" +
                        "<button onclick='location.reload()' style='background:#F5A623;padding:10px 20px;border:none;border-radius:8px;'>RETRY</button>" +
                        "</body></html>";
                view.loadData(errorHtml, "text/html", "UTF-8");
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();

        // Interface to let HTML trigger Android actions natively
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidIoT");

        webView.loadUrl(serverUrl);

        // Start polling service
        Intent serviceIntent = new Intent(this, AppLockService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void saveServerIp(String ip) {
            getSharedPreferences("FocusPrefs", MODE_PRIVATE).edit().putString("hub_ip", ip).apply();
            // Restart service to use new IP
            Intent serviceIntent = new Intent(MainActivity.this, AppLockService.class);
            stopService(serviceIntent);
            ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
            
            // Reload WebView with new IP
            runOnUiThread(() -> {
                String newUrl = "http://" + ip + ":8000/mobile";
                webView.loadUrl(newUrl);
            });
        }

        @JavascriptInterface
        public void showIpDialog() {
            runOnUiThread(() -> {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Connect to Focus Hub");
                builder.setMessage("Enter your laptop's Hub IP address:");
                
                final android.widget.EditText input = new android.widget.EditText(MainActivity.this);
                input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                String currentIp = getSharedPreferences("FocusPrefs", MODE_PRIVATE).getString("hub_ip", DEFAULT_HUB_IP);
                input.setText(currentIp);
                
                builder.setView(input);
                builder.setPositiveButton("SYNC", (dialog, which) -> {
                    saveServerIp(input.getText().toString());
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                builder.show();
            });
        }

        @JavascriptInterface
        public void startServiceNative() {
            Intent serviceIntent = new Intent(MainActivity.this, AppLockService.class);
            ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
        }

        @JavascriptInterface
        public void stopServiceNative() {
            stopService(new Intent(MainActivity.this, AppLockService.class));
        }

        @JavascriptInterface
        public void openUsageAccess() {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasUsageStatsPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
    
    private boolean hasUsageStatsPermission() {
        android.app.AppOpsManager appOps = (android.app.AppOpsManager) getSystemService(android.content.Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == android.app.AppOpsManager.MODE_ALLOWED;
    }


}
