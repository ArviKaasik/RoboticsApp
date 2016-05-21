package com.app.robotics.roboticsapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Arvi on 2/4/2016.
 */
public class HttpRequest {

    public static int commandport = 8080; // käskude port
    public static int videoport = 5000; // video edastamise port

    public HttpURLConnection connection = null;

    public static String Url; // kasutuses olev URL, formaadis: "192.168.1.222:"
    // juhtkangi meetod
    public String SendSeekerRequest (double x, double y) {

        //loome urli järgse teksti, mida kasutatakse päringu tüübi jaoks.
        String request = "/update_servo?x=" + x + "&y=" + y;

        try {
            URL url = new URL ("http://" + Url + commandport + request);
            return PostRequest(url); // päringu tegemise meetod
        } catch (Exception e) {
            // vea puhul püüame selle, android studioga ühendatud rakendusel näeme eelnevalt välja
            // kutsutud meetodeid ning saame viga jälitada
            e.printStackTrace();
            return null;
        }
    }

    public String SendSensorRequest () {
        // anduri päring
        String request = "/get_sensor";
        try {
            URL url = new URL ("http://" + Url + commandport + request);
            return PostRequest(url); // päringu tegemise meetod
        } catch (Exception e) {
            // vea puhul püüame selle, android studioga ühendatud rakendusel näeme eelnevalt välja
            // kutsutud meetodeid ning saame viga jälitada
            e.printStackTrace();
            return null;
        }
    }

    private String PostRequest (URL url) {
        try {
            // ühenduse loomine
            connection = (HttpURLConnection) url.openConnection();

            // päringu päis
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/html");

            connection.setUseCaches(false);
            connection.setDoOutput(true);
            // päringu saatmine

            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.close();

            // wastuse saamine

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if not Java 5+

            String line;
            while((line = rd.readLine()) != null) {
                response.append(line);
                System.out.println("Response: " + response);
                response.append('\r');
            }
            rd.close();
            // tagastame päringu vastuse
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
    }

    public String SendSwitchRequest (boolean OnOff){
            String request = "";

            // sisse_välja lülitamise päring
            if (OnOff) {
                request = "/update_LED?x=1";
            } else {
                request = "/update_LED?x=0";
            }
        try {
            URL url = new URL("http://" + Url + commandport + request);
            return PostRequest(url);
        } catch (Exception e) {
            // vea puhul püüame selle, android studioga ühendatud rakendusel näeme eelnevalt välja
            // kutsutud meetodeid ning saame viga jälitada
            e.printStackTrace();
            return null;
        }
    }
}
