package com.dat.ai_receptionist_web.service.Notification;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationRecipientEligibilityPolicy {
    public EligibilityResult decide(Request request) {
        if (request.targetSource() == TargetSource.PERSON) {
            return EligibilityResult.UNSUPPORTED;
        }
        return EligibilityResult.ALLOW;
    }

    public enum EligibilityResult {
        ALLOW,
        DENY,
        UNSUPPORTED
    }

    public enum TargetSource {
        DIRECT_USER,
        PERSON,
        ROLE
    }

    public enum AudienceContext {
        ACCOUNT,
        PERSON
    }

    public record Request(NotificationType notificationType, String referenceType, String referenceId,
                          TargetSource targetSource, RelationshipType relationshipType,
                          UserPerson candidateUserPerson, AudienceContext audienceContext) {
    }

    public record ResolvedRecipientTarget(UUID recipientUserId, UUID contextPersonId,
                                          TargetSource source, AudienceContext audienceContext) {
    }
}
