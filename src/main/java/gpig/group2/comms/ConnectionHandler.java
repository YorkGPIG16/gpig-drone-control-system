package gpig.group2.comms;

import gpig.group2.comms.simulator.DoesStatusUpdates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by james on 23/05/2016.
 */
public class ConnectionHandler implements Runnable, OutputHandler {
    Socket serviceSocket;
    DoesStatusUpdates uut;

    DataOutputStream output;
    DataInputStream input;

    Logger log = LogManager.getLogger(ConnectionHandler.class);

    public ConnectionHandler(Socket serviceSocket, DoesStatusUpdates uut) {
        this.serviceSocket = serviceSocket;
        this.uut = uut;
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

    }

    @Override
    public void onOutput(CommonObject obj) {
        try {
            output.writeBytes(obj.getText()+"\n");
        } catch (IOException e) {
            uut.removeOutputHandler(this);
            log.warn("Client went away");
        }
    }
}
