package uz.akmal.aistudy10;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
    private static final int IMPORT_ANSWERS_REQUEST=1117;
    private static final int NAVY=Color.rgb(12,22,41), PANEL=Color.rgb(23,41,68), BLUE=Color.rgb(35,104,220);
    private static final String HOME="https://omp.aistudy.uz/", EXT_ID="aistudy10-companion@akmal.local";
    private final GeckoSession[] sessions=new GeckoSession[10];
    private final GeckoView[] views=new GeckoView[10];
    private final String[] statuses=new String[10];
    private final Button[] profileButtons=new Button[10];
    private final String[] pendingCommands = new String[10];
    private String pendingResearch = null;
    private boolean searchFromUser = false;
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
                                if("config".equals(type))return GeckoResult.fromValue(getConfiguration(idx));
                                if("researchQuestion".equals(type)) {
                                    final String q=input.optString("question","");
                                    runOnUiThread(()->searchQuestion(q));
                                    return GeckoResult.fromValue(null);
                                }
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
    private JSONObject getConfiguration(int idx){
        JSONObject j=new JSONObject();
        try{
            j.put("rate",2.0); // Fixed 2x across all 10 sessions.
            j.put("autoplay",options.getBoolean("autoplay",true));
            j.put("mute",options.getBoolean("mute",true));
            j.put("autoTest",options.getBoolean("autoTest",true));
            j.put("autoResearch",options.getBoolean("autoResearch",false));
            j.put("autoNext",options.getBoolean("autoNext",true));
            j.put("quizAssist",options.getBoolean("quizAssist",true));
            j.put("guessMissing",options.getBoolean("guessMissing",false));
            j.put("autoSubmit",options.getBoolean("autoSubmit",false));
            j.put("autoCourse",options.getBoolean("autoCourse",true));
            j.put("replayMissing",options.getBoolean("replayMissing",true));
            j.put("answers", new org.json.JSONArray(options.getString("importedAnswers", "[]")));
            synchronized (pendingCommands) {
                if (pendingCommands[idx] != null) {
                    j.put("command",pendingCommands[idx]);
                    pendingCommands[idx]=null;
                }
            }
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
        Button speed=button("Tezlik: 2×");navRow.addView(speed,tabparams());
        speed.setOnClickListener(v->Toast.makeText(this,"Video tezligi faqat 2×",Toast.LENGTH_SHORT).show());
        Button help=button("⚙ Avto sozlamalar");navRow.addView(help,tabparams());help.setOnClickListener(v->showOptions());
        Button play=button("▶ Video");navRow.addView(play,tabparams());play.setOnClickListener(v->queueCommand("play"));
        Button next=button("» Keyingi");navRow.addView(next,tabparams());next.setOnClickListener(v->queueCommand("next"));
        Button test=button("▣ Testga o'tish");navRow.addView(test,tabparams());test.setOnClickListener(v->queueCommand("test"));
        Button answer=button("✓ Javob");navRow.addView(answer,tabparams());answer.setOnClickListener(v->queueCommand("answer"));
        Button research=button("⌕ Savol izlash");navRow.addView(research,tabparams());research.setOnClickListener(v->{searchFromUser=true;queueCommand("research");});
        Button analyse=button("⌕ Holat tahlili");navRow.addView(analyse,tabparams());analyse.setOnClickListener(v->queueCommand("diagnose"));
        Button teach=button("✚ Tugmani tanitish");navRow.addView(teach,tabparams());teach.setOnClickListener(v->queueCommand("teach"));
        Button clearTeach=button("Tanitishni tozalash");navRow.addView(clearTeach,tabparams());clearTeach.setOnClickListener(v->queueCommand("clearTeach"));
        Button recheck=button("✓ Qayta tekshirish");navRow.addView(recheck,tabparams());recheck.setOnClickListener(v->queueCommand("recheck"));
        Button importDb=button("⇪ Javoblar bazasi");navRow.addView(importDb,tabparams());importDb.setOnClickListener(v->pickAnswers());
        Button back=button("← Orqaga");navRow.addView(back,tabparams());back.setOnClickListener(v->sessions[selected].goBack());
        Button home=button("⌂ Boshi");navRow.addView(home,tabparams());home.setOnClickListener(v->sessions[selected].loadUri(HOME));
        Button reload=button("↻ Yangilash");navRow.addView(reload,tabparams());reload.setOnClickListener(v->sessions[selected].reload());
        Button reloadAll=button("↻ 10 ta panelni yangilash");navRow.addView(reloadAll,tabparams());reloadAll.setOnClickListener(v->{for(GeckoSession sess:sessions)if(sess!=null)sess.reload();});
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
        .setMessage("1. Har bir profilni tanlab omp.aistudy.uz hisobiga kiring.\n2. '10 panel' barcha sessiyalarni ko‘rsatadi; '1 panel' tanlanganini kattalashtiradi.\n3. Video tezligi qat’iy 2×. Avto video va keyingi darsni ⚙ orqali boshqaring.\n4. Test yordamchisini faqat o‘zingiz ruxsat bergan testlarda yoqing.\n5. Fon rejimi telefon ekran o‘chganida 10 ta video ishlashini kafolatlamaydi.\n6. Tarmoq/CORS server xatolarini dastur tuzata olmaydi.\n\nMuallif: Akmal Muxamedjanov")
        .setPositiveButton("Tushunarli",(d,w)->{}).show();}
    private void showOptions(){
        String[] labels={"Avto ▶ video", "Keyingi dars", "Testni avtomatik ochish", "Test yordamchisi / avto javob", "Topilmasa taxmin", "Testni avtomatik yuborish", "Avto modul/kurs", "Ko‘rilmagan darsni qayta ko‘rish", "Ovozni o‘chirish", "Savolni internetdan izlash"};
        String[] keys={"autoplay","autoNext","autoTest","quizAssist","guessMissing","autoSubmit","autoCourse","replayMissing","mute","autoResearch"};
        boolean[] state=new boolean[keys.length];
        for(int i=0;i<keys.length;i++)state[i]=options.getBoolean(keys[i],i==0||i==1||i==2||i==6||i==7||i==8);
        new AlertDialog.Builder(this).setTitle("Avto sozlamalar")
            .setMultiChoiceItems(labels,state,(d,n,b)->state[n]=b)
            .setPositiveButton("Saqlash",(d,w)->{
                SharedPreferences.Editor ed=options.edit();for(int i=0;i<keys.length;i++)ed.putBoolean(keys[i],state[i]);ed.apply();
                if(state[5]||state[4])Toast.makeText(this,"Taxminiy javoblar xato bo‘lishi mumkin",Toast.LENGTH_LONG).show();
            }).setNegativeButton("Bekor qilish",(d,w)->{}).show();
    }
    // Commands are delivered only to the selected Gecko session through native messaging.
    private void queueCommand(String action){
        synchronized(pendingCommands){pendingCommands[selected]=action;}
        Toast.makeText(this,"Profil "+(selected+1)+": "+action,Toast.LENGTH_SHORT).show();
    }
    private void pickAnswers(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","text/javascript","application/javascript"});
        startActivityForResult(i,IMPORT_ANSWERS_REQUEST);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=IMPORT_ANSWERS_REQUEST||result!=RESULT_OK||data==null||data.getData()==null)return;
        try(InputStream input=getContentResolver().openInputStream(data.getData())) {
            if(input==null)throw new Exception("Fayl ochilmadi");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            byte[] buf=new byte[4096];int n;
            while((n=input.read(buf))!=-1){bytes.write(buf,0,n);if(bytes.size()>180000)throw new Exception("Fayl juda katta");}
            String source=new String(bytes.toByteArray(),StandardCharsets.UTF_8);
            org.json.JSONArray imported=parseAnswerFile(source);
            if(imported.length()==0)throw new Exception("ANSWERS_DB topilmadi");
            options.edit().putString("importedAnswers",imported.toString()).apply();
            Toast.makeText(this,imported.length()+" ta javob import qilindi",Toast.LENGTH_LONG).show();
        }catch(Exception ex){new AlertDialog.Builder(this).setTitle("Import xatosi")
          .setMessage(ex.getMessage()).setPositiveButton("OK",null).show();}
    }
    private org.json.JSONArray parseAnswerFile(String source)throws Exception {
        org.json.JSONArray out=new org.json.JSONArray();
        String t=source.trim();
        if(t.startsWith("[")){
            org.json.JSONArray arr=new org.json.JSONArray(t);
            for(int i=0;i<arr.length();i++){
                JSONObject item=arr.optJSONObject(i);
                if(item!=null&&item.optString("question","").length()>3&&item.optString("answer","").length()>0)out.put(item);
            }
            return out;
        }
        int a=source.indexOf("ANSWERS_DB");if(a<0)return out;
        int open=source.indexOf('{',a);int close=source.indexOf("};",open);
        if(open<0||close<open)return out;
        String object=source.substring(open,close+1);
        // Parse as JSON only; never execute content.js (untrusted JavaScript).
        JSONObject db=new JSONObject(object);
        java.util.Iterator<String> keys=db.keys();
        while(keys.hasNext()){
            String key=keys.next();String answer=db.optString(key,"");
            if(key.length()<4||answer.length()<1)continue;
            JSONObject row=new JSONObject();row.put("question",key);row.put("answer",answer);out.put(row);
        }
        return out;
    }
    private void searchQuestion(String question){
        if(!searchFromUser)return;
        searchFromUser=false;
        if(question==null||question.trim().isEmpty()){
            Toast.makeText(this,"Savol topilmadi",Toast.LENGTH_SHORT).show();return;
        }
        new AlertDialog.Builder(this).setTitle("Internetdan savol izlash")
          .setMessage(question+"\n\nSavol matni qidiruv tizimiga yuboriladi. Javob tasdiqlanmagan.")
          .setPositiveButton("Qidirish",(d,w)->{
              String url="https://www.google.com/search?q="+Uri.encode(question);
              startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
          }).setNegativeButton("Bekor qilish",null).show();
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