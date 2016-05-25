package gpig.group2.dcs.c2integration;

import gpig.group2.dcs.wrapper.StatusWrapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;


/**
 * Created by james on 24/05/2016.
 */
public class HttpC2Integraiton implements C2Integration {

    static Logger logger = LogManager.getLogger();
    @Override
    public void sendDroneStatus(StatusWrapper msg) {

        String url = "http://localhost:10080/GPIGGroup2MapsServer/app/push";

        int responseCode = -1;
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost request = new HttpPost(url);
            logger.debug("Sending " + msg.getText());
            StringEntity params =new StringEntity(msg.getText(),"UTF-8");
            params.setContentType("application/xml");
            request.addHeader("content-type", "application/xml");
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
            request.addHeader("Accept-Language", "en-US,en;q=0.8");
            request.setEntity(params);


            HttpResponse response = httpClient.execute(request);
            responseCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 204) {

                BufferedReader br = new BufferedReader(
                        new InputStreamReader((response.getEntity().getContent())));

                String output;
                // System.out.println("Output from Server ...." + response.getStatusLine().getStatusCode() + "\n");
                while ((output = br.readLine()) != null) {
                    // System.out.println(output);
                }
            }
            else{
                logger.error(response.getStatusLine().getStatusCode());

                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }

        }catch (Exception ex) {
            logger.error("ex Code sendPut: " + ex);
            logger.error("url:" + url);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }



    }
}
