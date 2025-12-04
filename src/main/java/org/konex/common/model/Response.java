package org.konex.common.model;

import java.io.Serializable;

public class Response<T> implements Serializable {
    private String command;
    private boolean success;
    private T data;

    public Response(String command, boolean success, T data) {
        this.command = command;
        this.success = success;
        this.data = data;
    }

    public static <T> Response<T> success(String command, T data) {
        return new Response<>(command, true, data);
    }

    public static <T> Response<T> error(String command, String errorMessage) {
        return new Response<>(command, false, null);
    }

    public String getCommand() {
        return command;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }
}