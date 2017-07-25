package musicplayer.spanion.com.music;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Media;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;


/**
 * Created by Ankit Kumar on 15-07-2017.
 */

public class ViewSongs extends Activity {

    private static final int MY_PERMISSION_REQUEST = 1;
    ArrayAdapter<String> arrayAdapter;
    ArrayList<String> arrayListPath;
    ArrayList<String> arrayListSong;
    ProgressDialog dialog;

    ListView listView;
    SearchView searchView;

    String song="";

    boolean songsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.view_songs);
        listView = findViewById(R.id.list_item);
        searchView = findViewById(R.id.search_song);

        //asking for permission to read storage
        if(ContextCompat.checkSelfPermission(ViewSongs.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            //if permission not given then ask for it
            if(ActivityCompat.shouldShowRequestPermissionRationale(ViewSongs.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(ViewSongs.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            }
            else{
                ActivityCompat.requestPermissions(ViewSongs.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            }
        }
        else {
            //got permission so launch the function and retrieve the songs
            sendSongs();
        }

    }

    public void sendSongs() {
        arrayListSong = new ArrayList<>();
        arrayListPath = new ArrayList<>();

        //a dialog box while the songs load
        dialog = new ProgressDialog(this);
        dialog.setMessage("Please wait, Fetching Songs...");
        dialog.setCancelable(false);
        dialog.show();

        //fetch the songs
        getSongs();

        if (songsLoaded) {
            //load all the song names in the listView dynamically by taking strings from arrayList of Songs
            arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.simple_list_item, arrayListSong);
            listView.setAdapter(this.arrayAdapter);
            dialog.dismiss();
        }

        //a listener to create intent and activate musicActivity whenever a listItem is tapped
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent launchPlayer = new Intent(ViewSongs.this.getApplicationContext(), MusicActivity.class);
                launchPlayer.putExtra("currentSong", listView.getItemAtPosition(i).toString()); //send selected song Name to MusicActivity
                launchPlayer.putExtra("songName", ViewSongs.this.arrayListSong); //send song Name of all songs to MusicActivity
                launchPlayer.putExtra("songPath", ViewSongs.this.arrayListPath); //send song Path of all songs to MusicActivity
                launchPlayer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                ViewSongs.this.startActivity(launchPlayer);
                overridePendingTransition( R.anim.activity_up, R.anim.activity_down );
            }
        });

        //a query listener for searchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                arrayAdapter.getFilter().filter(s);
                return false;
            }
        });
    }

    public void getSongs() {
        //getting all the songs from the database
        Cursor cursor = getContentResolver().query(Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int songName = cursor.getColumnIndex("_display_name");
            int path = cursor.getColumnIndex("_data");
            do {
                //keep on adding the songs to string arrayList
                String currentName = cursor.getString(songName);
                String currentPath = cursor.getString(path);
                if (currentName.contains(".mp3")) {
                    arrayListSong.add(currentName.replace(".mp3", ""));
                    arrayListPath.add(currentPath);
                }
            } while (cursor.moveToNext());
            songsLoaded = true;
        }
        if(!cursor.isClosed()){
            cursor.close();
        }
    }

    //this is to get permission whenever the app is installed first
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
       // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSION_REQUEST:{
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(ViewSongs.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                        sendSongs();
                    }
                }
                else{
                    Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }
    }
}
