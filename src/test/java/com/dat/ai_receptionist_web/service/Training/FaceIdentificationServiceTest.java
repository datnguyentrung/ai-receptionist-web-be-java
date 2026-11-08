package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.service.Core.PersonService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FaceIdentificationServiceTest {
    private final PersonService personService = mock(PersonService.class);
    private final FaceIdentificationService service = new FaceIdentificationService(personService);

    @Test
    void providedEmbeddingNeverCallsPythonEmbeddingGeneration() {
        UUID personId = UUID.randomUUID();
        List<Float> embedding = Collections.nCopies(512, 0.125f);
        when(personService.findNearestPersonByEmbedding(embedding))
                .thenReturn(new PersonService.NearestPersonMatch(personId, 0.91f));

        FaceIdentificationService.IdentifiedPerson result = service.identify(null, embedding);

        assertThat(result.personId()).isEqualTo(personId);
        assertThat(result.confidence()).isEqualTo(0.91f);
        verify(personService, never()).generateFaceEmbedding(null);
    }
}
