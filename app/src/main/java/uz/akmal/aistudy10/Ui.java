package uz.akmal.aistudy10;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int NAVY = Color.rgb(13,23,48);
    static final int PANEL = Color.rgb(22,38,70);
    static final int BLUE = Color.rgb(37,94,220);
    static final int WHITE = Color.WHITE;
    static final int MUTED = Color.rgb(166,187,224);

    static int dp(Context c, int v) { return (int)(v * c.getResources().getDisplayMetrics().density + 0.5f); }

    static GradientDrawable bg(Context c, int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(c, radius)); return g;
    }
    static TextView label(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c); t.setText(s); t.setTextSize(sp);
        t.setTextColor(color); if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }
    static Button button(Context c, String title, int color) {
        Button b = new Button(c); b.setText(title); b.setAllCaps(false);
        b.setTextColor(WHITE); b.setTextSize(13);
        b.setBackground(bg(c, color, 12)); return b;
    }
    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); return l;
    }
    static void pad(View v, Context c, int a, int b, int d, int e) {
        v.setPadding(dp(c,a), dp(c,b), dp(c,d), dp(c,e));
    }
    static LinearLayout.LayoutParams lp(Context c, int w, int h, int marginTop) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(c,w),h<0?h:dp(c,h));
        p.topMargin=dp(c,marginTop);return p;
    }
    private Ui() {}
}
