package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.PermissionDTO;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Security.PermissionMapper;
import com.dat.ai_receptionist_web.repository.Security.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository repository;
    private final PermissionMapper mapper;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<PermissionDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "permissionList", key = "T(com.dat.ai_receptionist_web.util.CacheKeys).pageable(#pageable)", cacheManager = "redisCacheManager")
    public PageResponse<PermissionDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận Integer id từ caller hoặc request.
     * Output: Trả về PermissionDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PermissionDTO.Response get(Integer id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận PermissionDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về PermissionDTO.Response theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "permissionList", allEntries = true, cacheManager = "redisCacheManager")
    public PermissionDTO.Response create(PermissionDTO.CreateRequest request) {
        Permission entity = new Permission();
        entity.setCode(request.code());
        entity.setModel(request.model());
        entity.setAction(request.action());
        entity.setDescription(request.description());
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận Integer id, PermissionDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về PermissionDTO.Response theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "permissionList", allEntries = true, cacheManager = "redisCacheManager")
    public PermissionDTO.Response update(Integer id,
                                         PermissionDTO.UpdateRequest request) {
        var entity = find(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận Integer id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    @CacheEvict(value = "permissionList", allEntries = true, cacheManager = "redisCacheManager")
    public void delete(Integer id) {
        var entity = find(id);
        repository.delete(entity);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận Integer id từ caller hoặc request.
     * Output: Trả về Permission theo kết quả xử lý.
     */
    private Permission find(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(SecurityErrorCode.PERMISSION_NOT_FOUND));
    }
}


