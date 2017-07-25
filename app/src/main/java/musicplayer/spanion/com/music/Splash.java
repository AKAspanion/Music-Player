package musicplayer.spanion.com.music;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.WindowManager;

/**
 * Created by Ankit Kumar on 15-07-2017.
 */

public class Splash extends Activity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.splash);

        //creating a thread which allows to sleep for 3secs
        Thread timer = new Thread(){
            @Override
            public void run() {
                //super.run();
                try{
                    sleep(3000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    //launch the ViewSongs class after 3 secs
                    Intent launchSongList = new Intent("musicplayer.spanion.com.music.VIEWSONGS");
                    launchSongList.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(launchSongList);
                }
            }
        };
        timer.start();
    }
}
