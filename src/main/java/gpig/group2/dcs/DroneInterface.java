package gpig.group2.dcs;

import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.status.DroneStatusMessage;

/**
 * Created by james on 23/05/2016.
 */
public interface DroneInterface {
    void handleStatusMessage(DroneStatusMessage sm);
    void handleResponseMessage(ResponseMessage rm);
    void handleRequestMessage(RequestMessage rq);
    void registerOutputHandler(OutputHandler handler);
}
