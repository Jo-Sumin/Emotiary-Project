package com.example.emotiary_multimedia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;

import com.example.emotiary_multimedia.decorator.SaturdayDecorator;
import com.example.emotiary_multimedia.decorator.SundayDecorator;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

public class MainActivity extends AppCompatActivity implements OnDateSelectedListener {

    private static final String TAG = "MainActivity";
    private MaterialCalendarView materialCalendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        this.setViews();

        materialCalendarView.setOnDateChangedListener(this);
    }

    // Setting views when MainActivity is launched
    private void setViews() {
        this.materialCalendarView = findViewById(R.id.main_material_calendar);
        materialCalendarView.setSelectedDate(CalendarDay.today());

        materialCalendarView.addDecorators(
                new SundayDecorator(),
                new SaturdayDecorator()
        );
    }

    // Called when date is selected from materialCalendarView
    @Override
    public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
        Intent intent = new Intent(this, DiaryActivity.class);

        intent.putExtra("Date", date.toString());
        startActivity(intent);
    }
}