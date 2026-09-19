package com.dat.ai_receptionist_web.service.Skill;

import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.SkillErrorCode;
import com.dat.ai_receptionist_web.mapper.Skill.FitnessRecordMapper;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FitnessRecordService {
    private final FitnessRecordRepository repository;
    private final FitnessRecordMapper mapper;
    private final PersonRepository personRepository;
    private final FitnessRepository fitnessRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<FitnessRecordDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<FitnessRecordDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAllDetailed(pageable), mapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận Long id từ caller hoặc request.
     * Output: Trả về FitnessRecordDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public FitnessRecordDTO.Response get(Long id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận FitnessRecordDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về FitnessRecordDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public FitnessRecordDTO.Response create(FitnessRecordDTO.CreateRequest request) {
        FitnessRecord entity = new FitnessRecord();
        entity.setStudent(personRepository.findById(request.studentId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setFitness(fitnessRepository.findById(request.fitnessId()).orElseThrow(() -> new ApiException(SkillErrorCode.FITNESS_NOT_FOUND)));
        entity.setRecordedByCoach(personRepository.findById(request.recordedByCoachId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setRecordDate(request.recordDate());
        entity.setDuration(request.duration());
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận Long id, FitnessRecordDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về FitnessRecordDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public FitnessRecordDTO.Response update(Long id, FitnessRecordDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setStudent(personRepository.findById(request.studentId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setFitness(fitnessRepository.findById(request.fitnessId()).orElseThrow(() -> new ApiException(SkillErrorCode.FITNESS_NOT_FOUND)));
        entity.setRecordedByCoach(personRepository.findById(request.recordedByCoachId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận Long id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(Long id) {
        var entity = find(id);
        repository.delete(entity);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận Long id từ caller hoặc request.
     * Output: Trả về FitnessRecord theo kết quả xử lý.
     */
    private FitnessRecord find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(SkillErrorCode.FITNESS_RECORD_NOT_FOUND));
    }
}


