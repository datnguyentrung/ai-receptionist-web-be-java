package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CoursePrice;
import com.dat.ai_receptionist_web.dto.Catalog.CoursePriceDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.mapper.Catalog.CoursePriceMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CoursePriceRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.enums.Catalog.CoursePriceStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoursePriceService {
    private final CoursePriceRepository repository;
    private final CoursePriceMapper mapper;
    private final CourseRepository courseRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<CoursePriceDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<CoursePriceDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CoursePriceDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public CoursePriceDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận CoursePriceDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về CoursePriceDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public CoursePriceDTO.Response create(CoursePriceDTO.CreateRequest request) {
        CoursePrice entity = new CoursePrice();
        entity.setCourse(courseRepository.findById(request.courseId()).orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND)));
        entity.setDurationMonths(request.durationMonths());
        entity.setSessionCount(request.sessionCount());
        entity.setBasePrice(request.basePrice());
        entity.setFinalPrice(request.finalPrice());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, CoursePriceDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về CoursePriceDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public CoursePriceDTO.Response update(UUID id, CoursePriceDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setCourse(courseRepository.findById(request.courseId()).orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND)));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(CoursePriceStatus.INACTIVE);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CoursePrice theo kết quả xử lý.
     */
    private CoursePrice find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_PRICE_NOT_FOUND));
    }
}


