package com.example.emotiary_multimedia;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.emotiary_multimedia.DB.DBHandler;
import com.example.emotiary_multimedia.FER.FdActivity;

import java.io.InputStream;

public class DiaryInsertActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "DiaryInsertActivity";

    // Views
    Button saveButton;
    TextView dateTextview, titleTextview;
    TextView recogTextview;
    ImageButton recogImageButton;
    EditText contextEdittext;

    // Data
    private DBHandler handler = null;
    private Cursor cursor = null;
    private String date = null;
    private boolean isWritten = false;
    private int emotion_idx = 0;
    private String emotion = null;
    private String[] emotion_label = new String[]{"angry", "disgust", "fear", "happy", "sad", "surprise", "neutral"};
    private final int ACTIVITY_REQUEST_CODE = 1000;
    private final int ACTIVITY_REQUEST_CODE_ALBUM = 2000;
    private final int ACTIVITY_REQUEST_CODE_ALBUM_DETECT = 3000;
    private Bitmap bmp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_diary_insert);

        this.setViews();
        this.setData();


        if (handler == null){
            handler = DBHandler.open(DiaryInsertActivity.this, "memo_db.db");
        }
        if(isWritten){
            this.getDB();
        }else{
            this.getdetection(null, false, ACTIVITY_REQUEST_CODE);
        }


        saveButton.setOnClickListener(this);
        recogImageButton.setOnClickListener(this);


    }

    private void setViews() {
        saveButton = findViewById(R.id.diary_insert_save_button);
        dateTextview = findViewById(R.id.diary_insert_date_textview);
        titleTextview = findViewById(R.id.diary_insert_title_textview);
        recogTextview = findViewById(R.id.diary_insert_image_recog_textview);
        recogImageButton = findViewById(R.id.diary_insert_image_recog_imagebutton);
        contextEdittext = findViewById(R.id.diary_insert_context_edittext);
    }

    private void setData() {
        Intent intent = getIntent();
        date = intent.getStringExtra("Date");
        isWritten = intent.getBooleanExtra("isWritten",false);
        this.titleTextview.setText(date);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.diary_insert_save_button:
                Log.d("DiaryInsertActivity", "save");
                if(contextEdittext.getText()!=null){

                    String str = contextEdittext.getText().toString();
                    Log.d("DiaryInsertActivity", "str="+str);

                    if(isWritten){
                        handler.update(date,str,emotion_label[emotion_idx]);
                    }else{
                        handler.insert(date,str,emotion_label[emotion_idx]);
                    }

                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK,resultIntent);
                    finish();
                }
                break;
            case R.id.diary_insert_image_recog_imagebutton:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_ALBUM);
                break;
            default:
                Log.e(TAG, "onclick switch default called!");
        }
    }
    private void getdetection(Uri uri, boolean album, int code){
        Log.d("DirayinsertActivity","getdetection()");
        Intent intent = new Intent(DiaryInsertActivity.this, FdActivity.class);
        intent.putExtra("ImageUri", uri);
        intent.putExtra("album", album);
        startActivityForResult(intent, code);
    }
    private void getDB(){
        Log.d("DirayinsertActivity","getDB");

        cursor = handler.select(date);
        cursor.moveToNext();
        contextEdittext.setText(cursor.getString(1));   //col 0 = date, 1 = memo, 2 = emotion
        emotion = cursor.getString(2);
        Log.d("DirayinsertActivity","emotion value = " + emotion);

    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK)
        {
            if(requestCode == ACTIVITY_REQUEST_CODE || requestCode == ACTIVITY_REQUEST_CODE_ALBUM_DETECT){
                emotion_idx = data.getIntExtra("res", 0);    //emotion_idx 값을 받음 => emtoin_label의 index값
                Log.d("DirayinsertActivity", "emotion res_cam = " + emotion_label[emotion_idx]);

            }else if(requestCode == ACTIVITY_REQUEST_CODE_ALBUM){
                try{
                    Uri uri = (Uri) data.getData();
                    getdetection(uri,true,ACTIVITY_REQUEST_CODE_ALBUM_DETECT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
    }
}