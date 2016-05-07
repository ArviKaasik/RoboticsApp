package com.app.robotics.roboticsapp;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.RadioButton;
import android.widget.ToggleButton;
import android.widget.VideoView;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private HttpRequest request;

    private AlertDialog urlAlert;
    private AlertDialog sensorAlert;

    private VideoView videoView;

    private int VIDEO_RESOLUTION_WIDTH = 340;
    private int VIDEO_RESOLUTION_HEIGHT = 240;

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

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        createConnectionAlert();
        createSensorAlert();

        setupTimer();

        request = new HttpRequest();

        requestSensorValueButton = (Button) findViewById(R.id.sensor_get_value);
        requestSensorValueButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    updateSensorValue(false);
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
        addHandlers();
        mv = (MjpegView) findViewById(R.id.mjpeg_view);
    }

    private void SendRequest(float degrees, float offset) {
        double x = 0;
        double y = 0;
        degrees -= 90;
        double distance = 10 * offset;
        Double val = Double.parseDouble(String.valueOf(degrees));
        val = Math.toRadians(val);
        x = Math.cos(val) * distance;
        y = Math.sin(val) * distance;
        final double g_x = x;
        final double g_y = y;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                request.SendSeekerRequest(g_x, g_y);
            }
        });
    }

    private void createConnectionAlert() {
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View connectionChoiceView = li.inflate(R.layout.url_prompt, null);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle("Choose connection");
        alertBuilder.setView(connectionChoiceView);
        alertBuilder.setCancelable(true);

        Button urlConfirm = (Button) connectionChoiceView.findViewById(R.id.url_confirm_button);
        final EditText urlText = (EditText) connectionChoiceView.findViewById(R.id.change_url_edit);

        urlText.setText("192.168.1.125:");
        urlConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpRequest.Url = urlText.getText().toString();
                urlAlert.hide();

            }
        });

        urlAlert = alertBuilder.create();
    }

    private RadioButton sensorPeriod;
    private RadioButton sensorButton;
    private RadioButton sensorNoSend;

    private void createSensorAlert() {
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View sensorChoiceView = li.inflate(R.layout.sensor_send_type, null);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle(R.string.sensor_alert_title);
        String titleString = getString(R.string.sensor_alert_title);
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
                    setSensorMode(SENSOR_TYPE_SEND_PERIODICALLY);
                } else if (sensorButton.isChecked()) {
                    setSensorMode(SENSOR_TYPE_SEND_WITH_BUTTON);
                } else if (sensorNoSend.isChecked()) {
                    setSensorMode(SENSOR_TYPE_NO_SEND);
                }

                sensorAlert.hide();
            }
        });

        sensorAlert = alertBuilder.create();
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
            if (!this.isFinishing()) {
                urlAlert.show();
            }
            return true;
        } else if (id == R.id.action_choose_sensor_mode) {
            if (!this.isFinishing()) {
                CheckCorrectBullet();
                sensorAlert.show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void CheckCorrectBullet () {
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
    }

    private void setSensorMode(int sensorMode) {
        SELECTED_SENSOR_TYPE = sensorMode;
        changeSensorUI();
    }

    private void changeSensorUI() {
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

    private void updateSensorValue(boolean empty) {
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

    private void setupTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateSensorValue(false);
            }
        }, 0, 1000);
    }

    private void addHandlers() {
        final ToggleButton toggleBtn = (ToggleButton) findViewById(R.id.toggBtn);
        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                                      @Override
                                      public void run() {
                                          request.SendSwitchRequest(toggleBtn.isChecked());
                                      }
                                  });
                    }
            });

        Button openVideo = (Button) findViewById(R.id.open_video_btn);

        openVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(MainActivity.class.getSimpleName(), "url: " + request.Url);
                if (request.Url != null && request.Url.endsWith(":")) {
                    //if (videoView != null && videoView.isPlaying()) {
                        //videoView.stopPlayback();
                    //}
                    //startVideoStream();
                    if (!mv.isStreaming()) {
                        startVideoStreamMJPEG();
                    }
                }
            }
        });

        Button stopVideo = (Button) findViewById(R.id.stop_button_btn);

        stopVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVideoStreamMJPEG();
            }
        });
    }

    private void startVideoStream() {
        String url = "http://" + HttpRequest.Url + HttpRequest.videoport + "/video_feed";

        videoView = (VideoView) findViewById(R.id.videoView);

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);
        videoView.setVideoURI(Uri.parse(url));

        videoView.start();
    }

    private void startVideoStreamMJPEG() {
        String url = "http://" + HttpRequest.Url + HttpRequest.videoport + "/video_feed";

        mv.setResolution(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT);

        new DoRead().execute(url);
    }

    private void stopVideoStreamMJPEG() {
        if (mv != null) {
            if (mv.isStreaming()) {
                mv.stopPlayback();
            }
            mv.freeCameraMemory();;
        }
    }

    private static final boolean DEBUG = true;
    private static final String TAG = "MJPEG";

    private MjpegView mv = null;

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5 * 1000);
            HttpConnectionParams.setSoTimeout(httpParams, 5 * 1000);
            if (DEBUG) Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                if (DEBUG)
                    Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if (res.getStatusLine().getStatusCode() == 401) {
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            } catch (ClientProtocolException e) {
                if (DEBUG) {
                    e.printStackTrace();
                    Log.d(TAG, "Request failed-ClientProtocolException", e);
                }
                //Error connecting to camera
            } catch (IOException e) {
                if (DEBUG) {
                    e.printStackTrace();
                    Log.d(TAG, "Request failed-IOException", e);
                }
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            if(result!=null) result.setSkip(1);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(false);
        }
    }
}