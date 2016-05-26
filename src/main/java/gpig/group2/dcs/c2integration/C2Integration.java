package gpig.group2.dcs.c2integration;

import gpig.group2.dcs.wrapper.ResponseWrapper;
import gpig.group2.dcs.wrapper.StatusWrapper;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseData;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.status.DroneStatusMessage;

/**
 * Created by james on 24/05/2016.
 */
public interface C2Integration {
    void sendDroneStatus(StatusWrapper msg);
    void sendPOI(ResponseData msg);
    void sendCompletedDeploymentArea(ResponseData rd);

    RequestMessage getRequests();
}
