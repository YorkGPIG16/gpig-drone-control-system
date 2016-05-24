package gpig.group2.dcs;

import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.status.StatusMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.Socket;

/**
 * Created by james on 23/05/2016.
 */
public class DroneConnectionHandler implements Runnable, OutputHandler {
    Socket serviceSocket;
    DoesStatusUpdates uut;

    DataOutputStream output;
    DataInputStream input;

    Logger log = LogManager.getLogger(DroneConnectionHandler.class);

    DroneInterface di;

    public DroneConnectionHandler(Socket serviceSocket, DoesStatusUpdates uut, DroneInterface di) {
        this.serviceSocket = serviceSocket;
        this.uut = uut;
        this.di = di;
    }

    @Override
    public void run() {
        try {
            output = new DataOutputStream(serviceSocket.getOutputStream());
            input = new DataInputStream(serviceSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        uut.bindOutputHandler(this);


        String responseLine;
        try {
            while ((responseLine = input.readLine()) != null) {
                log.debug("Server: " + responseLine);

                {
                    try {
                        JAXBContext jc = JAXBContext.newInstance(StatusMessage.class);
                        Unmarshaller u = jc.createUnmarshaller();

                        StatusMessage sm = (StatusMessage) u.unmarshal(new StringReader(responseLine));

                        log.info("Got status message");
                        di.handleStatusMessage(sm);
                        continue;
                    } catch (JAXBException e) {
                        //e.printStackTrace();
                    }
                }


                {
                    try {
                        JAXBContext jc = JAXBContext.newInstance(ResponseMessage.class);
                        Unmarshaller u = jc.createUnmarshaller();

                        ResponseMessage rm = (ResponseMessage) u.unmarshal(new StringReader(responseLine));
                        log.info("Got response message");
                        di.handleResponseMessage(rm);
                        continue;


                    } catch (JAXBException e) {
                        //e.printStackTrace();
                    }
                }


                {
                    try {
                        JAXBContext jc = JAXBContext.newInstance(RequestMessage.class);
                        Unmarshaller u = jc.createUnmarshaller();

                        RequestMessage rq = (RequestMessage) u.unmarshal(new StringReader(responseLine));
                        log.info("Got request message");
                        di.handleRequestMessage(rq);
                        continue;


                    } catch (JAXBException e) {
                        //e.printStackTrace();
                    }
                }

                log.warn("Received invalid response message. Ignoring");
            }
        } catch (IOException e) {
            log.error("Unexpected client disconnect");
            log.error(e);
            uut.removeOutputHandler(this);
        }


    }

    @Override
    public void onOutput(CommonObject obj) {
        try {
            log.debug("Sending Drone Commands: " +obj.getText());
            output.writeBytes(obj.getText()+"\n");
        } catch (IOException e) {
            uut.removeOutputHandler(this);
            log.warn("Client went away");
        }
    }
}
