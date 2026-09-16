package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.dto.Core.UserPersonDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Core.UserPersonMapper;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPersonService {
    private final UserPersonRepository repository;
    private final UserPersonMapper mapper;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<UserPersonDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserPersonDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về UserPersonDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public UserPersonDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận UserPersonDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về UserPersonDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public UserPersonDTO.Response create(UserPersonDTO.CreateRequest request) {
        UserPerson entity = new UserPerson();
        entity.setUser(userRepository.findById(request.userId()).orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        entity.setPerson(personRepository.findById(request.personId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setRelationshipType(request.relationshipType());
        entity.setActive(request.active());
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, UserPersonDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về UserPersonDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public UserPersonDTO.Response update(UUID id, UserPersonDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setUser(userRepository.findById(request.userId()).orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        entity.setPerson(personRepository.findById(request.personId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
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
        entity.setActive(false);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về UserPerson theo kết quả xử lý.
     */
    private UserPerson find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(CoreErrorCode.USER_PERSON_NOT_FOUND));
    }
}


