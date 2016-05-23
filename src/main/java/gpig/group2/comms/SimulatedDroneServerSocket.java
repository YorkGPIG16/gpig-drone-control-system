package gpig.group2.comms;

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


    public static void main(String[] args) throws IOException {
        new SimulatedDroneServerSocket();
    }

    public SimulatedDroneServerSocket() {
        ServerSocket MyService = null;
        try {
            MyService = new ServerSocket(9988);
        }
        catch (IOException e) {
            System.out.println(e);
        }

        Socket serviceSocket = null;
        try {
            serviceSocket = MyService.accept();


            DataOutputStream output;
            DataInputStream input;

            output = new DataOutputStream(serviceSocket.getOutputStream());
            input = new DataInputStream(serviceSocket.getInputStream());


            output.writeBytes("This is a test string\nNewline\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }



    }
}
