package icu.nubbo.ioc.exception;

public class CircularDependence extends RuntimeException {

    public CircularDependence(String msg) {
        super(msg);
    }
}
