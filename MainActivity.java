package uz.aistudy.companion;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AIStudy Companion - the safe read-only Android adaptation of a Windows tool.
 * Only fetches course state; NEVER forges viewing progress or submits exams.
 * Credentials are held only in process memory and not persisted or logged.
 */
public class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(15, 23, 42);
    private static final int TEAL = Color.rgb(15, 118, 110);
    private static final int LIGHT = Color.rgb(248, 250, 252);
    private static final int BORDER = Color.rgb(226, 232, 240);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final String BASE = "https://api.aistudy.uz";

    private final List<CourseTask> tasks = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private volatile boolean stopped = false;

    private EditText tokenField, labelField, coursesField;
    private TextView statsView, progressView, detailView, logView;
    private LinearLayout taskRows;
    private Button runButton, stopButton;

    static class CourseTask {
        final String name;
        final String token;
        final List<String> courses;
        String state = "Kutilmoqda";

        CourseTask(String name, String token, List<String> courses) {
            this.name = name;
            this.token = token;
            this.courses = courses;
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(LIGHT);
        buildUi();
        appendLog("Dastur tayyor. Bu versiya faqat kurs holatini o'qiydi.");
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(LIGHT);
        LinearLayout main = column();
        scroll.addView(main);
        setContentView(scroll);

        LinearLayout banner = column();
        banner.setPadding(dp(20), dp(29), dp(20), dp(23));
        banner.setBackgroundColor(NAVY);
        TextView brand = text("AI STUDY  •  ANDROID", 12, 0xFF8CE3D7, true);
        banner.addView(brand);
        TextView headline = text("Kurslar nazorati", 27, Color.WHITE, true);
        addTop(headline, 8);
        banner.addView(headline);
        TextView caption = text("Kurs holatini xavfsiz tekshirish uchun yordamchi ilova", 14, 0xFFCBD5E1, false);
        addTop(caption, 7);
        banner.addView(caption);
        main.addView(banner);

        LinearLayout content = column();
        content.setPadding(dp(16), dp(17), dp(16), dp(24));
        main.addView(content);

        LinearLayout security = column();
        security.setPadding(dp(14), dp(12), dp(14), dp(12));
        security.setBackground(shape(0xFFE9F7F4, 14, 0xFFB8E3D9));
        security.addView(text("🔒  Token faqat vaqtincha xotirada saqlanadi", 13, TEAL, true));
        TextView securitySmall = text("HTTPS tekshiruvi yoqilgan. Token chatga yoki boshqa serverlarga yuborilmaydi.", 12, 0xFF355E58, false);
        addTop(securitySmall, 4);
        security.addView(securitySmall);
        content.addView(security);

        LinearLayout inputCard = card();
        addTop(inputCard, 16);
        content.addView(inputCard);
        inputCard.addView(text("Akkaunt qo'shish", 19, NAVY, true));
        inputCard.addView(spacer(12));
        inputCard.addView(text("Shaxsiy AIStudy tokeningiz", 13, NAVY, true));
        tokenField = field("JWT tokenni kiriting");
        tokenField.setSingleLine(true);
        tokenField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tokenField.setTypeface(Typeface.DEFAULT);
        tokenField.setSaveEnabled(false);
        tokenField.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        addTop(tokenField, 5);
        inputCard.addView(tokenField);
        Button eye = secondaryButton("Tokenni ko'rsatish / yashirish");
        addTop(eye, 7);
        eye.setOnClickListener(v -> {
            boolean hidden = (tokenField.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0;
            tokenField.setInputType(InputType.TYPE_CLASS_TEXT |
                    (hidden ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            tokenField.setSelection(tokenField.length());
        });
        inputCard.addView(eye);

        inputCard.addView(spacer(10));
        inputCard.addView(text("Ism (ixtiyoriy)", 13, NAVY, true));
        labelField = field("Masalan, mening akkauntim");
        addTop(labelField, 5);
        inputCard.addView(labelField);
        inputCard.addView(spacer(10));
        inputCard.addView(text("Kurs raqamlari (vergul bilan)", 13, NAVY, true));
        coursesField = field("1,2,3,4,5");
        coursesField.setInputType(InputType.TYPE_CLASS_TEXT);
        coursesField.setText("1,2,3,4,5");
        addTop(coursesField, 5);
        inputCard.addView(coursesField);

        Button add = primaryButton("+  Ro'yxatga qo'shish");
        addTop(add, 13);
        add.setOnClickListener(v -> addTask());
        inputCard.addView(add);

        LinearLayout jobsCard = card();
        addTop(jobsCard, 14);
        content.addView(jobsCard);
        statsView = text("Vazifalar (0)", 19, NAVY, true);
        jobsCard.addView(statsView);
        taskRows = column();
        addTop(taskRows, 8);
        jobsCard.addView(taskRows);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        addTop(controls, 14);
        content.addView(controls);
        runButton = primaryButton("Tekshirish");
        LinearLayout.LayoutParams wide = new LinearLayout.LayoutParams(0, dp(49), 1f);
        wide.setMargins(0, 0, dp(7), 0);
        controls.addView(runButton, wide);
        runButton.setOnClickListener(v -> startChecking());
        stopButton = secondaryButton("To'xtatish");
        stopButton.setEnabled(false);
        LinearLayout.LayoutParams narrow = new LinearLayout.LayoutParams(0, dp(49), 1f);
        narrow.setMargins(dp(7), 0, 0, 0);
        controls.addView(stopButton, narrow);
        stopButton.setOnClickListener(v -> { stopped = true; appendLog("To'xtatish so'raldi…"); });

        LinearLayout output = card();
        addTop(output, 14);
        content.addView(output);
        output.addView(text("Natijalar", 19, NAVY, true));
        progressView = text("Hali tekshirilmadi", 14, MUTED, false);
        addTop(progressView, 9);
        output.addView(progressView);
        detailView = text("Darslar holati shu yerda ko'rsatiladi.", 13, NAVY, false);
        addTop(detailView, 8);
        output.addView(detailView);
        output.addView(spacer(12));
        output.addView(text("Jarayon jurnali", 15, NAVY, true));
        logView = text("", 12, MUTED, false);
        addTop(logView, 8);
        logView.setPadding(dp(10), dp(10), dp(10), dp(10));
        logView.setBackground(shape(0xFFF1F5F9, 9, BORDER));
        output.addView(logView);

        Button website = secondaryButton("AIStudy.uz saytini ochish ↗");
        addTop(website, 14);
        website.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudy.uz/"))));
        content.addView(website);
        renderTasks();
    }

    private void addTask() {
        if (isRunning) { toast("Tekshiruv tugaguncha kuting"); return; }
        String token = tokenField.getText().toString().trim();
        String csv = coursesField.getText().toString().trim();
        if (token.isEmpty()) { toast("Tokenni kiriting"); return; }
        if (token.length() > 12000 || !token.matches("[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+")) {
            toast("Uch qismli JWT token kiritilishi kerak"); return;
        }
        if (tasks.size() >= 5) { toast("Bir vaqtda 5 tagacha akkaunt"); return; }
        List<String> selected = new ArrayList<>();
        for (String item : csv.split(",")) {
            String id = item.trim();
            if (id.isEmpty()) continue;
            if (!id.matches("[0-9]{1,10}")) { toast("Kurs raqami faqat son bo'lsin"); return; }
            if (!selected.contains(id)) selected.add(id);
        }
        if (selected.isEmpty() || selected.size() > 30) {
            toast("1 dan 30 tagacha kurs raqamini kiriting"); return;
        }
        JSONObject claims = jwtClaims(token);
        String name = labelField.getText().toString().trim();
        if (name.isEmpty()) name = claims.optString("FullName", "Akkaunt " + (tasks.size()+1));
        if (name.isEmpty()) name = "Akkaunt " + (tasks.size()+1);
        tasks.add(new CourseTask(name, token, selected));
        tokenField.setText("");  // Never keep a second visible copy in an input field.
        labelField.setText("");
        renderTasks();
        appendLog(name + ": " + selected.size() + " ta kurs qo'shildi");
    }

    private void renderTasks() {
        taskRows.removeAllViews();
        statsView.setText("Vazifalar (" + tasks.size() + ")");
        if (tasks.isEmpty()) {
            taskRows.addView(text("Hozircha akkaunt qo'shilmagan.", 13, MUTED, false));
            return;
        }
        for (int i=0; i<tasks.size(); i++) {
            final int position=i;
            CourseTask task=tasks.get(i);
            LinearLayout item = column();
            item.setPadding(dp(11), dp(11), dp(11), dp(11));
            item.setBackground(shape(0xFFF8FAFC, 11, BORDER));
            addTop(item, 7);
            item.addView(text(task.name, 15, NAVY, true));
            TextView courses=text("Kurslar: " + TextUtils.join(", ", task.courses), 12, MUTED, false);
            addTop(courses, 4);
            item.addView(courses);
            TextView state=text("Holat: " + task.state, 12, TEAL, true);
            addTop(state, 5);
            item.addView(state);
            Button remove=secondaryButton("O'chirish");
            addTop(remove, 6);
            remove.setEnabled(!isRunning);
            remove.setOnClickListener(v -> {
                if (isRunning) return;
                tasks.remove(position);
                renderTasks();
            });
            item.addView(remove);
            taskRows.addView(item);
        }
    }

    private void startChecking() {
        if (tasks.isEmpty()) { toast("Avval akkaunt qo'shing"); return; }
        if (isRunning) return;
        isRunning=true; stopped=false;
        runButton.setEnabled(false); stopButton.setEnabled(true);
        detailView.setText("");
        progressView.setText("Tekshirilmoqda…");
        appendLog("Kurs holatini tekshirish boshlandi");
        renderTasks();
        final List<CourseTask> snapshot = new ArrayList<>(tasks);
        worker.execute(() -> {
            int success=0, failures=0;
            for (CourseTask task : snapshot) {
                if (stopped) break;
                task.state="Tekshirilmoqda";
                ui.post(this::renderTasks);
                for (String courseId : task.courses) {
                    if (stopped) break;
                    final String prefix = task.name + " / Kurs " + courseId;
                    try {
                        final JSONObject root = getCourseTree(task.token, courseId);
                        final JSONObject result = root.optJSONObject("result");
                        if (result == null) {
                            throw new Exception("Server natija qaytarmadi yoki kursga ruxsat yo'q");
                        }
                        final String report = renderCourse(courseId, result);
                        success++;
                        ui.post(() -> {
                            appendLog(prefix + ": ma'lumot olindi");
                            detailView.append(report + "\n\n");
                        });
                    } catch (Exception e) {
                        failures++;
                        final String safeError = e.getMessage() == null ? "noma'lum xatolik" : e.getMessage();
                        ui.post(() -> appendLog(prefix + ": " + safeError));
                    }
                }
                task.state = stopped ? "To'xtatildi" : "Tekshirildi";
                ui.post(this::renderTasks);
            }
            final int ok=success, error=failures;
            ui.post(() -> {
                isRunning=false;
                stopButton.setEnabled(false);
                runButton.setEnabled(true);
                progressView.setText((stopped ? "To'xtatildi. " : "Yakunlandi. ")+
                        "Muvaffaqiyatli: " + ok + "  •  Xatolik: " + error);
                appendLog(stopped ? "Tekshiruv to'xtatildi" : "Barcha tekshiruvlar tugadi");
                renderTasks();
            });
        });
    }

    private static JSONObject getCourseTree(String token, String courseId) throws Exception {
        String u = BASE + "/api/StudyAILms/Progress/GetPrivateCourseContentTree?courseId=" +
                URLEncoder.encode(courseId, "UTF-8");
        URL url = new URL(u);
        if (!"https".equalsIgnoreCase(url.getProtocol()) || !"api.aistudy.uz".equalsIgnoreCase(url.getHost())) {
            throw new Exception("Ruxsat etilmagan server manzili");
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("accept", "application/json");
        conn.setRequestProperty("authorization", "Bearer " + token);
        conn.setRequestProperty("Origin", "https://omp.aistudy.uz");
        conn.setRequestProperty("Referer", "https://omp.aistudy.uz/");
        conn.setRequestProperty("User-Agent", "AIStudy-Companion-Android/1.0");
        String region = jwtClaims(token).optString("RegionId", "15");
        conn.setRequestProperty("TenantId", region);
        try {
            int status = conn.getResponseCode();
            if (status == 401) throw new Exception("Token eskirgan yoki noto'g'ri (401)");
            if (status == 403) throw new Exception("Kursga kirishga ruxsat yo'q (403)");
            if (status == 429) throw new Exception("Server so'rovlarni chekladi (429)");
            if (status < 200 || status >= 300) throw new Exception("Server xatosi: HTTP " + status);
            String body=readLimited(conn.getInputStream(), 2_000_000);
            return new JSONObject(body);
        } finally {
            conn.disconnect();
        }
    }

    private static String readLimited(InputStream stream, int maxChars) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] chunk = new char[4096];
            int count;
            while ((count=reader.read(chunk)) != -1) {
                if (sb.length() + count > maxChars) throw new Exception("Server javobi juda katta");
                sb.append(chunk,0,count);
            }
            return sb.toString();
        }
    }

    private static JSONObject jwtClaims(String jwt) {
        try {
            String[] segments=jwt.split("\\.");
            if (segments.length != 3) return new JSONObject();
            byte[] raw=Base64.decode(segments[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return new JSONObject(new String(raw, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static String renderCourse(String id, JSONObject result) {
        int count = result.optInt("lessonCount", -1);
        int finished = result.optInt("finishLessonCount", -1);
        StringBuilder sb = new StringBuilder("📘 Kurs " + id + "\n");
        if (count >= 0 && finished >= 0) {
            int percent = count > 0 ? Math.round(100f * finished / count) : 0;
            sb.append("Jarayon: ").append(finished).append("/").append(count)
                    .append(" (" ).append(percent).append("%)\n");
        }
        JSONArray modules=result.optJSONArray("children");
        if (modules == null) return sb.append("Darslar ro'yxati mavjud emas.").toString();
        int displayed=0, all=0;
        for (int i=0;i<modules.length();i++) {
            JSONObject module=modules.optJSONObject(i);
            if (module==null) continue;
            String moduleName = localTitle(module, "Modul " + (i+1));
            JSONArray lessons=module.optJSONArray("children");
            if (lessons == null) continue;
            sb.append("\n").append(moduleName).append("\n");
            for (int j=0;j<lessons.length();j++) {
                JSONObject lesson=lessons.optJSONObject(j);
                if (lesson==null) continue;
                all++;
                if (displayed++ >= 80) continue;
                boolean done=lesson.optInt("status", -1)==2;
                String type = lesson.optInt("type", -1)==3 ? "Test" : "Dars";
                sb.append(done ? "✓ " : "○ ")
                  .append(type).append(": ")
                  .append(localTitle(lesson, "#" + lesson.optString("id", "?"))).append("\n");
            }
        }
        if (all > 80) sb.append("… yana ").append(all-80).append(" dars (ekranda qisqartirildi)");
        return sb.toString();
    }

    private static String localTitle(JSONObject object, String fallback) {
        Object title=object.opt("title");
        if (title instanceof JSONObject) {
            JSONObject localized=(JSONObject)title;
            String val=localized.optString("uz", localized.optString("ru", localized.optString("en",fallback)));
            return val.isEmpty()? fallback : val;
        }
        if (title instanceof String && !((String)title).isEmpty()) return (String)title;
        return fallback;
    }

    private void appendLog(String message) {
        String date=new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String combined=logView.getText().toString() + "[" + date + "] " + message + "\n";
        if (combined.length() > 6000) combined=combined.substring(combined.length()-6000);
        logView.setText(combined);
    }

    private LinearLayout column() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    private LinearLayout card() {
        LinearLayout l=column();
        l.setPadding(dp(16), dp(17), dp(16), dp(17));
        l.setBackground(shape(Color.WHITE, 17, BORDER));
        return l;
    }

    private EditText field(String hint) {
        EditText f=new EditText(this);
        f.setHint(hint);
        f.setTextColor(NAVY);
        f.setHintTextColor(0xFF94A3B8);
        f.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        f.setSingleLine(true);
        f.setPadding(dp(12), dp(10), dp(12), dp(10));
        f.setBackground(shape(Color.WHITE, 9, BORDER));
        return f;
    }

    private TextView text(String txt, int sp, int color, boolean bold) {
        TextView t=new TextView(this);
        t.setText(txt);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        t.setLineSpacing(dp(2),1f);
        return t;
    }

    private Button primaryButton(String text) { return button(text, TEAL, Color.WHITE); }
    private Button secondaryButton(String text) { return button(text, 0xFFF1F5F9, NAVY); }

    private Button button(String label, int bg, int fg) {
        Button b=new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTextColor(fg);
        b.setBackground(shape(bg, 10, bg));
        b.setPadding(dp(9), dp(8), dp(9), dp(8));
        return b;
    }

    private GradientDrawable shape(int color, int radiusDp, int stroke) {
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radiusDp));
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private TextView spacer(int dp) {
        TextView v=new TextView(this);
        v.setHeight(dp(dp));
        return v;
    }
    private static void addTop(View view, int dp) {
        // This layout margin method uses raw DP only when called through the View's context.
        float density=view.getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=Math.round(dp*density);
        view.setLayoutParams(p);
    }
    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
    private void toast(String msg) { Toast.makeText(this,msg,Toast.LENGTH_LONG).show(); }
    @Override public void onDestroy() {
        stopped=true;
        worker.shutdownNow();
        for (CourseTask task:tasks) { /* discard in-memory credentials on process exit */ }
        tasks.clear();
        super.onDestroy();
    }
}
