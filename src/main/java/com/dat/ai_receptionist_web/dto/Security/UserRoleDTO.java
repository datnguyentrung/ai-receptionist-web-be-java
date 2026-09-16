package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.*;
import java.util.*;

public class UserRoleDTO {
    public record CreateRequest(@NotNull UUID userId, @NotBlank String roleCode) {}
    public record UpdateRequest(@NotNull UUID userId, @NotBlank String roleCode) {}
    public record AssignRequest(@NotNull UUID userId, @NotBlank String roleCode) {
        public UUID getUserId() { return userId; }
        public String getRoleCode() { return roleCode; }
    }
    public static class ReplaceRequest {
        @NotEmpty private Set<@NotBlank String> roleCodes;
        public Set<String> getRoleCodes() { return roleCodes; }
        public void setRoleCodes(Set<String> roleCodes) { this.roleCodes = roleCodes; }
    }
    public record Response(UUID userId, Set<String> roleCodes) {}
    public record ItemResponse(UUID userId, String roleCode) {}
    public record SimpleResponse(UUID userId, String roleCode) {}
}
