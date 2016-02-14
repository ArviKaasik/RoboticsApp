package com.app.robotics.roboticsapp;

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

    public  HttpURLConnection connection = null;



    public String SendSeekerRequest (int x) {

        //Create connection
        String request = "/update_seeker?x=" + x;

        try {
            //urli asemele peab sisestama nutiroboti IP:PORT
            URL url = new URL("http://192.168.1.64:8080" + request);
            return PostRequest(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String PostRequest (URL url) {
        try {
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
            //urli asemele peab sisestama nutiroboti IP:PORT
            URL url = new URL("http://192.168.1.64:8080" + request);
            return PostRequest(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
