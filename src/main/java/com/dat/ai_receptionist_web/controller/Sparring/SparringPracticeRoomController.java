package com.dat.ai_receptionist_web.controller.Sparring;

import com.dat.ai_receptionist_web.dto.Sparring.*;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.service.Core.SupabaseStorageService;
import com.dat.ai_receptionist_web.service.Sparring.SparringPracticeRoomRegistry;
import com.dat.ai_receptionist_web.service.Sparring.SparringSocketTicketService;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** API cho host web tạo/cấu hình phòng tập và cho mọi client lấy ticket socket. */
@RestController
@RequestMapping("/api/v1/sparring/practice-rooms")
@RequiredArgsConstructor
public class SparringPracticeRoomController {
    private final SparringPracticeRoomRegistry rooms;
    private final SparringSocketTicketService tickets;
    private final SupabaseStorageService storage;

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SPARRING_OPERATE.getCode())")
    public SparringPracticeRoomResponse create(@Valid @RequestBody SparringPracticeCreateRequest request) { return rooms.create(request); }

    @GetMapping("/{roomCode}")
    public SparringPracticeRoomResponse get(@PathVariable String roomCode) { return rooms.get(roomCode); }

    @PostMapping("/{roomCode}/socket-ticket")
    public SparringSocketTicketResponse socketTicket(@PathVariable String roomCode) {
        rooms.get(roomCode);
        UUID userId=SecurityUtil.getCurrentUserId().orElseThrow();
        boolean operator=SecurityUtil.getJwt().map(jwt -> jwt.getClaimAsStringList("permissions"))
                .map(values -> values.contains(PermissionDefinition.SPARRING_OPERATE.getCode())).orElse(false);
        SparringSocketTicketService.IssuedTicket ticket=tickets.issue(roomCode,userId,operator,"practice");
        return new SparringSocketTicketResponse(roomCode,ticket.value(),operator,ticket.expiresAtEpochMs());
    }

    @PostMapping("/{roomCode}/video-upload")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SPARRING_OPERATE.getCode())")
    public SparringPracticeUploadResponse createVideoUpload(@PathVariable String roomCode) {
        String objectPath=rooms.expectedVideoPath(roomCode);
        SupabaseStorageService.SignedUpload upload=storage.createPracticeVideoUploadUrl(objectPath);
        return new SparringPracticeUploadResponse(upload.url(),upload.objectPath(),524_288_000L);
    }

    /** Browser chỉ gọi sau PUT thành công vào Supabase; server khi đó mới công bố URL xem. */
    @PostMapping("/{roomCode}/video-upload-complete")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SPARRING_OPERATE.getCode())")
    public SparringPracticeRoomResponse completeVideoUpload(@PathVariable String roomCode, @RequestBody UploadComplete request) {
        if (!rooms.expectedVideoPath(roomCode).equals(request.objectPath())) throw new IllegalArgumentException("Unexpected video path");
        return rooms.videoUploaded(roomCode,request.objectPath(),storage.createPracticeVideoSignedUrl(request.objectPath()));
    }

    @PostMapping("/{roomCode}/close")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SPARRING_OPERATE.getCode())")
    public void close(@PathVariable String roomCode) {
        String objectPath = rooms.close(roomCode);
        if (objectPath != null) storage.deletePracticeVideo(objectPath);
    }

    public record UploadComplete(String objectPath) { }
}
