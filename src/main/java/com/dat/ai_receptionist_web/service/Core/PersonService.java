package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.client.PythonBackendClient;
import com.dat.ai_receptionist_web.client.PythonBackendClient.PythonBackendClientException;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.Position;
import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import com.dat.ai_receptionist_web.enums.Finance.WalletStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.PositionRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import com.dat.ai_receptionist_web.util.AccountUtil;
import com.dat.ai_receptionist_web.util.converter.NameConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonService {
    private final PersonRepository personRepository;
    private final WalletRepository walletRepository;
    private final PersonMapper personMapper;
    private final PersonCodePolicy personCodePolicy;
    private final PositionRepository positionRepository;
    private final PythonBackendClient pythonBackendClient;
    private final SupabaseStorageService supabaseStorageService;
    private final PersonAvatarUrlCacheService avatarUrlCacheService;

    @Value("${face.match-threshold:${FACE_MATCH_THRESHOLD:0.70}}")
    private float faceMatchThreshold = 0.70f;

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận PersonDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public PersonDTO.Response create(PersonDTO.CreateRequest request) {
        if (request.nationalCode() != null && personRepository.existsByNationalCode(request.nationalCode())) {
            throw new ApiException(
                    CoreErrorCode.NATIONAL_CODE_ALREADY_EXISTS);
        }
        if (personRepository.existsByPersonCode(AccountUtil.getUserCode(request.fullName(), request.birthDate(), "VQ"))) {
            throw new ApiException(
                    CoreErrorCode.PERSON_CODE_ALREADY_EXISTS);
        }
        Position position = resolvePosition(request.positionId());
        Person person = personMapper.toEntity(request);
        person.setFullName(NameConverter.formatVietnameseName(request.fullName()));
        person.setPersonCode(AccountUtil.getUserCode(request.fullName(), request.birthDate(), "VQ"));
        person.setPosition(position);
        person = personRepository.save(person);
        walletRepository.save(Wallet.builder()
                .person(person)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build());
        return personMapper.toResponse(person);
    }

    /**
     * Tác dụng: Thực hiện logic search của lớp hiện tại.
     * Input: Nhận String query, Pageable pageable từ caller hoặc request.
     * Output: Trả về Page<PersonDTO.Response> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public Page<PersonDTO.Response> search(String query, Pageable pageable) {
        Page<Person> people = query == null || query.isBlank()
                ? personRepository.findAll(pageable)
                : personRepository.findByFullNameContainingIgnoreCaseOrPersonCodeContainingIgnoreCase(
                        query.trim(), query.trim(), pageable);
        return people.map(personMapper::toResponse);
    }

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<PersonDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<PersonDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(personRepository.findAll(pageable), personMapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<PersonDTO.SimpleResponse> list(UUID positionId, Pageable pageable) {
        if (positionId == null) {
            return list(pageable);
        }
        return PageResponse.of(personRepository.findByPosition_PositionId(positionId, pageable),
                personMapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PersonDTO.Response get(UUID id) {
        return personMapper.toResponse(personRepository.findById(id)
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, PersonDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public PersonDTO.Response update(UUID id, PersonDTO.UpdateRequest request) {
        Person person = find(id);
        if (request.personCode() != null && !request.personCode().isBlank()) {
            personCodePolicy.validateFormat(request.personCode());
        }
        person.setPosition(resolvePosition(request.positionId()));
        personMapper.updateEntity(request, person);
        return personMapper.toResponse(personRepository.save(person));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        Person person = find(id);
        person.setStatus(PersonStatus.INACTIVE);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về Person theo kết quả xử lý.
     */
    private Person find(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
    }

    private Position resolvePosition(UUID positionId) {
        if (positionId == null) {
            return null;
        }
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new ApiException(CoreErrorCode.POSITION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PersonDTO.Response identifyPerson(MultipartFile file, String personCode) {
        if (file != null && !file.isEmpty()) {
            PythonBackendClient.FaceEmbeddingResponse embeddingResponse = generateFaceEmbedding(file);
            NearestPersonMatch nearestPerson = findNearestPersonByEmbedding(embeddingResponse.embedding());
            if (nearestPerson.personId() == null) {
                throw new ApiException(CoreErrorCode.FACE_NOT_MATCHED);
            }
            return personMapper.toResponse(find(nearestPerson.personId()));
        }

        if (!StringUtils.hasText(personCode)) {
            throw new ApiException(CoreErrorCode.INVALID_IDENTIFICATION_REQUEST);
        }
        List<UUID> personIds = personRepository.findPersonIdsByPersonCode(personCode.trim());
        if (personIds.isEmpty()) {
            throw new ApiException(CoreErrorCode.PERSON_NOT_FOUND);
        }
        if (personIds.size() > 1) {
            throw new ApiException(CoreErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID);
        }
        return personMapper.toResponse(find(personIds.getFirst()));
    }

    @Transactional
    public PersonDTO.FaceEmbeddingUpdateResponse updateFaceEmbedding(MultipartFile file, UUID personId) {
        Person person = find(personId);
        SupabaseStorageService.ValidatedImage image = supabaseStorageService.validateImage(file);
        PythonBackendClient.FaceEmbeddingResponse response = requestFaceEmbedding(file);
        float[] embedding = toEmbeddingArray(response);
        String oldPath = person.getFaceImagePath();
        String uploadedPath = null;
        try {
            uploadedPath = supabaseStorageService.uploadPersonFaceImage(personId, image);
            person.setFaceEmbedding(embedding);
            person.setFaceImagePath(uploadedPath);
            Person savedPerson = personRepository.saveAndFlush(person);
            registerStorageCompensation(savedPerson.getPersonId(), uploadedPath, oldPath);
            String avatarUrl = supabaseStorageService.getPublicUrl(savedPerson.getFaceImagePath());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    avatarUrlCacheService.put(savedPerson.getPersonId(), avatarUrl);
                }
            });
            return new PersonDTO.FaceEmbeddingUpdateResponse(
                    savedPerson.getPersonId(),
                    response.dimension(),
                    response.model(),
                    savedPerson.getFaceImagePath(),
                    avatarUrl,
                    savedPerson.getUpdatedAt()
            );
        } catch (RuntimeException exception) {
            if (uploadedPath != null) {
                cleanupObjectAfterFailure(personId, uploadedPath, "immediate rollback");
            }
            throw exception;
        }
    }

    @Transactional
    public void deleteFaceEmbedding(UUID personId) {
        Person person = find(personId);
        String oldPath = person.getFaceImagePath();
        person.setFaceEmbedding(null);
        person.setFaceImagePath(null);
        personRepository.saveAndFlush(person);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                avatarUrlCacheService.remove(personId);
                cleanupObjectAfterFailure(personId, oldPath, "delete after commit");
            }
        });
    }

    @Transactional(readOnly = true)
    public PersonDTO.FaceImageUrlResponse getFaceImageUrl(UUID personId) {
        Person person = find(personId);
        Map<UUID, String> cached = avatarUrlCacheService.getMany(List.of(personId));
        String avatarUrl = cached.get(personId);
        if (!StringUtils.hasText(avatarUrl)) {
            avatarUrl = supabaseStorageService.getPublicUrl(person.getFaceImagePath());
            avatarUrlCacheService.put(personId, avatarUrl);
        }
        return new PersonDTO.FaceImageUrlResponse(avatarUrl);
    }

    public PythonBackendClient.FaceEmbeddingResponse generateFaceEmbedding(MultipartFile file) {
        supabaseStorageService.validateImage(file);
        return requestFaceEmbedding(file);
    }

    private PythonBackendClient.FaceEmbeddingResponse requestFaceEmbedding(MultipartFile file) {
        try {
            return pythonBackendClient.generateFaceEmbedding(file);
        } catch (PythonBackendClientException exception) {
            throw new ApiException(resolvePythonBackendErrorCode(exception));
        }
    }

    public NearestPersonMatch findNearestPersonByEmbedding(List<Float> embeddingVector) {
        return findNearestPersonByEmbedding(embeddingVector, faceMatchThreshold);
    }

    public NearestPersonMatch findNearestPersonByEmbedding(List<Float> embeddingVector, float threshold) {
        if (embeddingVector == null || embeddingVector.isEmpty()) {
            return new NearestPersonMatch(null, 0.0f);
        }
        List<PersonRepository.NearestFaceMatchProjection> matches =
                personRepository.findNearestFaceMatch(toPgVectorLiteral(embeddingVector));
        if (matches.isEmpty()) {
            return new NearestPersonMatch(null, 0.0f);
        }
        PersonRepository.NearestFaceMatchProjection match = matches.getFirst();
        float distance = match.getDistance() == null ? 1.0f : match.getDistance().floatValue();
        float confidence = Math.max(0.0f, Math.min(1.0f, 1.0f - distance));
        return confidence < threshold
                ? new NearestPersonMatch(null, confidence)
                : new NearestPersonMatch(match.getPersonId(), confidence);
    }

    private CoreErrorCode resolvePythonBackendErrorCode(PythonBackendClientException exception) {
        CoreErrorCode backendError = resolveFaceEmbeddingErrorCode(exception.getBackendErrorCode());
        if (backendError != null) {
            return backendError;
        }
        return exception.getFailureType().isUnavailable()
                ? CoreErrorCode.PYTHON_BACKEND_UNAVAILABLE
                : CoreErrorCode.PYTHON_BACKEND_ERROR;
    }

    private static CoreErrorCode resolveFaceEmbeddingErrorCode(String backendErrorCode) {
        if (!StringUtils.hasText(backendErrorCode)) {
            return null;
        }
        try {
            return switch (CoreErrorCode.valueOf(backendErrorCode.trim().toUpperCase(Locale.ROOT))) {
                case INVALID_IMAGE_FILE, EMPTY_IMAGE_FILE, FILE_TOO_LARGE, UNSUPPORTED_IMAGE_TYPE,
                     IMAGE_DECODE_FAILED, FACE_NOT_DETECTED, MULTIPLE_FACES_DETECTED,
                     FACE_EMBEDDING_FAILED, INVALID_EMBEDDING, MODEL_NOT_INITIALIZED ->
                        CoreErrorCode.valueOf(backendErrorCode.trim().toUpperCase(Locale.ROOT));
                default -> null;
            };
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static float[] toEmbeddingArray(PythonBackendClient.FaceEmbeddingResponse response) {
        if (response.dimension() == null || response.dimension() != 512
                || response.embedding() == null || response.embedding().size() != 512) {
            throw new ApiException(CoreErrorCode.INVALID_EMBEDDING);
        }
        float[] embedding = new float[512];
        for (int index = 0; index < embedding.length; index++) {
            Float value = response.embedding().get(index);
            if (value == null || !Float.isFinite(value)) {
                throw new ApiException(CoreErrorCode.INVALID_EMBEDDING);
            }
            embedding[index] = value;
        }
        return embedding;
    }

    private static String toPgVectorLiteral(List<Float> embeddingVector) {
        StringBuilder vector = new StringBuilder("[");
        for (int index = 0; index < embeddingVector.size(); index++) {
            Float value = embeddingVector.get(index);
            if (value == null || !Float.isFinite(value)) {
                throw new ApiException(CoreErrorCode.INVALID_EMBEDDING);
            }
            if (index > 0) {
                vector.append(',');
            }
            vector.append(value);
        }
        return vector.append(']').toString();
    }

    public record NearestPersonMatch(UUID personId, float confidence) {
    }

    private void registerStorageCompensation(UUID personId, String uploadedPath, String oldPath) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (oldPath != null && !oldPath.equals(uploadedPath)) {
                    cleanupObjectAfterFailure(personId, oldPath, "replace after commit");
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    cleanupObjectAfterFailure(personId, uploadedPath, "transaction rollback");
                }
            }
        });
    }

    private void cleanupObjectAfterFailure(UUID personId, String objectPath, String stage) {
        if (!StringUtils.hasText(objectPath)) {
            return;
        }
        try {
            supabaseStorageService.deleteObject(objectPath);
        } catch (RuntimeException cleanupException) {
            log.error("Face-image cleanup failed: personId={}, objectPath={}, stage={}, exceptionType={}",
                    personId, objectPath, stage, cleanupException.getClass().getSimpleName(), cleanupException);
        }
    }
}


