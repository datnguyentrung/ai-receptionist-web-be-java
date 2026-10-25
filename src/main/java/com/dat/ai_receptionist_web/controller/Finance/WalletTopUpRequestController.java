package com.dat.ai_receptionist_web.controller.Finance;

import com.dat.ai_receptionist_web.dto.Finance.WalletTopUpRequestDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Finance.WalletTopUpRequestStatus;
import com.dat.ai_receptionist_web.service.Finance.WalletTopUpRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet-top-up-requests")
@RequiredArgsConstructor
public class WalletTopUpRequestController {
    private static final String REQUEST_CREATE = "hasAuthority('WALLET_TOP_UP_REQUEST_CREATE')";
    private static final String DIRECT_CREATE = "hasAuthority('WALLET_TOP_UP_DIRECT_CREATE')";
    private static final String REQUEST_READ = "hasAuthority('WALLET_TOP_UP_REQUEST_READ')";
    private static final String REVIEW = "hasAuthority('WALLET_TOP_UP_REVIEW')";

    private final WalletTopUpRequestService service;

    @PostMapping
    @PreAuthorize(REQUEST_CREATE)
    public WalletTopUpRequestDTO.Response createGuardian(
            @Valid @RequestBody WalletTopUpRequestDTO.CreateRequest request) {
        return service.createGuardian(request);
    }

    @PostMapping("/direct")
    @PreAuthorize(DIRECT_CREATE)
    public WalletTopUpRequestDTO.Response createDirect(
            @Valid @RequestBody WalletTopUpRequestDTO.CreateRequest request) {
        return service.createDirect(request);
    }

    @PutMapping(value = "/{id}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(REQUEST_CREATE + " or " + DIRECT_CREATE)
    public WalletTopUpRequestDTO.ProofResponse uploadProof(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        return service.uploadProof(id, file);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize(REQUEST_CREATE + " or " + DIRECT_CREATE)
    public WalletTopUpRequestDTO.Response submit(
            @PathVariable UUID id,
            @Valid @RequestBody WalletTopUpRequestDTO.SubmitRequest request) {
        return service.submit(id, request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(REVIEW)
    public WalletTopUpRequestDTO.Response approve(
            @PathVariable UUID id,
            @Valid @RequestBody WalletTopUpRequestDTO.SubmitRequest request) {
        return service.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(REVIEW)
    public WalletTopUpRequestDTO.Response reject(
            @PathVariable UUID id,
            @Valid @RequestBody WalletTopUpRequestDTO.RejectRequest request) {
        return service.reject(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(REQUEST_READ)
    public WalletTopUpRequestDTO.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-client-request/{clientRequestId}")
    @PreAuthorize(REQUEST_READ)
    public WalletTopUpRequestDTO.Response getByClientRequestId(@PathVariable UUID clientRequestId) {
        return service.getByClientRequestId(clientRequestId);
    }

    @GetMapping("/mine")
    @PreAuthorize(REQUEST_READ)
    public PageResponse<WalletTopUpRequestDTO.Response> listMine(Pageable pageable) {
        return service.listMine(pageable);
    }

    @GetMapping("/review")
    @PreAuthorize(REVIEW)
    public PageResponse<WalletTopUpRequestDTO.Response> listForReview(
            @RequestParam(required = false) WalletTopUpRequestStatus status,
            Pageable pageable) {
        return service.listForReview(status, pageable);
    }

    @GetMapping("/{id}/proof-url")
    @PreAuthorize(REQUEST_READ)
    public WalletTopUpRequestDTO.ProofUrlResponse proofUrl(@PathVariable UUID id) {
        return service.proofUrl(id);
    }
}

