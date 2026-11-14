package uz.akmal.aistudy10;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.graphics.Color;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public abstract class ProfileActivity extends Activity {
    private static boolean dataDirectoryInitialized = false;
    private static final String HOME="https://omp.aistudy.uz/";
    private final String[] rates={"1×","1.5×","2×","3×"};
    private final double[] rateValues={1,1.5,2,3};
    protected abstract int profileNumber();
    private WebView web;
    private TextView status;
    private Spinner speed;
    private CheckBox autoplay, next, autoTest;
    private String script;
    private String answersJson="[]";
    private FrameLayout webFrame;
    private View fullscreen;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // Called before ANY WebView instance or other android.webkit operation in this process.
        // Each of the ten activities is declared in a distinct Android process.
        if (!dataDirectoryInitialized) {
            WebView.setDataDirectorySuffix("aistudy_profile_"+profileNumber());
            dataDirectoryInitialized = true;
        }
        getWindow().setStatusBarColor(Ui.NAVY);
        getWindow().setNavigationBarColor(Ui.NAVY);
        try { script = loadAsset("companion.js"); answersJson = loadAsset("answers.json"); }
        catch(Exception e) { script=""; answersJson="[]"; }

        LinearLayout page=Ui.column(this);page.setBackgroundColor(Ui.NAVY);
        LinearLayout header=Ui.row(this);Ui.pad(header,this,6,7,6,6);
        Button close=Ui.button(this,"‹",Ui.PANEL);
        header.addView(close,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44)));
        close.setOnClickListener(v->finish());
        TextView name=Ui.label(this,"  AiStudy10 · Foydalanuvchi "+profileNumber(),16,Ui.WHITE,true);
        name.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.addView(name,new LinearLayout.LayoutParams(0,Ui.dp(this,44),1));
        Button help=Ui.button(this,"?",Ui.BLUE);
        header.addView(help,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44)));
        help.setOnClickListener(v -> showHelp());page.addView(header);

        LinearLayout controls=Ui.row(this);Ui.pad(controls,this,9,3,9,4);
        speed=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,rates);
        speed.setAdapter(adapter);speed.setSelection(3);
        controls.addView(speed,new LinearLayout.LayoutParams(Ui.dp(this,92),Ui.dp(this,43)));
        autoplay=new CheckBox(this);autoplay.setText("Avto ▶");autoplay.setTextColor(Ui.WHITE);
        autoplay.setChecked(true);controls.addView(autoplay);
        next=new CheckBox(this);next.setText("Keyingi");next.setTextColor(Ui.WHITE);
        next.setChecked(true);controls.addView(next);
        page.addView(controls);

        LinearLayout second=Ui.row(this);Ui.pad(second,this,8,0,8,4);
        autoTest=new CheckBox(this);autoTest.setText("Test yordamchisi");autoTest.setTextColor(Ui.WHITE);
        autoTest.setChecked(false);second.addView(autoTest);
        Button reload=Ui.button(this,"↻ Yangilash",Ui.PANEL);
        second.addView(reload,Ui.lp(this,122,42,0));
        reload.setOnClickListener(v->web.reload());page.addView(second);

        status=Ui.label(this,"Sayt yuklanmoqda...",12,Ui.MUTED,false);
        Ui.pad(status,this,12,3,12,7);page.addView(status);
        webFrame=new FrameLayout(this);page.addView(webFrame,new LinearLayout.LayoutParams(-1,0,1));
        web=new WebView(this);web.setBackgroundColor(Color.WHITE);
        webFrame.addView(web,new FrameLayout.LayoutParams(-1,-1));
        WebSettings ws=web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        ws.setSupportMultipleWindows(false);
        ws.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view,String url) {
                status.setText("Sayt ochildi · "+profileNumber()+"-profil");
                inject(url);
                view.postDelayed(() -> inject(view.getUrl()),1900);
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error) {
                if(req.isForMainFrame()) status.setText("Ulanish xatosi. Internetni tekshiring va ↻ ni bosing.");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if(fullscreen!=null) {callback.onCustomViewHidden();return;}
                fullscreen=view;fullscreenCallback=callback;
                web.setVisibility(View.GONE);
                webFrame.addView(view,new FrameLayout.LayoutParams(-1,-1));
            }
            @Override public void onHideCustomView() {
                if(fullscreen==null)return;
                webFrame.removeView(fullscreen);fullscreen=null;
                web.setVisibility(View.VISIBLE);
                if(fullscreenCallback!=null)fullscreenCallback.onCustomViewHidden();
                fullscreenCallback=null;
            }
        });
        android.widget.AdapterView.OnItemSelectedListener changes=new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){updateConfig();}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        };
        speed.setOnItemSelectedListener(changes);
        autoplay.setOnCheckedChangeListener((v,b)->updateConfig());
        next.setOnCheckedChangeListener((v,b)->updateConfig());
        autoTest.setOnCheckedChangeListener((v,b)->updateConfig());
        setContentView(page);
        web.loadUrl(HOME);
    }

    private String loadAsset(String file) throws Exception {
        try (InputStream in = getAssets().open(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int n;
            while ((n=in.read(buffer))!=-1) out.write(buffer,0,n);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private String options() {
        return "{rate:"+rateValues[speed.getSelectedItemPosition()]+",autoplay:"+autoplay.isChecked()
          +",autoNext:"+next.isChecked()+",quizAssist:"+autoTest.isChecked()+"}";
    }
    private void updateConfig() {
        if(web==null)return;
        String url=web.getUrl();if(url==null||!"omp.aistudy.uz".equals(Uri.parse(url).getHost()))return;
        web.evaluateJavascript("if(window.__AIS_ANDROID){window.__AIS_ANDROID.configure("+options()+");}",null);
    }
    private void inject(String url) {
        if(url==null||!"omp.aistudy.uz".equals(Uri.parse(url).getHost()))return;
        if(script.isEmpty())return;
        web.evaluateJavascript("window.__AIS_ANDROID_OPTIONS="+options()+";\nwindow.__AIS_ANDROID_ANSWERS="+answersJson+";\n"+script,null);
    }
    private void showHelp(){new AlertDialog.Builder(this).setTitle("Foydalanish yo‘riqnomasi")
        .setMessage("• Avval saytda o‘z login-parolingiz bilan kiring.\n• Video oynasini oching; 3× tezlikni tanlang.\n• Avto ▶ va Keyingi yoqilganida yangi darsga o‘tishga urinadi.\n• Test yordamchisi tanlangan bo‘lsa, ma’lum javoblarni taklif qiladi; yuborish qo‘lda tasdiqlanadi.\n• Video vaqtini tiklash saytdagi pleerga bog‘liq.\n• Fon rejimida Android videoni to‘xtatishi mumkin.\n\nMuallif: Akmal Muxamedjanov")
        .setPositiveButton("Yopish",(d,w)->{}).show();}
    @Override public void onBackPressed(){
        if(fullscreen!=null){web.getWebChromeClient().onHideCustomView();return;}
        if(web!=null&&web.canGoBack()){web.goBack();return;}
        super.onBackPressed();
    }
    @Override protected void onDestroy(){
        if(web!=null){web.stopLoading();webFrame.removeView(web);web.destroy();web=null;}
        super.onDestroy();
    }
}
