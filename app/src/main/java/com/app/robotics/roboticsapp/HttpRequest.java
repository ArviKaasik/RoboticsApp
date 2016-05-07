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

    public static int commandport = 8080;
    public static int videoport = 5000;

    public HttpURLConnection connection = null;

    public static String Url;

    public String SendSeekerRequest (double x, double y) {

        //Create connection
        String request = "/update_servo?x=" + x + "&y=" + y;

        try {
            //urli asemele peab sisestama roboti IP:PORT
            URL url = new URL ("http://" + Url + commandport + request);
            return PostRequest(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String SendSensorRequest () {
        String request = "/get_sensor";
        try {
            URL url = new URL ("http://" + Url + commandport + request);
            return PostRequest(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String PostRequest (URL url) {
        try {
            System.out.println("Try to contact url: " + url);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/html");

            connection.setUseCaches(false);
            connection.setDoOutput(true);
            //Send request

            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.close();

            //Get Response

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
            //Create connection
            String request = "";

            if (OnOff) {
                request = "/update_LED?x=1";
            } else {
                request = "/update_LED?x=0";
            }
        try {
            URL url = new URL("http://" + Url + commandport + request);
            return PostRequest(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
