package gpig.group2.dcs.c2integration;

import gpig.group2.dcs.wrapper.StatusWrapper;
import gpig.group2.models.drone.status.DroneStatusMessage;

/**
 * Created by james on 24/05/2016.
 */
public interface C2Integration {
    void sendDroneStatus(StatusWrapper msg);


}
