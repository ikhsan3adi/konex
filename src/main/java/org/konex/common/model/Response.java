package org.konex.common.model;

import java.io.Serializable;

public class Response<T extends Serializable> implements Serializable {
    private final String command;
    private final boolean success;
    private final String message;
    private final T data;

    public Response(String command, boolean success, String message, T data) {
        this.command = command;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T extends Serializable> Response<T> success(String command, T data) {
        return new Response<>(command, true, "OK", data);
    }

    public static <T extends Serializable> Response<T> error(String command, String errorMessage) {
        return new Response<>(command, false, errorMessage, null);
    }

    public String getCommand() {
        return command;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Response{" +
                "command='" + command + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}