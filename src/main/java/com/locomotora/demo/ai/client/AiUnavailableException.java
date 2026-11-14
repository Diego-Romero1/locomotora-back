package com.locomotora.demo.ai.client;

/** La API del modelo fallo, tardo demasiado o no esta configurada. */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiUnavailableException(String message) {
        super(message);
    }
}
