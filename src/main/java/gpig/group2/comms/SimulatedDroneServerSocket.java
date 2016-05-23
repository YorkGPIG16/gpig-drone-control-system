package gpig.group2.comms;

import gpig.group2.comms.simulator.DoesStatusUpdates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by james on 23/05/2016.
 */
public class SimulatedDroneServerSocket {

    Logger log = LogManager.getLogger(SimulatedDroneServerSocket.class);

    public SimulatedDroneServerSocket(DoesStatusUpdates uut) {
        log.info("In Simulated Server Socket");
        ServerSocket MyService = null;
        try {
            MyService = new ServerSocket(9988);
        }
        catch (IOException e) {
            System.out.println(e);
        }


        while(true) {
            try {
                log.info("Waiting for connection");
                Socket serviceSocket = MyService.accept();

                log.info("Connection Accepted");
                Runnable connectionHandler = new ConnectionHandler(serviceSocket,uut);
                new Thread(connectionHandler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }
}
