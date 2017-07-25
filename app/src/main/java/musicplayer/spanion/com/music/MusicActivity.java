package musicplayer.spanion.com.music;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;


public class MusicActivity extends Activity {

    static MediaPlayer mPlayer;
    static seekUpdater su;
    ImageView albumArt,circle;
    byte[] art;
    TextView artist;
    String artistName;
    MediaMetadataRetriever mmr;
    ArrayList<String> mySongs, mySongsName;
    TextView name;
    ImageButton next, play, playlist, prev, info, repeat;
    String path,received;
    int position;
    boolean repeatSelected = false, isPaused=false;
    SeekBar sb;
    Bitmap songArt;
    String songName;
    Uri u;
    Animation animationCircle;
    PhoneStateListener phoneStateListener;
    TelephonyManager mgr;
    AudioManager am;
    AudioManager.OnAudioFocusChangeListener afChangeListener;
    static int animateCount=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        //initialize variables
        albumArt =  findViewById(R.id.albumArt);
        name =  findViewById(R.id.songName);
        name.setSelected(true);
        artist =  findViewById(R.id.artistName);
        play =  findViewById(R.id.playButton);
        prev =  findViewById(R.id.previousButton);
        next =  findViewById(R.id.nextButton);
        playlist =  findViewById(R.id.playlistButton);
        info = findViewById(R.id.info);
        sb =  findViewById(R.id.seekBar);
        repeat =  findViewById(R.id.toggleRepeat);
        mySongs = new ArrayList<>();
        mySongsName = new ArrayList<>();
        circle = findViewById(R.id.circle);
        animationCircle = AnimationUtils.loadAnimation(this, R.anim.circle);

        //audioManager to create audio listener
        am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        //retrieving data sent through intent putExtra
        Intent intent = getIntent();
        received = intent.getStringExtra("currentSong");
        mySongs = intent.getStringArrayListExtra("songPath");
        mySongsName = intent.getStringArrayListExtra("songName");

        //get the song position in arrayList using the name got from intent
        calculatePos(received);

        //animate all Views in position
        animateViews();

        //a listener to find if any external source wants to use audio stream
        afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                if (focusChange <=0) {
                    //external source wants to use audio stream so pause
                    mPlayer.pause();
                    circle.clearAnimation();
                    play.setImageResource(R.drawable.ic_play);
                }else {
                    //audio stream is free for use so resume again if song not paused
                    if(!isPaused){
                        mPlayer.start();
                        circle.startAnimation(animationCircle);
                        play.setImageResource(R.drawable.ic_pause);
                    }
                }
            }
        };

        //once listener is created start song
        startSong(position);

        //for track change on swipe
        albumArt.setOnTouchListener(new View.OnTouchListener() {
            private float x1,x2;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        return true;
                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();
                        if (x1 > x2) {
                            nextSong();
                        } else if (x2 > x1) {
                            prevSong();
                        }
                        return true;
                }
                return false;
            }
        });

        //go back to song list
        playlist.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                am.abandonAudioFocus(afChangeListener);
                Intent launchSongList = new Intent(MusicActivity.this, ViewSongs.class);
                startActivity(launchSongList);
                overridePendingTransition( R.anim.activity_up_2, R.anim.activity_down_2 );
            }
        });

        //sets repeat of one song on/off
        repeat.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(repeatSelected){
                    repeatSelected=false;
                    repeat.setImageResource(R.drawable.ic_repeat_unselected);
                    Toast.makeText(MusicActivity.this, "Song Reapeat Off!", Toast.LENGTH_SHORT).show();
                }
                else{
                    repeatSelected=true;
                    repeat.setImageResource(R.drawable.ic_repeat_selected);
                    Toast.makeText(MusicActivity.this, "Song Reapeat On!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //to get info of song
        info.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getInfo(mySongs.get(position));
            }
        });

        //play/pause on tap
        play.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPlayer!=null) {
                    if (mPlayer.isPlaying()) {
                        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
                        play.startAnimation(animation);
                        play.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                circle.clearAnimation();
                                play.setImageResource(R.drawable.ic_play);
                                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                                play.startAnimation(animation);
                            }
                        }, 100);
                        isPaused=true;
                        mPlayer.pause();
                    } else {
                        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
                        play.startAnimation(animation);
                        play.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                circle.startAnimation(animationCircle);
                                play.setImageResource(R.drawable.ic_pause);
                                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                                play.startAnimation(animation);
                            }
                        }, 100);
                        isPaused=false;
                        mPlayer.start();
                    }
                }
                else {
                    startSong(0);
                }
            }
        });

        //next track on tap
        next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.next_forward);
                next.startAnimation(animation);
                next.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.next_back);
                        next.startAnimation(animation);
                    }
                }, 100);
                nextSong();
            }
        });

        //previous track on tap
        prev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.prev_forward);
                prev.startAnimation(animation);
                prev.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.prev_back);
                        prev.startAnimation(animation);
                    }
                }, 100);
                prevSong();
            }
        });

    }

    //a function to calculate position of song in the received arrayList using the song name
    private void calculatePos(String received) {
        int flag=0;
        for (int i = 0; i < mySongsName.size(); i++){
            String temp = mySongsName.get(i);
            if (temp.equals(received)){
                position= i;
                flag=1;
                break;
            }
        }
        if(flag==0){
            position=0;
        }
    }

    //animates views on launch
    private void animateViews() {
        if(animateCount==0){
            animateCount++;
            Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_off_left);
            next.startAnimation(animation1);
            Animation animation2 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_off_right);
            prev.startAnimation(animation2);
            Animation animation3 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
            sb.startAnimation(animation3);
            Animation animation4 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.albumart_down);
            albumArt.startAnimation(animation4);

            albumArt.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.albumart_up);
                    albumArt.startAnimation(animation);
                }
            }, 400);
            next.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_intro_left);
                    next.startAnimation(animation);
                }
            }, 800);

            prev.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_intro_right);
                    prev.startAnimation(animation);
                }
            }, 800);

            sb.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up_sb);
                    sb.startAnimation(animation);
                }
            }, 1800);
        }
        else {
            Animation animation4 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.albumart_down);
            albumArt.startAnimation(animation4);
            albumArt.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.albumart_up);
                    albumArt.startAnimation(animation);
                }
            }, 400);
        }

    }

    //a thread inner class for updating seek
    class seekUpdater extends Thread {
        private boolean running;

        seekUpdater(boolean status) {
            this.running = status;
        }

        public void endSeekThread() {
            this.running = false;
        }

        public void run() {
            int tot = mPlayer.getDuration();
            int pos = 0;
            while(running==true){
                try {
                    sleep(500);
                    pos=mPlayer.getCurrentPosition();
                    if(pos<tot){
                        sb.setProgress(pos);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    //starts the song
    public void startSong(final int position) {

        int result = am.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (mPlayer != null) {
                if (su != null) {
                    su.endSeekThread();
                    su.interrupt();
                    su = null;
                }
                mPlayer.stop();
                mPlayer.release();
            }
            this.path = (String) this.mySongs.get(position);
            this.u = Uri.parse(this.path);
            setSongData(this.path, position);
            mPlayer = new MediaPlayer();
            mPlayer = MediaPlayer.create(getApplicationContext(), this.u);
            if(!isPaused){
                play.setImageResource(R.drawable.ic_pause);
                circle.startAnimation(animationCircle);
                mPlayer.start();
            }
            else {
                play.setImageResource(R.drawable.ic_play);
            }
            this.sb.setMax(mPlayer.getDuration());
            su = new seekUpdater(true);
            su.start();

            sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if(isPaused){
                        isPaused=false;
                        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
                        play.startAnimation(animation);
                        play.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                                play.startAnimation(animation);
                                play.setImageResource(R.drawable.ic_pause);
                                circle.startAnimation(animationCircle);
                                mPlayer.start();
                            }
                        }, 100);
                        play.setImageResource(R.drawable.ic_pause);
                    }
                    mPlayer.seekTo(seekBar.getProgress());
                }
            });

            mPlayer.setOnCompletionListener(new OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    if (MusicActivity.this.repeatSelected) {
                        MusicActivity.this.startSong(position);
                    } else {
                        MusicActivity.this.nextSong();
                    }
                }
            });
        }
    }


    public void nextSong() {
        position = (this.position + 1) % this.mySongs.size();

        albumArt.postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_off);
                albumArt.startAnimation(animation);
            }
        }, 150);
        albumArt.postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in);
                albumArt.startAnimation(animation);
                startSong(position);
            }
        }, 300);
    }

    public void prevSong() {
        position = this.position + -1 < 0 ? this.mySongs.size() - 1 : this.position - 1;
        albumArt.postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_off_reverse);
                albumArt.startAnimation(animation);
            }
        }, 150);
        albumArt.postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_reverse);
                albumArt.startAnimation(animation);
                startSong(position);
            }
        }, 300);
    }

    public void setSongData(String songPath, int pos) {
        this.mmr = new MediaMetadataRetriever();
        this.mmr.setDataSource(songPath);
        try {
            this.songName = this.mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            this.art = this.mmr.getEmbeddedPicture();
            this.songArt = BitmapFactory.decodeByteArray(this.art, 0, this.art.length);
            this.artistName = this.mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            this.artist.setText(this.artistName);
            this.name.setText(this.songName);
            this.albumArt.setImageBitmap(this.songArt);
        } catch (Exception e){
            this.albumArt.setImageResource(R.drawable.ic_albumart);
            this.artist.setText("Unknown Artist");
            this.name.setText(mySongsName.get(pos));
        }
    }


    public void getInfo(String songPath){
        this.mmr = new MediaMetadataRetriever();
        this.mmr.setDataSource(songPath);
        StringBuffer br = new StringBuffer();
        try{
            br.append("LOCATION : " + songPath +"\n\n");
            br.append("TITLE : " + mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)+"\n\n");
            br.append("GENRE : " + mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)+"\n\n");
            br.append("ALBUM : " + mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)+"\n\n");
            br.append("ARTIST : " + mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)+"\n\n");
            br.append("BITRATE : " + mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)+"\n\n");
            br.append("DURATION : " + convertToMin(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))+"\n");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlert);
        builder.setTitle("Song Info");
        builder.setMessage(br.toString());
        builder.setNegativeButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.show();
    }

    public String convertToMin(String songDuration){
        long duration = Long.parseLong(songDuration );
        String seconds = String.valueOf((duration  % 60000) / 1000);

        String minutes = String.valueOf(duration / 60000);

        return minutes+":"+seconds+"min";
    }

    public void onBackPressed() {
        circle.clearAnimation();
        mPlayer.pause();
        Builder dialog = new Builder(this, R.style.MyAlert);
        dialog.setPositiveButton( "Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if(isPaused){
                    play.setImageResource(R.drawable.ic_pause);
                    isPaused=false;
                }
                circle.startAnimation(animationCircle);
                MusicActivity.mPlayer.start();
                MusicActivity.this.moveTaskToBack(false);
            }
        });
        dialog.setNegativeButton((CharSequence) "No",  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MusicActivity.mPlayer.stop();
                MusicActivity.mPlayer.release();
                am.abandonAudioFocus(afChangeListener);
                mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                MusicActivity.this.finish();
            }
        });
        dialog.setMessage((CharSequence) "Keep playing in the background after exit?");
        dialog.setTitle((int) R.string.app_name);
        dialog.show();
    }
}
