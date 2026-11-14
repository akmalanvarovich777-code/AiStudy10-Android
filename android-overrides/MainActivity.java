package uz.akmal.aistudy10;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int NAVY=Color.rgb(12,22,41), PANEL=Color.rgb(23,41,68), BLUE=Color.rgb(35,104,220);
    private static final String HOME="https://omp.aistudy.uz/", EXT_ID="aistudy10-companion@akmal.local";
    private final GeckoSession[] sessions=new GeckoSession[10];
    private final GeckoView[] views=new GeckoView[10];
    private final String[] statuses=new String[10];
    private final Button[] profileButtons=new Button[10];
    private GeckoRuntime runtime;
    private LinearLayout shell, toolbar, hiddenHost;
    private FrameLayout content;
    private TextView statusLabel;
    private SharedPreferences options;
    private int selected=0;
    private boolean tenPanel=false, ready=false;

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private LinearLayout col(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);return x;}
    private LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);return x;}
    private TextView label(String t,int size,int color){TextView x=new TextView(this);x.setText(t);x.setTextColor(color);x.setTextSize(size);x.setGravity(Gravity.CENTER_VERTICAL);x.setPadding(dp(6),dp(4),dp(6),dp(4));return x;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));b.setMinHeight(dp(42));b.setMinimumHeight(dp(42));return b;}
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-1,-2);}
    private LinearLayout.LayoutParams tabparams(){return new LinearLayout.LayoutParams(-2,dp(46));}

    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
        options=getSharedPreferences("aistudy10_settings",MODE_PRIVATE);
        for(int i=0;i<10;i++)statuses[i]="Ochilmoqda...";
        buildShell();
        // GeckoView contextId stores cookies and localStorage separately for each profile.
        runtime=GeckoRuntime.create(this);
        for(int i=0;i<10;i++){
            final int n=i;
            GeckoSessionSettings settings=new GeckoSessionSettings.Builder()
                .contextId("aistudy-profile-"+(i+1))
                .suspendMediaWhenInactive(false)
                .allowJavascript(true)
                .build();
            sessions[i]=new GeckoSession(settings);
            sessions[i].open(runtime);
            views[i]=new GeckoView(this);
            views[i].setSession(sessions[i]);
        }
        renderPanels();
        runtime.getWebExtensionController().ensureBuiltIn(
            "resource://android/assets/aistudy/",EXT_ID).accept(extension->{
                for(int i=0;i<10;i++){
                    final int idx=i;
                    sessions[i].getWebExtensionController().setMessageDelegate(extension,
                        new WebExtension.MessageDelegate(){
                            @Override public GeckoResult<Object> onMessage(String nativeApp,Object message,WebExtension.MessageSender sender){
                                if(!(message instanceof JSONObject))return GeckoResult.fromValue(null);
                                if(sender.session!=sessions[idx])return GeckoResult.fromValue(null);
                                JSONObject input=(JSONObject)message;
                                String type=input.optString("type","");
                                if("config".equals(type))return GeckoResult.fromValue(getConfiguration());
                                if("status".equals(type)){
                                    final String text=input.optString("text","Dars ochiq");
                                    runOnUiThread(()->{statuses[idx]=text;if(selected==idx)statusLabel.setText("Profil "+(idx+1)+": "+text);updateProfileButtons();});
                                }
                                return GeckoResult.fromValue(null);
                            }
                        },"aistudy");
                }
                ready=true;
                for(GeckoSession s:sessions)s.loadUri(HOME);
                runOnUiThread(()->{statusLabel.setText("10 ta sessiya ochildi. Har birida login qiling.");updateProfileButtons();});
            },error->runOnUiThread(()->{
                statusLabel.setText("Ichki kengaytma yuklanmadi: "+error.getMessage());
                Toast.makeText(this,"Kengaytma yuklanmadi",Toast.LENGTH_LONG).show();
                for(GeckoSession s:sessions)s.loadUri(HOME);
            }));
    }
    private JSONObject getConfiguration(){
        JSONObject j=new JSONObject();
        try{
            j.put("rate",options.getFloat("rate",2f));
            j.put("autoplay",options.getBoolean("autoplay",true));
            j.put("autoNext",options.getBoolean("autoNext",true));
            j.put("quizAssist",options.getBoolean("quizAssist",false));
            j.put("guessMissing",options.getBoolean("guessMissing",false));
            j.put("autoSubmit",options.getBoolean("autoSubmit",false));
            j.put("autoCourse",options.getBoolean("autoCourse",true));
            j.put("replayMissing",options.getBoolean("replayMissing",true));
        }catch(Exception ignored){}
        return j;
    }
    private void buildShell(){
        shell=col();shell.setBackgroundColor(NAVY);setContentView(shell);
        LinearLayout brand=row();brand.setPadding(dp(6),dp(3),dp(6),dp(1));
        TextView title=label("AiStudy10 · Android",18,Color.WHITE);title.setTypeface(null,1);brand.addView(title,new LinearLayout.LayoutParams(0,dp(45),1));
        Button guide=button("? Yo‘riqnoma");brand.addView(guide,tabparams());guide.setOnClickListener(v->showGuide());shell.addView(brand);
        HorizontalScrollView nav=new HorizontalScrollView(this);nav.setHorizontalScrollBarEnabled(false);
        LinearLayout navRow=row();navRow.setPadding(dp(4),0,dp(4),0);
        Button mode=button("1 / 10 panel");navRow.addView(mode,tabparams());
        mode.setOnClickListener(v->{tenPanel=!tenPanel;mode.setText(tenPanel?"10 panel":"1 panel");renderPanels();});
        Button speed=button(speedLabel(options.getFloat("rate",2f)));navRow.addView(speed,tabparams());speed.setOnClickListener(v->{
            final String[] choices={"1×","1.25×","1.5×","2×","3×","4×","5×"};
            final float[] values={1f,1.25f,1.5f,2f,3f,4f,5f};
            int checked=0;float current=options.getFloat("rate",2f);
            for(int i=0;i<values.length;i++)if(Math.abs(values[i]-current)<0.01f)checked=i;
            new AlertDialog.Builder(this).setTitle("Video tezligi").setSingleChoiceItems(choices,checked,(d,which)->{
                options.edit().putFloat("rate",values[which]).apply();speed.setText(choices[which]);d.dismiss();
            }).show();
        });
        solveButton=button("✓ Testni ishlash");navRow.addView(solveButton,tabparams());updateSolveButton();solveButton.setOnClickListener(v->{
            boolean enabled=!options.getBoolean("quizAssist",false);
            options.edit().putBoolean("quizAssist",enabled).putBoolean("guessMissing",false).putBoolean("autoSubmit",false).apply();
            updateSolveButton();
            Toast.makeText(this,enabled?"Test yordamchisi yoqildi: bazadagi aniq javoblar tanlanadi, topilmaganlari sizga qoldiriladi":"Test yordamchisi o‘chirildi",Toast.LENGTH_LONG).show();
        });
        Button help=button("⚙ Avto sozlamalar");navRow.addView(help,tabparams());help.setOnClickListener(v->showOptions());
        Button reload=button("↻ Yangilash");navRow.addView(reload,tabparams());reload.setOnClickListener(v->sessions[selected].reload());
        Button bg=button("▶ Fon rejimi");navRow.addView(bg,tabparams());bg.setOnClickListener(v->toggleService());
        nav.addView(navRow);shell.addView(nav,wrap());
        HorizontalScrollView profiles=new HorizontalScrollView(this);profiles.setHorizontalScrollBarEnabled(false);
        LinearLayout pRow=row();pRow.setPadding(dp(4),dp(2),dp(4),dp(2));
        for(int i=0;i<10;i++){final int n=i;Button p=button(String.valueOf(i+1));profileButtons[i]=p;pRow.addView(p,new LinearLayout.LayoutParams(dp(52),dp(46)));p.setOnClickListener(v->{selected=n;if(!tenPanel)renderPanels();else updateProfileButtons();statusLabel.setText("Profil "+(n+1)+": "+statuses[n]);});}
        profiles.addView(pRow);shell.addView(profiles,wrap());
        statusLabel=label("Sayt ochilmoqda...",12,Color.rgb(196,214,246));shell.addView(statusLabel,wrap());
        content=new FrameLayout(this);shell.addView(content,new LinearLayout.LayoutParams(-1,0,1));updateProfileButtons();
        TextView author=label("Muallif: Akmal Muxamedjanov · Mustaqil yordamchi dastur",11,Color.rgb(174,191,213));
        author.setGravity(Gravity.CENTER);shell.addView(author,wrap());
    }
    private String speedLabel(float rate){return (rate==(int)rate?Integer.toString((int)rate):Float.toString(rate))+"×";}
    private void updateSolveButton(){
        if(solveButton!=null){boolean enabled=options.getBoolean("quizAssist",false);solveButton.setText(enabled?"✓ Testni ishlash: yoqildi":"✓ Testni ishlash");solveButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(enabled?BLUE:PANEL));}
    }
    private void updateProfileButtons(){
        for(int i=0;i<10;i++){
            if(profileButtons[i]==null)continue;
            profileButtons[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(i==selected?BLUE:PANEL));
            profileButtons[i].setText(String.valueOf(i+1)+(statuses[i].contains("Video")?" ▶":""));
        }
    }
    private void detachViews(){
        for(GeckoView v:views){if(v!=null&&v.getParent() instanceof ViewGroup)((ViewGroup)v.getParent()).removeView(v);}
    }
    private void renderPanels(){
        if(content==null||views[0]==null)return;
        detachViews();content.removeAllViews();
        LinearLayout column=col();column.setBackgroundColor(NAVY);
        if(tenPanel){
            ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
            LinearLayout layout=col();
            for(int r=0;r<5;r++){
                LinearLayout pair=row();
                for(int c=0;c<2;c++){
                    final int idx=r*2+c;
                    LinearLayout tile=col();tile.setPadding(dp(2),dp(2),dp(2),dp(2));tile.setBackgroundColor(PANEL);
                    TextView heading=label("Profil "+(idx+1),12,Color.WHITE);
                    heading.setOnClickListener(v->{selected=idx;tenPanel=false;renderPanels();});
                    tile.addView(heading,wrap());
                    tile.addView(views[idx],new LinearLayout.LayoutParams(-1,dp(260)));
                    pair.addView(tile,new LinearLayout.LayoutParams(0,dp(295),1));
                }
                layout.addView(pair,wrap());
            }
            scroll.addView(layout);column.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        }else{
            column.addView(views[selected],new LinearLayout.LayoutParams(-1,0,1));
            // Keep all 9 off-screen GeckoViews attached; sessions are never closed when switching.
            hiddenHost=col();hiddenHost.setAlpha(0.02f);
            for(int i=0;i<10;i++)if(i!=selected)hiddenHost.addView(views[i],new LinearLayout.LayoutParams(dp(2),dp(2)));
            column.addView(hiddenHost,new LinearLayout.LayoutParams(dp(2),dp(18)));
        }
        content.addView(column,new FrameLayout.LayoutParams(-1,-1));
        updateProfileButtons();
    }
    private void showGuide(){new AlertDialog.Builder(this).setTitle("AiStudy10 · Yo‘riqnoma")
        .setMessage("1. Har bir profilni tanlab omp.aistudy.uz hisobiga kiring.\n2. '10 panel' barcha sessiyalarni ko‘rsatadi; '1 panel' tanlanganini kattalashtiradi.\n3. Tezlikni tanlang, avto video va keyingi darsni ⚙ da boshqaring.\n4. Test yordamchisini faqat o‘zingiz ruxsat bergan testlarda yoqing.\n5. Fon rejimi telefon ekran o‘chganida 10 ta video ishlashini kafolatlamaydi.\n6. Tarmoq/CORS server xatolarini dastur tuzata olmaydi.\n\nMuallif: Akmal Muxamedjanov")
        .setPositiveButton("Tushunarli",(d,w)->{}).show();}
    private void showOptions(){
        String[] labels={"Avto ▶ video", "Keyingi dars", "Test yordamchisi", "Topilmasa taxmin", "Testni yuborish", "Avto modul/kurs", "Ko‘rilmagan darsni qayta ko‘rish"};
        String[] keys={"autoplay","autoNext","quizAssist","guessMissing","autoSubmit","autoCourse","replayMissing"};
        boolean[] state=new boolean[keys.length];
        for(int i=0;i<keys.length;i++)state[i]=options.getBoolean(keys[i],i<2||i>=5);
        new AlertDialog.Builder(this).setTitle("Avto sozlamalar")
            .setMultiChoiceItems(labels,state,(d,n,b)->state[n]=b)
            .setPositiveButton("Saqlash",(d,w)->{
                SharedPreferences.Editor ed=options.edit();for(int i=0;i<keys.length;i++)ed.putBoolean(keys[i],state[i]);ed.apply();updateSolveButton();
                if(state[4]||state[3])Toast.makeText(this,"Taxminiy javoblar xato bo‘lishi mumkin",Toast.LENGTH_LONG).show();
            }).setNegativeButton("Bekor qilish",(d,w)->{}).show();
    }
    private boolean runningService=false;
    private void toggleService(){
        if(!runningService){
            try{Intent i=new Intent(this,PlaybackService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
                runningService=true;Toast.makeText(this,"Fon rejimi yoqildi",Toast.LENGTH_SHORT).show();}
            catch(Exception e){Toast.makeText(this,"Fon rejimi: "+e.getMessage(),Toast.LENGTH_LONG).show();}
        }else{stopService(new Intent(this,PlaybackService.class));runningService=false;Toast.makeText(this,"Fon rejimi o‘chdi",Toast.LENGTH_SHORT).show();}
    }
    @Override public void onBackPressed(){if(ready)sessions[selected].goBack();else super.onBackPressed();}
    @Override public void onDestroy(){
        for(GeckoView v:views)if(v!=null)v.releaseSession();
        for(GeckoSession s:sessions)if(s!=null&&s.isOpen())s.close();
        super.onDestroy();
    }
}
