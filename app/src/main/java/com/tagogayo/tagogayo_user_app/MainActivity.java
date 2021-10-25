package com.tagogayo.tagogayo_user_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.net.URISyntaxException;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mWebView = findViewById(R.id.webView);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        this.mWebView.setWebChromeClient(new MyWebChromeClient());
        this.mWebView.setWebViewClient(new MyWebViewClient(getApplicationContext()));


        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg = token;
                        System.out.println("토큰:"+msg);
                        mWebView.loadUrl("https://tagogayo.co.kr?push_token="+msg);
//                        Toast.makeText(MainActivity.this, "https://tagogayo.kr?push_token="+msg, Toast.LENGTH_SHORT).show();
                    }
                });


    }

    private long backBtnTime = 0;

    @Override
    public void onBackPressed() {
        long curTime = System.currentTimeMillis();
        long gapTime = curTime - backBtnTime;
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else if (0 <= gapTime && 2000 >= gapTime) {
            super.onBackPressed();
        } else {
            backBtnTime = curTime;
            Toast.makeText(this, "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }


    public class MyWebViewClient extends WebViewClient {
        private String TAG = "MyWebViewClient";
        private Context mApplicationContext = null;

        public MyWebViewClient(Context _applicationContext) {
            mApplicationContext = _applicationContext;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = parse(url);
            if (url != null && url.startsWith("intent://")) {
                try {
                    Intent intent3 = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent3.getPackage());
                    if (existPackage != null) {
                        startActivity(intent3);
                    } else {
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                        marketIntent.setData(Uri.parse("market://details?id="+intent.getPackage()));
                        startActivity(marketIntent);
                    }
                    return true;
                }catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (url != null && url.startsWith("market://")) {
                try {
                    Intent intent3 = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent3 != null) {
                        startActivity(intent3);
                    }
                    return true;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }else if (isIntent(url)) {
                if (isExistInfo(intent, view.getContext()) || isExistPackage(intent, view.getContext()))
                    return start(intent, view.getContext());
                else
                    gotoMarket(intent, view.getContext());
            }else if(url.startsWith("tel:")){
                Intent dial = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                //현재의 activity 에 대하여 startActivity 호출
                startActivity(dial);
                return true;
            } else if (isMarket(url)) {
                return start(intent, view.getContext());
            }
            return url.contains("https://bootpaymark");
        }

        private Intent parse(String url) {
            try {
                return Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
        }

        private Boolean isIntent(String url) {
            return url.matches("^intent:?\\w*://\\S+$");
        }

        private Boolean isMarket(String url) {
            return url.matches("^market://\\S+$");
        }

        private Boolean isExistInfo(Intent intent, Context context) {
            try {
                return intent != null && context.getPackageManager().getPackageInfo(intent.getPackage(), PackageManager.GET_ACTIVITIES) != null;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }

        private Boolean isExistPackage(Intent intent, Context context) {
            return intent != null && context.getPackageManager().getLaunchIntentForPackage(intent.getPackage()) != null;
        }

        private boolean start(Intent intent, Context context) {
            context.startActivity(intent);
            return true;
        }

        private boolean gotoMarket(Intent intent, Context context) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + intent.getPackage())));
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                onReceivedError(error.getErrorCode(), String.valueOf(error.getDescription()));
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                onReceivedError(errorCode, description);
            }
        }

        private void onReceivedError(int errorCode, String description) {
            switch (errorCode) {
                case WebViewClient.ERROR_TIMEOUT: //연결 시간 초과
                case WebViewClient.ERROR_CONNECT: //서버로 연결 실패
                case WebViewClient.ERROR_UNKNOWN: // 일반 오류
                case WebViewClient.ERROR_FILE_NOT_FOUND: //404
                case WebViewClient.ERROR_HOST_LOOKUP:
                case WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME:
                case WebViewClient.ERROR_AUTHENTICATION:
                case WebViewClient.ERROR_PROXY_AUTHENTICATION:
                case WebViewClient.ERROR_IO:
                case WebViewClient.ERROR_REDIRECT_LOOP:
                case WebViewClient.ERROR_UNSUPPORTED_SCHEME:
                case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                case WebViewClient.ERROR_BAD_URL:
                case WebViewClient.ERROR_FILE:
                case WebViewClient.ERROR_TOO_MANY_REQUESTS:
                case WebViewClient.ERROR_UNSAFE_RESOURCE:

                    Log.e(TAG, "WebViewClient,onReceivedError(" + errorCode + ") 에러 발생 ");
                    break;
            }
        }

    }


    public class MyWebChromeClient extends WebChromeClient {
        private final String TAG = "MyWebChromeClient";

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            Log.i(TAG, "onProgressChanged(view:" + view.toString() + ", newProgress:" + newProgress + ")");
        }

        @Override
        public boolean onCreateWindow(final WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            final WebView newWebView = new WebView(view.getContext());
            WebSettings webSettings = newWebView.getSettings();
            WebSettings settings = newWebView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setSupportMultipleWindows(true); //final Dialog dialog = new Dialog(view.getContext(),R.style.Theme_DialogFullScreen);
            final Dialog dialog = new Dialog(view.getContext());
            dialog.setContentView(newWebView);
            dialog.show();
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) { //Log.toastMakeTextShow(view.getContext(), "TAG", "KEYCODE_BACK");
                        if (newWebView.canGoBack()) {
                            newWebView.goBack();
                        } else {
                            dialog.dismiss();
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            newWebView.setWebViewClient(new MyWebViewClient(view.getContext()));
            newWebView.setWebChromeClient(new MyWebChromeClient() {
                @Override
                public void onCloseWindow(WebView window) {
                    dialog.dismiss();
                }
            });
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(newWebView);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            Log.i(getClass().getName(), "onCloseWindow");
            window.setVisibility(View.GONE);
            window.destroy(); //mWebViewSub=null;
            super.onCloseWindow(window);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            Log.i(getClass().getName(), "onJsAlert() url:" + url + ", message:" + message); //return super.onJsAlert(view, url, message, result);
            new AlertDialog.Builder(view.getContext()).setTitle("").setMessage(message).setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setCancelable(false).create().show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            Log.i(getClass().getName(), "onJsConfirm() url:" + url + ", message" + message); //return super.onJsConfirm(view, url, message, result);
            new AlertDialog.Builder(view.getContext()).setTitle("").setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            }).create().show();
            return true;
        }
    }


}
