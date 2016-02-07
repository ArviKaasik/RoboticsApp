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

    public String SendSwitchRequest (boolean OnOff){
        try {
            //Create connection
            String request = "";

            if (OnOff) {
                request = "/update_LED?x=1";
            } else {
                request = "/update_LED?x=0";
            }
            //urli asemele peab sisestama nutiroboti IP:PORT
            URL url = new URL("http://192.168.1.103:8080" + request);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/html");

            connection.setUseCaches(false);
            connection.setDoOutput(true);
            //Send request

            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(request);
            System.out.println(request);
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

}
