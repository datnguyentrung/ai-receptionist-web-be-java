package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.service.Core.PersonService;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaceIdentificationService {
    private final PersonService personService;

    public IdentifiedPerson identify(MultipartFile file) {
        var embeddingResponse = personService.generateFaceEmbedding(file);
        PersonService.NearestPersonMatch nearestPerson =
                personService.findNearestPersonByEmbedding(embeddingResponse.embedding());
        if (nearestPerson.personId() == null) {
            throw new ApiException(CoreErrorCode.FACE_NOT_MATCHED);
        }
        return new IdentifiedPerson(nearestPerson.personId(), nearestPerson.confidence());
    }

    public record IdentifiedPerson(UUID personId, float confidence) {
    }
}
