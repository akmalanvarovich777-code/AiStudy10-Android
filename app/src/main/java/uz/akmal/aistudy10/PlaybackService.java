package uz.akmal.aistudy10;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class PlaybackService extends Service {
  private static final String CHANNEL="aistudy_playback";
  @Override public void onCreate(){
    super.onCreate();
    NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"AiStudy10 ijro",NotificationManager.IMPORTANCE_LOW));
    PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    Notification n=new Notification.Builder(this,CHANNEL).setContentTitle("AiStudy10")
        .setContentText("Dars sessiyalari faol. Ilovani ochish uchun bosing")
        .setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(open).setOngoing(true).build();
    if(Build.VERSION.SDK_INT>=29)startForeground(44,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    else startForeground(44,n);
  }
  @Override public int onStartCommand(Intent intent,int flags,int startId){return START_STICKY;}
  @Override public IBinder onBind(Intent i){return null;}
}
