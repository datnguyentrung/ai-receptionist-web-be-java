package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum SparringErrorCode implements ErrorCode {
    ROOM_NOT_FOUND("SPARRING_ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "Sparring room not found", "This ring code is not active"),
    INVALID_ROOM_CODE("SPARRING_ROOM_CODE_INVALID", HttpStatus.BAD_REQUEST, "Invalid ring code", "Ring code must contain exactly four digits"),
    SOCKET_TICKET_INVALID("SPARRING_SOCKET_TICKET_INVALID", HttpStatus.UNAUTHORIZED, "Socket ticket invalid", "Connect again to the sparring room"),
    COMMAND_NOT_ALLOWED("SPARRING_COMMAND_NOT_ALLOWED", HttpStatus.FORBIDDEN, "Sparring command not allowed", "This device cannot send that sparring command"),
    COMMAND_INVALID("SPARRING_COMMAND_INVALID", HttpStatus.BAD_REQUEST, "Sparring command invalid", "The sparring command is invalid for the current room");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
