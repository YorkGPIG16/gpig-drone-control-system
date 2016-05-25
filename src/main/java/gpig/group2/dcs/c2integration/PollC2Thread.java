package gpig.group2.dcs.c2integration;

import co.j6mes.infra.srf.query.QueryResponse;
import co.j6mes.infra.srf.query.ServiceQuery;
import co.j6mes.infra.srf.query.SimpleServiceQuery;
import gpig.group2.models.drone.request.RequestMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by james on 25/05/2016.
 */
public class PollC2Thread implements Runnable {

    private boolean connectionUpMaps = false;
    private String mapsPath = "";

    final PollC2Thread tthis;
    static Logger log = LogManager.getLogger();

    private RequestMessage state = null;

    public synchronized RequestMessage getState() {
        return state;
    }


    public PollC2Thread() {
        tthis = this;

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    synchronized (tthis) {
                        connectionUpMaps = false;

                        ServiceQuery sq = new SimpleServiceQuery();

                        QueryResponse qr = sq.query("c2", "maps");
                        if (qr.Path != null) {
                            connectionUpMaps = true;
                            mapsPath = "http://" + qr.IP + ":" + qr.Port + "/" + qr.Path;
                        }

                        tthis.notify();
                    }


                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            }
        });
        t1.start();
    }

    @Override
    public void run() {

        while(true) {

            doRequest();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private synchronized void doRequest() {

        String url = "";
        synchronized (tthis) {
            while(!connectionUpMaps) {
                try {
                    tthis.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            url = mapsPath+"deployAreas";
        }

        try {

            Content cnt = Request.Get(url).execute().returnContent();
            log.debug("Got response from C2: " + cnt.asString());

            JAXBContext jc = JAXBContext.newInstance(RequestMessage.class);
            Unmarshaller u = jc.createUnmarshaller();

            RequestMessage rm = (RequestMessage) u.unmarshal(cnt.asStream());

            log.info("Unmarshalled C2 Response OK");
            this.state = rm;

        }catch (Exception ex) {
            log.error("ex Code sendPut: " + ex);
            log.error("url:" + url);
        } finally {
        }


    }
}
