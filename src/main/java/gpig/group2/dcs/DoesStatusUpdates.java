package gpig.group2.dcs;

/**
 * Created by james on 24/05/2016.
 */
public interface DoesStatusUpdates {
    void bindOutputHandler(OutputHandler connectionHandler);
    void removeOutputHandler(OutputHandler connectionHandler);
}

