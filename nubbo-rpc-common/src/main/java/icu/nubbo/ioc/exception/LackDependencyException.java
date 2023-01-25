package icu.nubbo.ioc.exception;

public class LackDependencyException extends RuntimeException {

    public LackDependencyException(String msg) {
        super(msg);
    }
}
