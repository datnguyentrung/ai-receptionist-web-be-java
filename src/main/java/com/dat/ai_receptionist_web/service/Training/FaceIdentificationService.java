package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.service.Core.PersonService;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaceIdentificationService {
    private final PersonService personService;

    public IdentifiedPerson identify(MultipartFile file){
        return identify(file, null);
    }

    public IdentifiedPerson identify(MultipartFile file, List<Float> embeddingFace) {
        var embeddingResponse = personService.generateFaceEmbedding(file);
        PersonService.NearestPersonMatch nearestPerson =
                personService.findNearestPersonByEmbedding(
                        embeddingFace != null ? embeddingFace : embeddingResponse.embedding()
                );
        if (nearestPerson.personId() == null) {
            throw new ApiException(CoreErrorCode.FACE_NOT_MATCHED);
        }
        return new IdentifiedPerson(nearestPerson.personId(), nearestPerson.confidence());
    }

    public record IdentifiedPerson(UUID personId, float confidence) {
    }
}
