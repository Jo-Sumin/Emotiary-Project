package com.example.emotiary_multimedia;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.emotiary_multimedia.DB.DBHandler;

public class DiaryActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "DiaryActivity";

    // Views
    Button modifyButton, deleteButton;
    TextView dateTextView, phraseTextView, contentTextView;
    ImageView imageView;

    // Data
    private Pair<String, String> parsedDate;
    private boolean isWritten = false;
    private DBHandler handler= null;
    private Cursor cursor= null;

    Drawable drawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);

        this.setViews();
        this.setDateString();
        getDB();
        setInitialData();

        modifyButton.setOnClickListener(this);
        deleteButton.setOnClickListener(this);
    }

    private void setViews() {
        modifyButton = findViewById(R.id.diary_modify_button);
        deleteButton = findViewById(R.id.diary_delete_button);

        dateTextView = findViewById(R.id.diary_date_textview);
        imageView = findViewById(R.id.diary_phrase_imageview);
        phraseTextView = findViewById(R.id.diary_phrase_textview);
        contentTextView = findViewById(R.id.diary_content_textview);
    }

    private void setInitialData() {
        isWritten = this.phraseTextView.getText() != "";
        Log.d("DiaryActivity","iswritten= "+isWritten);
        if (isWritten) {
            deleteButton.setVisibility(View.VISIBLE);
            modifyButton.setText("수정");
        } else {
            deleteButton.setVisibility(View.GONE);
            modifyButton.setText("기록하기");
            drawable = getResources().getDrawable(
                    R.drawable.gray);
            imageView.setImageDrawable(drawable);
            contentTextView.setText("아직 감정이 없어요!\n감정을 기록해주세요.\uD83D\uDE2E");
        }
    }

    private static final int DIARY_INSERT_CODE = 100;

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.diary_modify_button:
                Intent intent = new Intent(getApplicationContext(), DiaryInsertActivity.class);
                intent.putExtra("Date", parsedDate.first);
                intent.putExtra("isWritten", isWritten);
                startActivityForResult(intent, DIARY_INSERT_CODE);
                break;
            case R.id.diary_delete_button:
                handler.delete(parsedDate.first);
                finish();
            default:
                Log.e(TAG, "onclick switch default called!");
        }
    }


    // Parsing data string from clicked calendar date into write format
    private void setDateString() {
        Intent intent = getIntent();

        String str = intent.getStringExtra("Date");
        str = str.substring(str.lastIndexOf("{") + 1);
        str = str.substring(0, str.length() - 1);

        String[] date = str.split("-");
        int month = Integer.parseInt(date[1]) + 1;

        String dateString = date[0] + "-" + month + "-" + date[2];
        String koreanDateString = date[0] + "년 " + month + "월 " + date[2] + "일";

        this.parsedDate = new Pair<>(dateString, koreanDateString);
        dateTextView.setText(parsedDate.second);
    }

    //DB에 데이터가 들어있는지 확인후 불러오기
    private void getDB(){
        if (handler == null) {
            handler = DBHandler.open(DiaryActivity.this, "memo_db.db");
        }
        cursor = handler.select(parsedDate.first);
        if(cursor.moveToNext()){
            contentTextView.setText(cursor.getString(1));   //col 0 = date, 1 = memo, 2 = emotion
            String emotion = cursor.getString(2);
            String emotionPhrase = "";

            if(emotion.equals("angry")) {
                drawable = getResources().getDrawable(
                        R.drawable.red);
                emotionPhrase = "오늘은 화나요!\uD83D\uDE21";
            }
            else if(emotion.equals("happy") || emotion.equals("surprise")) {
                drawable = getResources().getDrawable(
                        R.drawable.yellow);
                emotionPhrase = "오늘은 정말 행복해요!!\uD83D\uDE03";
            }
            else if(emotion.equals("neutral")) {
                drawable = getResources().getDrawable(
                        R.drawable.green);
                emotionPhrase = "오늘은 그럭저럭이에요.\uD83D\uDE36";
            }
            else if(emotion.equals("sad")) {
                drawable = getResources().getDrawable(
                        R.drawable.blue);
                emotionPhrase = "오늘은 너무 슬퍼요.\uD83D\uDE25";
            }
            else if (emotion.equals("disgust") || emotion.equals("fear")) {
                drawable = getResources().getDrawable(
                        R.drawable.purple);
                emotionPhrase = "오늘은 정말 최악이에요!\uD83E\uDD22";
            }
            imageView.setImageDrawable(drawable);
            phraseTextView.setText(emotionPhrase);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){

            getDB();
            setInitialData();

        }
    }
}