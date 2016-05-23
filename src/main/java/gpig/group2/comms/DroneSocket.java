package gpig.group2.comms;

import gpig.group2.comms.simulator.DoesStatusUpdates;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.status.StatusMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.Socket;

/**
 * Created by james on 23/05/2016.
 */
public class DroneSocket implements OutputHandler {

    DataOutputStream output = null;
    DataInputStream input = null;

    Logger log = LogManager.getLogger(DroneSocket.class);
    public DroneSocket(final DroneInterface di, final DoesStatusUpdates outputHandler) throws IOException {
        Socket MyClient = null;
        try {
            MyClient = new Socket("127.0.0.1", 9988);
        }
        catch (IOException e) {
            System.out.println(e);
        }


        try {
            output = new DataOutputStream(MyClient.getOutputStream());
            input = new DataInputStream(MyClient.getInputStream());
        }
        catch (IOException e) {
            System.out.println(e);
        }

        outputHandler.bindOutputHandler(this);

        String responseLine;
        while ((responseLine = input.readLine()) != null) {
            System.out.println("Server: " + responseLine);

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
    }

    public static void main(String[] args) throws IOException {

        C2DroneInterface di = new C2DroneInterface();
        new DroneSocket(di, di);
    }

    @Override
    public void onOutput(CommonObject obj) {
        try {
            output.writeBytes(obj.getText()+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
