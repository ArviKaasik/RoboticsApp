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

        // loome tööriistariba
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // loome anduri ja ühenduse valikute jaoks popup aknad
        createConnectionAlert();
        createSensorAlert();

        // initialiseerime anduri taimeri
        setupTimer();

        request = new HttpRequest();

        addHandlers(); // loetakse elemendid XML-ist Java koodi

        changeSensorUI(); // muudame kasutajaliidest (esialgu ei tohiks nupp olla nähtaval)

        mv = (MjpegView) findViewById(R.id.mjpeg_view);
    }

    private void SendRequest(float degrees, float offset) {
        double x = 0;
        double y = 0;
        // joondame nurga ümber (tagurpidi ühendades, saab nurka 180 kraadi muutes panna roboti õiget pidi liikuma
        degrees -= 90;
        // suurendame kaugust, muutes saatmise väärtused sobivaks
        double distance = 10 * offset;
        Double val = Double.parseDouble(String.valueOf(degrees));
        // kraadid radiaanideks
        val = Math.toRadians(val);
        // tekivad täisnurksed kolmurgad, mille abil saame munaku x ja t koordinaadid arvutada
        x = Math.cos(val) * distance;
        y = Math.sin(val) * distance;
        final double g_x = x;
        final double g_y = y;
        // saadame teisel lõimel robotile servode päringu
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                request.SendSeekerRequest(g_x, g_y);
            }
        });
    }

    // loome ühenduse valimise vaate
    private void createConnectionAlert () {
        // loome java objekti kasutades url_prompt kujunduse faili
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View connectionChoiceView = li.inflate(R.layout.url_prompt, null);

        // akna loomine
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle("Choose connection"); // akna pealkiri
        alertBuilder.setView(connectionChoiceView); // vaate lisamine sisuks
        alertBuilder.setCancelable(true);

        // loome valiku kinnitamise ja URLi sisestamise välja Java objektid
        Button urlConfirm = (Button) connectionChoiceView.findViewById(R.id.url_confirm_button);
        final EditText urlText = (EditText) connectionChoiceView.findViewById(R.id.change_url_edit);

        urlText.setText("192.168.1.125:"); // näidisurl, siia peab sisestama Raspberry Pi IP aadressi.
        // lisame URL kinnituse vajutusele kuulari
        urlConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // nupule vajutades võtame URLi kasutusele ja peidame popupi
                HttpRequest.Url = urlText.getText().toString();
                urlAlert.hide();

            }
        });
        // lõpuks loome vaate objekti, mida saame hiljem näidata
        urlAlert = alertBuilder.create();
    }

    private RadioButton sensorPeriod;
    private RadioButton sensorButton;
    private RadioButton sensorNoSend;

    // loome anduri tüübi valiku akna
    private void createSensorAlert() {
        // loome java objekti kujunduse failist
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View sensorChoiceView = li.inflate(R.layout.sensor_send_type, null);

        // tüübi aken
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle(R.string.sensor_alert_title); // valime pealkirja
        alertBuilder.setView(sensorChoiceView); // lisame sisu vaate
        alertBuilder.setCancelable(true);

        // loome kinnituse nupu ja tüüpida valikute java objektid
        final Button sensorConfirm = (Button) sensorChoiceView.findViewById(R.id.sensor_confirm_btn);
        sensorPeriod = (RadioButton) sensorChoiceView.findViewById(R.id.sensor_period);
        sensorButton = (RadioButton) sensorChoiceView.findViewById(R.id.sensor_btn_press);
        sensorNoSend = (RadioButton) sensorChoiceView.findViewById(R.id.sensor_no_send);

        // Lisame valiku kinnitamise nupule kuulari
        sensorConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // vastavalt valitud tüübi nupule salvestame valiku
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
        // lõpuks loome vaate eelnevalt defineeritud koodi abil
        sensorAlert = alertBuilder.create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Võtame menüü kasutusele, menüüle lisatakse kõik XML-is kirjeldatud elemendid
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // reageerime menüü elementide vajutuamisele
        int id = item.getItemId();

        // Valiti ühenduse valimise nuppu
        if (id == R.id.action_choose_connection) {
            if (!this.isFinishing()) {
                // Näitame ühenduse valiku popupi
                urlAlert.show();
            }
            return true;
            // valiti sensori andmete edastuse tüübi valiku nuppu
        } else if (id == R.id.action_choose_sensor_mode) {
            if (!this.isFinishing()) {
                // märgistame hetkel aktiivse anduri valiku
                CheckCorrectBullet();
                // näitame andmete valiku popupi
                sensorAlert.show();
            }
            return true;
        }
        // juhul kui siin kirjeldatud meetodile ei vajutatud, kutsume välja baasklassi
        return super.onOptionsItemSelected(item);
    }

    void CheckCorrectBullet () {
        // vastavalt rakenduse mälus salvestatud väärtusele märgistame praegu kehtiva
        // andurilt andmete lugemise tüübi
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

    private void setSensorMode (int sensorMode) {
        // salvestame lokaalses muutujas andurilt lugemise tüübi
        SELECTED_SENSOR_TYPE = sensorMode;
        // uuendame kasutajalidiest
        changeSensorUI();
    }

    private void changeSensorUI () {
        // kui anduri lugemiseks kasutatakse nuppu
        if (SELECTED_SENSOR_TYPE == SENSOR_TYPE_SEND_WITH_BUTTON) {
            // teeme nupu nähtavaks
            requestSensorValueButton.setVisibility(View.VISIBLE);
            // igaks juhuks katkestame taimeri
            timer.cancel();
        } else {
            // andurilt loetakse andmeid regulaarselt
            if (SELECTED_SENSOR_TYPE == SENSOR_TYPE_SEND_PERIODICALLY) {
                // seadistama taimeri
                setupTimer();
                //andurilt ei loeta andmeid
            } else {
                // taimer peatatakse
                timer.cancel();
            }
            // teeme nupu nähtamatuks
            requestSensorValueButton.setVisibility(View.INVISIBLE);
        }
        // nullime kasutajaliideses anduri väärtuse
        updateSensorValue(true);
    }

    private void updateSensorValue (boolean empty) {
        final AppCompatTextView sensorValueView = (AppCompatTextView) findViewById(R.id.sensor_value);

        if (empty) {
            sensorValueView.setText("");
        } else {
            // teeme päringu raspberry Pi serverile andurilt andmete saamiseks
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    final String sensorValue = request.SendSensorRequest();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // uuendame saadud väärtusega kasutajaliidest
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
        // teeme uue taimeri
        timer = new Timer();
        // määrame taimeri sageduse (sageduse muutmiseks peab viimast arvu muutma) millisekundites
        // ja meetod, mida käivitatakse iga kord, kui fikseeritud aeg on möödunud
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateSensorValue(false);
            }
        }, 0, 1000);
    }

    private void addHandlers() {
        // Loome Java objekti LED tule lülitamise nupu jaoks
        final ToggleButton toggleBtn = (ToggleButton) findViewById(R.id.toggBtn);
        // Lisame kuulari LED tule nupule
        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                                      @Override
                                      public void run() {
                                          // teeme päringu, anname päringusse nupu praeguse väärtuse
                                          // ja ütleme sellega kas LED tuli läheb põlema või kustub ära
                                          request.SendSwitchRequest(toggleBtn.isChecked());
                                      }
                                  });
                    }
            });
        // Loome Java objekti video avamise nupu jaoks
        Button openVideo = (Button) findViewById(R.id.open_video_btn);
        // lisame kuulari video avamise nupu jaoks
        openVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // logi, mida saab terminalis jälgida
                Log.d(MainActivity.class.getSimpleName(), "url: " + request.Url);
                // kontrollime päringu urli valiidsust
                if (request.Url != null && request.Url.endsWith(":")) {
                    // välja kommenteeritud koodi saab kasutada, kui kasutada androidi poolt toetatud videoedastust
                    /*if (videoView != null && videoView.isPlaying()) {
                        videoView.stopPlayback();
                    }
                    startVideoStream();*/
                    // See osa koodi on MJPEG video edastamise jaoks
                    if (!mv.isStreaming()) {
                        startVideoStreamMJPEG();
                    }
                }
            }
        });

        // loome Java objekti video peatamise nupu jaoks.
        Button stopVideo = (Button) findViewById(R.id.stop_button_btn);

        // lisame video peatamise kuulari
        stopVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // käivitame peatamise meetodi
                stopVideoStreamMJPEG();
            }
        });

        // loome Java objekti andurilt andmete nupu jaoks
        requestSensorValueButton = (Button) findViewById(R.id.sensor_get_value);

        // lisame kuulari
        requestSensorValueButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // nupu vajutus on edukas
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // meetod, milles tehakse anduri päring
                    updateSensorValue(false);
                    return true;
                }
                // kui vajutus ei ole edukas, tagastame false, süsteem saab aru, et me ei teinud midagi selle vajutusega
                return false;
            }
        });

        // juhtkangi Java objekti loomine
        Joystick joystick = (Joystick) findViewById(R.id.joystick);

        // lisame kuulari
        joystick.setJoystickListener(new JoystickListener() {
            @Override
            // vajutati alla, ei reageeri
            public void onDown() {

            }

            @Override
            // juhtkangi liigutatakse
            public void onDrag(float degrees, float offset) {
                // edastame munaku suuna ja kauguse keskkohast
                SendRequest(degrees, offset);
            }

            @Override
            // vajutus lõpetati
            public void onUp() {
                // anname teada, et nurk on 0 kraadi ja kaugus keskmisest kohast 0
                SendRequest(0, 0);
            }
        });
    }

    private void startVideoStream() {
        // meetod, mis kasutab androidi video edastamise klasse, ei toeta MJPEG video edastamist
        String url = "http://" + HttpRequest.Url + HttpRequest.videoport + "/video_feed";

        videoView = (VideoView) findViewById(R.id.videoView);

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);
        videoView.setVideoURI(Uri.parse(url));

        videoView.start();
    }

    private void startVideoStreamMJPEG() {
        // meetod kasutab MJPEG video edastamist
        String url = "http://" + HttpRequest.Url + HttpRequest.videoport + "/video_feed";

        // määrame vaate resolutsiooni
        mv.setResolution(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT);

        // käivitame video edastuse kokku pandud URLil
        new DoRead().execute(url);
    }

    private void stopVideoStreamMJPEG() {
        if (mv != null) {
            // lõpetame video edastuse ja vabastame mälu
            if (mv.isStreaming()) {
                mv.stopPlayback();
            }
            mv.freeCameraMemory();;
        }
    }

    private static final boolean DEBUG = true;
    private static final String TAG = "MJPEG";

    private MjpegView mv = null;

    // sisse toodud meetod MJPEG video edastamise alustamiseks

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
