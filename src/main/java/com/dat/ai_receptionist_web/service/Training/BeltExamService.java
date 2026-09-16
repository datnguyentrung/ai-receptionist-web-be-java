package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.BeltExam;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.BeltExamDTO;
import com.dat.ai_receptionist_web.enums.Training.BeltExamResult;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.BeltExamMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Training.BeltExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeltExamService {
    private final BeltExamRepository repository;
    private final BeltExamMapper mapper;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<BeltExamDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public BeltExamDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public BeltExamDTO.Response create(BeltExamDTO.CreateRequest request) {
        var person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        BeltExam entity = new BeltExam();
        entity.setPerson(person);
        entity.setCreatedByUser(userRepository.findById(request.createdByUserId())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        entity.setFromBelt(request.fromBelt());
        entity.setTargetBelt(request.targetBelt());
        entity.setYear(request.year());
        entity.setQuarter(request.quarter());
        entity.setExamDate(request.examDate());
        entity.setResult(request.result() == null ? BeltExamResult.PENDING : request.result());
        entity.setNote(request.note());
        entity.setType(request.type());
        applyPassedBelt(entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public BeltExamDTO.Response update(UUID id, BeltExamDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setPerson(personRepository.findById(request.personId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setCreatedByUser(userRepository.findById(request.createdByUserId())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        mapper.updateEntity(request, entity);
        applyPassedBelt(entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private BeltExam find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.BELT_EXAM_NOT_FOUND));
    }

    private void applyPassedBelt(BeltExam entity) {
        if (entity.getResult() == BeltExamResult.PASSED) {
            entity.getPerson().setCurrentBelt(entity.getTargetBelt());
        }
    }
}
