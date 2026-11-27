package com.dat.ai_receptionist_web.controller.Knowledge;

import com.dat.ai_receptionist_web.client.PythonAiGatewayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Authenticated multi-tenant gateway for Knowledge Governance (Forms, RichText, Document, FAQ, Review/Publish).
 */
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeGatewayController {

    private final PythonAiGatewayClient gatewayClient;

    @PostMapping("/forms/{formType}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<String> submitForm(
            @PathVariable String formType,
            @RequestBody Map<String, Object> payload
    ) {
        return gatewayClient.forwardJson(HttpMethod.POST, "/api/v1/knowledge/forms/" + formType, payload);
    }

    @GetMapping("/forms/{formType}/published")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAnyAuthority('KNOWLEDGE_READ','KNOWLEDGE_WRITE','KNOWLEDGE_REVIEW','KNOWLEDGE_PUBLISH')")
    public ResponseEntity<String> getLatestPublishedForm(@PathVariable String formType) {
        return gatewayClient.forwardJson(HttpMethod.GET, "/api/v1/knowledge/forms/" + formType + "/published", null);
    }

    @PostMapping("/rich-text")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<String> submitRichText(@RequestBody Map<String, Object> payload) {
        return gatewayClient.forwardJson(HttpMethod.POST, "/api/v1/knowledge/rich-text", payload);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("scope_key") String scopeKey,
            @RequestParam(value = "title", defaultValue = "") String title,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return gatewayClient.forwardDocument("/api/v1/knowledge/documents", scopeKey, title, file);
    }

    @PostMapping("/faq")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<String> submitFaq(@RequestBody Map<String, Object> payload) {
        return gatewayClient.forwardJson(HttpMethod.POST, "/api/v1/knowledge/faq", payload);
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAnyAuthority('KNOWLEDGE_READ','KNOWLEDGE_REVIEW','KNOWLEDGE_PUBLISH')")
    public ResponseEntity<String> listItems(@RequestParam(value = "status", required = false) String status) {
        String path = status == null || status.isBlank()
                ? "/api/v1/knowledge/items"
                : "/api/v1/knowledge/items?status=" + status;
        return gatewayClient.forwardJson(HttpMethod.GET, path, null);
    }

    @GetMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAnyAuthority('KNOWLEDGE_READ','KNOWLEDGE_REVIEW','KNOWLEDGE_PUBLISH')")
    public ResponseEntity<String> getItem(@PathVariable String itemId) {
        return gatewayClient.forwardJson(HttpMethod.GET, "/api/v1/knowledge/items/" + itemId, null);
    }

    @PostMapping("/items/{itemId}/submit")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<String> submitItem(@PathVariable String itemId) {
        return gatewayClient.forwardJson(HttpMethod.POST, "/api/v1/knowledge/items/" + itemId + "/submit", null);
    }

    @PostMapping("/items/{itemId}/reject")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_REVIEW')")
    public ResponseEntity<String> rejectItem(
            @PathVariable String itemId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return gatewayClient.forwardJson(HttpMethod.POST, "/api/v1/knowledge/items/" + itemId + "/reject", body);
    }

    @PostMapping("/items/{itemId}/publish")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_PUBLISH')")
    public ResponseEntity<String> publishItem(@PathVariable String itemId) {
        return gatewayClient.forwardJson(HttpMethod.POST, "/api/v1/knowledge/items/" + itemId + "/publish", null);
    }

    @PutMapping("/items/{itemId}/form")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<String> updateFormItem(
            @PathVariable String itemId,
            @RequestBody Map<String, Object> payload
    ) {
        return gatewayClient.forwardJson(HttpMethod.PUT, "/api/v1/knowledge/items/" + itemId + "/form", payload);
    }

    @PostMapping("/items/{itemId}/retry")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<String> retryFormItem(@PathVariable String itemId) {
        return gatewayClient.forwardJson(HttpMethod.POST, "/api/v1/knowledge/items/" + itemId + "/retry", null);
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_PUBLISH')")
    public ResponseEntity<String> deleteItem(@PathVariable String itemId) {
        return gatewayClient.forwardJson(HttpMethod.DELETE, "/api/v1/knowledge/items/" + itemId, null);
    }

    @PostMapping("/proposals")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','OWNER') or hasAuthority('KNOWLEDGE_WRITE')")
    public ResponseEntity<String> createProposal(@RequestBody Map<String, Object> body) {
        return gatewayClient.forwardJson(HttpMethod.POST, "/api/v1/knowledge/proposals", body);
    }
}
