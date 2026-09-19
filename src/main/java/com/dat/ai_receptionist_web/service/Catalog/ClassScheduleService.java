package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.ClassScheduleDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.mapper.Catalog.ClassScheduleMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Core.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassScheduleService {
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassScheduleMapper classScheduleMapper;
    private final BranchRepository branchRepository;
    private final CourseRepository courseRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<ClassScheduleDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<ClassScheduleDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(classScheduleRepository.findAllDetailed(pageable), classScheduleMapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về ClassScheduleDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public ClassScheduleDTO.Response get(UUID id) {
        return classScheduleMapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận ClassScheduleDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về ClassScheduleDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public ClassScheduleDTO.Response create(ClassScheduleDTO.CreateRequest request) {
        ClassSchedule classSchedule = new ClassSchedule();
        classSchedule.setBranch(branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.BRANCH_NOT_FOUND)));
        classSchedule.setWeekday(request.weekday());
        classSchedule.setLevel(request.level());
        classSchedule.setLocation(request.location());
        classSchedule.setStatus(request.status());
        classSchedule.setStartTime(request.startTime());
        classSchedule.setEndTime(request.endTime());
        return classScheduleMapper.toResponse(classScheduleRepository.save(classSchedule));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, ClassScheduleDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về ClassScheduleDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public ClassScheduleDTO.Response update(UUID id, ClassScheduleDTO.UpdateRequest request) {
        ClassSchedule classSchedule = find(id);
        requireNotReferenced(id);
        classSchedule.setBranch(branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.BRANCH_NOT_FOUND)));
        classScheduleMapper.updateEntity(request, classSchedule);
        return classScheduleMapper.toResponse(classScheduleRepository.save(classSchedule));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        ClassSchedule classSchedule = find(id);
        requireNotReferenced(id);
        classSchedule.setStatus(ScheduleStatus.INACTIVE);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về ClassSchedule theo kết quả xử lý.
     */
    private ClassSchedule find(UUID id) {
        return classScheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND));
    }

    private void requireNotReferenced(UUID scheduleId) {
        long referenced = courseRepository.countByClassSchedule_ScheduleIdAndStatusNot(
                        scheduleId, CourseStatus.CANCELLED)
                + courseRepository.countByNextClassSchedule_ScheduleId(scheduleId);
        if (referenced > 0) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT,
                    "Class schedule is used by a course; change it through the course schedule API");
        }
    }
}


