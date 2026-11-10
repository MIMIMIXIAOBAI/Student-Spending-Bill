package com.campusbill.app.chat.ai.assistant;

public final class AssistantCallException extends RuntimeException {
    public final FailureKind kind;

    public AssistantCallException(FailureKind kind, String message) {
        super(message == null ? "" : message);
        this.kind = kind == null ? FailureKind.NETWORK : kind;
    }
}
