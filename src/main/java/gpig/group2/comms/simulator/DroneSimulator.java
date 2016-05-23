package gpig.group2.comms.simulator;

import gpig.group2.comms.SimulatedDroneServerSocket;
import gpig.group2.maps.platform.Drone;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by james on 23/05/2016.
 */
public class DroneSimulator {

    Logger log = LogManager.getLogger(DroneSimulator.class);

    public DroneSimulator() {

        System.out.println("Drone sim");

        log.info("Drone simulator");

        SimulatedDrone drone = new SimulatedDrone();
        Thread t = new Thread(drone);
        t.start();


        SimulatedDroneServerSocket sdss = new SimulatedDroneServerSocket(drone);


    }

    public static void main(String[] args) {
        new DroneSimulator();
    }

}
