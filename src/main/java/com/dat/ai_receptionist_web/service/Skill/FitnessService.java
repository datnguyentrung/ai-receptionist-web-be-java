package com.dat.ai_receptionist_web.service.Skill;

import com.dat.ai_receptionist_web.domain.Skill.Fitness;
import com.dat.ai_receptionist_web.dto.Skill.FitnessDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SkillErrorCode;
import com.dat.ai_receptionist_web.mapper.Skill.FitnessMapper;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FitnessService {
    private final FitnessRepository repository;
    private final FitnessMapper mapper;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<FitnessDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<FitnessDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận Long id từ caller hoặc request.
     * Output: Trả về FitnessDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public FitnessDTO.Response get(Long id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận FitnessDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về FitnessDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public FitnessDTO.Response create(FitnessDTO.CreateRequest request) {
        Fitness entity = new Fitness();
        entity.setScheduleLevel(request.scheduleLevel());
        entity.setAmount(request.amount());
        entity.setDuration(request.duration());
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận Long id, FitnessDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về FitnessDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public FitnessDTO.Response update(Long id, FitnessDTO.UpdateRequest request) {
        var entity = find(id);
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
     * Output: Trả về Fitness theo kết quả xử lý.
     */
    private Fitness find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(SkillErrorCode.FITNESS_NOT_FOUND));
    }
}


