package gpig.group2.comms;

import gpig.group2.comms.simulator.DoesStatusUpdates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by james on 23/05/2016.
 */
public abstract class ConnectionManager implements DoesStatusUpdates {

    protected List<OutputHandler> outputHandlerList = new ArrayList<>();
    protected Set<OutputHandler> needRemoval = new HashSet<>();


    @Override
    public void bindOutputHandler(OutputHandler connectionHandler) {
        outputHandlerList.add(connectionHandler);
    }

    @Override
    public void removeOutputHandler(OutputHandler connectionHandler) {
        synchronized (this) {
            needRemoval.add(connectionHandler);
        }
    }

    protected void clearDeadConnections(){
        synchronized (this) {
            if(needRemoval.size()>0) {
                for (OutputHandler h : needRemoval) {
                    outputHandlerList.remove(h);
                }
            }
            needRemoval.clear();
        }
    }

}
