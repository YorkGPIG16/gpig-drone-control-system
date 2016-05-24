package gpig.group2.dcs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by james on 24/05/2016.
 */
public class DCSDroneConnectionManager extends ConnectionManager implements DoesStatusUpdates, OutputHandler {
    static Logger log = LogManager.getLogger(DCSDroneConnectionManager.class);

    DroneInterface c2DroneInterface;
    ServerSocket socket;
    public DCSDroneConnectionManager(DroneInterface c2DroneInterface) throws IOException {
        socket = new ServerSocket(9876);
        this.c2DroneInterface = c2DroneInterface;





    }


    public void listen() throws IOException {
        log.info("Listening on port " + socket.getLocalPort());
        while(true) {
            Socket clientConnection = socket.accept();

            log.info("Connection accepted from "+ clientConnection.getRemoteSocketAddress().toString());
            Runnable connectionHandler = new DroneConnectionHandler(clientConnection,this, c2DroneInterface);
            new Thread(connectionHandler).start();
        }
    }
    @Override
    public void onOutput(CommonObject obj) {
        clearDeadConnections();
        for(OutputHandler h : outputHandlerList) {
            h.onOutput(obj);
        }
    }


    public static void main(String args[]) {
        C2DroneInterface c2Bridge = new C2DroneInterface();

        try {
            DCSDroneConnectionManager connm = new DCSDroneConnectionManager(c2Bridge);
            c2Bridge.registerOutputHandler(connm);


            connm.listen();
        } catch (IOException e) {
            log.fatal(e);
        }


    }


}
