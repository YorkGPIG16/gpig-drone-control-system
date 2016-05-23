package gpig.group2.comms.simulator;

import gpig.group2.comms.ConnectionHandler;
import gpig.group2.comms.OutputHandler;

/**
 * Created by james on 23/05/2016.
 */
public interface DoesStatusUpdates {
    void bindOutputHandler(OutputHandler connectionHandler);

    void removeOutputHandler(OutputHandler connectionHandler);
}
