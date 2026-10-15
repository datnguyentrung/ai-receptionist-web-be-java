package com.dat.ai_receptionist_web.controller.Sparring;

import com.dat.ai_receptionist_web.dto.Sparring.SparringRoomResponse;
import com.dat.ai_receptionist_web.dto.Sparring.SparringSocketTicketResponse;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.service.Sparring.SparringRoomRegistry;
import com.dat.ai_receptionist_web.service.Sparring.SparringSocketTicketService;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sparring/rooms")
@RequiredArgsConstructor
public class SparringRoomController {
    private final SparringRoomRegistry rooms;
    private final SparringSocketTicketService tickets;

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SPARRING_OPERATE.getCode())")
    public SparringRoomResponse create() { return rooms.create(); }

    @GetMapping("/{roomCode}")
    public SparringRoomResponse get(@PathVariable String roomCode) { return rooms.get(roomCode); }

    @PostMapping("/{roomCode}/socket-ticket")
    public SparringSocketTicketResponse socketTicket(@PathVariable String roomCode) {
        rooms.get(roomCode);
        UUID userId = SecurityUtil.getCurrentUserId().orElseThrow();
        boolean operator = SecurityUtil.getJwt()
                .map(jwt -> jwt.getClaimAsStringList("permissions"))
                .map(values -> values.contains(PermissionDefinition.SPARRING_OPERATE.getCode()))
                .orElse(false);
        SparringSocketTicketService.IssuedTicket ticket = tickets.issue(roomCode, userId, operator);
        return new SparringSocketTicketResponse(roomCode, ticket.value(), operator, ticket.expiresAtEpochMs());
    }
}
