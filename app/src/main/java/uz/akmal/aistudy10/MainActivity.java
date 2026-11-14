package uz.akmal.aistudy10;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @SuppressWarnings("unchecked")
    private static final Class<? extends Activity>[] PROFILES = new Class[]{
        Profile01Activity.class, Profile02Activity.class, Profile03Activity.class,
        Profile04Activity.class, Profile05Activity.class, Profile06Activity.class,
        Profile07Activity.class, Profile08Activity.class, Profile09Activity.class,
        Profile10Activity.class
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.NAVY);
        getWindow().setNavigationBarColor(Ui.NAVY);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.NAVY);
        LinearLayout page=Ui.column(this); Ui.pad(page,this,18,26,18,24);

        TextView brand=Ui.label(this,"AiStudy10",31,Ui.WHITE,true);
        page.addView(brand);
        page.addView(Ui.label(this,"Android · 10 ta mustaqil akkaunt",14,Ui.MUTED,false),Ui.lp(this,-1,-2,4));
        TextView desc=Ui.label(this,"omp.aistudy.uz sayti ilova ichida ochiladi. Har bir profil o‘z login sessiyasini saqlaydi.",14,Ui.WHITE,false);
        page.addView(desc,Ui.lp(this,-1,-2,18));

        android.widget.Button guide=Ui.button(this,"❔  Foydalanish yo‘riqnomasi",Ui.BLUE);
        page.addView(guide,Ui.lp(this,-1,52,18));
        guide.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("AiStudy10 · Yo‘riqnoma")
            .setMessage("1. Kerakli profilni tanlang.\n2. omp.aistudy.uz saytiga login-parolingizni kiriting.\n3. Darsga o‘ting. Video tezligini tanlang.\n4. Avto ▶ va Keyingi dars funksiyalari sayt ruxsat bersa ishlaydi.\n5. Har bir profil uchun login cookie alohida saqlanadi.\n6. Test va modul funksiyalari sinov rejimida.\n\nAndroid’da bir vaqtning o‘zida 10 ta videoning fonda ijrosi kafolatlanmaydi.\n\nMuallif: Akmal Muxamedjanov")
            .setPositiveButton("Tushunarli",(d,w)->{}).show());
        page.addView(Ui.label(this,"PROFILLAR",13,Ui.MUTED,true),Ui.lp(this,-1,-2,25));
        for(int i=0;i<10;i++) {
            final int n=i;
            android.widget.Button b=Ui.button(this,String.format(java.util.Locale.US,"%02d    Foydalanuvchi %d",i+1,i+1),Ui.PANEL);
            page.addView(b,Ui.lp(this,-1,54,9));
            b.setOnClickListener(v -> {
                Intent intent = new Intent(this, PROFILES[n]);
                startActivity(intent);
            });
        }
        page.addView(Ui.label(this,"Muallif: Akmal Muxamedjanov",13,Ui.MUTED,false),Ui.lp(this,-1,-2,23));
        page.addView(Ui.label(this,"Mustaqil yordamchi ilova · AIStudy saytining rasmiy ilovasi emas",12,Ui.MUTED,false),Ui.lp(this,-1,-2,6));
        scroll.addView(page);setContentView(scroll);
    }
}
