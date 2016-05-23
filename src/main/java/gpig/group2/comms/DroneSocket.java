package gpig.group2.comms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

/**
 * Created by james on 23/05/2016.
 */
public class DroneSocket {

    public DroneSocket() throws IOException {
        Socket MyClient = null;
        try {
            MyClient = new Socket("127.0.0.1", 9988);
        }
        catch (IOException e) {
            System.out.println(e);
        }

        DataOutputStream output = null;
        DataInputStream input = null;
        try {
            output = new DataOutputStream(MyClient.getOutputStream());
            input = new DataInputStream(MyClient.getInputStream());
        }
        catch (IOException e) {
            System.out.println(e);
        }


        String responseLine;
        while ((responseLine = input.readLine()) != null) {
            System.out.println("Server: " + responseLine);

        }
    }

    public static void main(String[] args) throws IOException {
        new DroneSocket();
    }
}
