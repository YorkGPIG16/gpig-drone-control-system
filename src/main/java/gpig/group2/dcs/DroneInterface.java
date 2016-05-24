package gpig.group2.dcs;

import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.status.DroneStatusMessage;

/**
 * Created by james on 23/05/2016.
 */
public interface DroneInterface {
    void handleStatusMessage(int id, DroneStatusMessage sm);
    void handleResponseMessage(int id, ResponseMessage rm);
    void handleRequestMessage(int id, RequestMessage rq);
    void registerOutputHandler(OutputHandler handler);
}
