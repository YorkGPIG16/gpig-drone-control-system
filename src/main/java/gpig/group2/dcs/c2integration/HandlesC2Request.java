package gpig.group2.dcs.c2integration;

import gpig.group2.models.drone.request.RequestMessage;

/**
 * Created by james on 25/05/2016.
 */
public interface HandlesC2Request {
    void handleRequest(RequestMessage rm);
}
