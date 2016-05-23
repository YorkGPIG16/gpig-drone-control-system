package gpig.group2.comms;

import gpig.group2.comms.simulator.DoesStatusUpdates;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.status.StatusMessage;

/**
 * Created by james on 23/05/2016.
 */
public class C2DroneInterface extends ConnectionManager implements DroneInterface, DoesStatusUpdates {
    @Override
    public void handleStatusMessage(StatusMessage sm) {

    }

    @Override
    public void handleResponseMessage(ResponseMessage rm) {

    }

    @Override
    public void handleRequestMessage(RequestMessage rq) {

    }

    @Override
    public void bindOutputHandler(OutputHandler connectionHandler) {

    }

    @Override
    public void removeOutputHandler(OutputHandler connectionHandler) {

    }
}
