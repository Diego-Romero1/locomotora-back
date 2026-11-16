package com.locomotora.demo.ai.client;

/**
 * La API del modelo fallo, tardo demasiado, no esta configurada o rechazo la peticion.
 *
 * @see #status() codigo HTTP del proveedor (0 si no hubo respuesta).
 * @see #retryAfterSeconds() espera pedida por el proveedor en un 429 (null si no la dio).
 */
public class AiUnavailableException extends RuntimeException {
    private final int status;
    private final Long retryAfterSeconds;

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
        this.retryAfterSeconds = null;
    }

    public AiUnavailableException(String message) {
        this(message, 0, null);
    }

    public AiUnavailableException(String message, int status, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int status() {
        return status;
    }

    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }

    /** El proveedor limito la cuenta (429) o esta saturado (498). */
    public boolean rateLimited() {
        return status == 429 || status == 498;
    }

    /** El proveedor rechazo el contenido o la salida no cumplio el esquema (400/422). */
    public boolean rejected() {
        return status == 400 || status == 422;
    }
}
