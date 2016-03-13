package com.app.robotics.roboticsapp;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.ToggleButton;
import android.widget.CompoundButton;
import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

import java.io.Console;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private HttpRequest request;

    private AlertDialog urlAlert;
    private AlertDialog sensorAlert;

    private int SELECTED_SENSOR_TYPE = 2;
    private int SENSOR_TYPE_SEND_PERIODICALLY = 0;
    private int SENSOR_TYPE_SEND_WITH_BUTTON = 1;
    private int SENSOR_TYPE_NO_SEND = 2;

    private Button requestSensorValueButton;

    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        createConnectionAlert();
        createSensorAlert();

        setupTimer();

        request = new HttpRequest();

        requestSensorValueButton = (Button) findViewById(R.id.sensor_get_value);
        requestSensorValueButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    updateSensorValue (false);
                    return true;
                }
                return false;
            }
        });
        changeSensorUI();

        Joystick joystick = (Joystick) findViewById(R.id.joystick);
        joystick.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {

            }

            @Override
            public void onDrag(float degrees, float offset) {
                SendRequest(degrees, offset);
            }

            @Override
            public void onUp() {
                SendRequest(0, 0);
            }
        });
    }

    private void SendRequest(float degrees, float offset) {
        double x = 0;
        double y = 0;
        degrees -= 90;
        double distance = 10 * offset;
        Double val = Double.parseDouble(String.valueOf(degrees));
        val = Math.toRadians(val);
        x = Math.cos(val)*distance;
        y = Math.sin(val)*distance;
        final double g_x = x;
        final double g_y = y;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                request.SendSeekerRequest(g_x, g_y);
            }
        });
    }

    private void createConnectionAlert () {
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View connectionChoiceView = li.inflate(R.layout.url_prompt, null);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle("Choose connection");
        alertBuilder.setView(connectionChoiceView);
        alertBuilder.setCancelable(true);

        Button urlConfirm = (Button) connectionChoiceView.findViewById(R.id.url_confirm_button);
        final EditText urlText = (EditText) connectionChoiceView.findViewById(R.id.change_url_edit);

        urlText.setText("192.168.1.179:8080");
        urlConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpRequest.Url = urlText.getText().toString();
                urlAlert.hide();

            }
        });

        urlAlert=alertBuilder.create();
    }
    private RadioButton sensorPeriod;
    private RadioButton sensorButton;
    private RadioButton sensorNoSend;

    private void createSensorAlert() {
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View sensorChoiceView = li.inflate(R.layout.sensor_send_type, null);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle("Choose sensor send type");
        alertBuilder.setView(sensorChoiceView);
        alertBuilder.setCancelable(true);

        final Button sensorConfirm = (Button) sensorChoiceView.findViewById(R.id.sensor_confirm_btn);
        sensorPeriod = (RadioButton) sensorChoiceView.findViewById(R.id.sensor_period);
        sensorButton = (RadioButton) sensorChoiceView.findViewById(R.id.sensor_btn_press);
        sensorNoSend = (RadioButton) sensorChoiceView.findViewById(R.id.sensor_no_send);

        sensorConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorPeriod.isChecked()) {
                    setSensorMode (SENSOR_TYPE_SEND_PERIODICALLY);
                } else if (sensorButton.isChecked()) {
                    setSensorMode(SENSOR_TYPE_SEND_WITH_BUTTON);
                } else if (sensorNoSend.isChecked()) {
                    setSensorMode(SENSOR_TYPE_NO_SEND);
                }

                sensorAlert.hide();
            }
        });

        sensorAlert=alertBuilder.create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_choose_connection) {
            System.out.println("open choose connection!!!");
            if (!this.isFinishing()) {
                urlAlert.show();
            }
            return true;
        } else if (id == R.id.action_choose_sensor_mode) {
            System.out.println("Open sensor mode");
            if (!this.isFinishing()) {
                if (SELECTED_SENSOR_TYPE == SENSOR_TYPE_SEND_PERIODICALLY) {
                    sensorPeriod.setChecked(true);
                    sensorButton.setChecked(false);
                    sensorNoSend.setChecked(false);
                } else if (SELECTED_SENSOR_TYPE == SENSOR_TYPE_SEND_WITH_BUTTON) {
                    sensorPeriod.setChecked(false);
                    sensorButton.setChecked(true);
                    sensorNoSend.setChecked(false);
                } else if (SELECTED_SENSOR_TYPE == SENSOR_TYPE_NO_SEND) {
                    sensorPeriod.setChecked(false);
                    sensorButton.setChecked(false);
                    sensorNoSend.setChecked(true);
                }
                sensorAlert.show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setSensorMode (int sensorMode) {
        SELECTED_SENSOR_TYPE = sensorMode;
        changeSensorUI();
    }

    private void changeSensorUI () {
        if (SELECTED_SENSOR_TYPE == SENSOR_TYPE_SEND_WITH_BUTTON) {
            requestSensorValueButton.setVisibility(View.VISIBLE);
            timer.cancel();
        } else {
            if (SELECTED_SENSOR_TYPE == SENSOR_TYPE_SEND_PERIODICALLY) {
                setupTimer();
            } else {
                timer.cancel();
            }
            requestSensorValueButton.setVisibility(View.INVISIBLE);
        }
        updateSensorValue(true);
    }

    private void updateSensorValue (boolean empty) {
        final AppCompatTextView sensorValueView = (AppCompatTextView) findViewById(R.id.sensor_value);
        if (empty) {
            sensorValueView.setText("");
        } else {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    final String sensorValue = request.SendSensorRequest();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (sensorValue != null) {
                                sensorValueView.setText(sensorValue);
                            }
                        }
                    });
                }
            });
        }
    }

    private void setupTimer () {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateSensorValue(false);
            }
        }, 0, 1000);
    }

}
