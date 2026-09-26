package com.dat.ai_receptionist_web.controller.Core;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Core.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {
    private final PersonService service;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<PersonDTO.SimpleResponse> theo kết quả xử lý.
     */
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_READ.getCode())")
    public PageResponse<PersonDTO.SimpleResponse> list(@RequestParam(required = false) UUID positionId,
                                                       Pageable pageable) {
        return service.list(positionId, pageable);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_READ.getCode())")
    public PersonDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận PersonDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_CREATE.getCode())")
    public PersonDTO.Response create(@Valid @RequestBody PersonDTO.CreateRequest request) { return service.create(request); }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, PersonDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_UPDATE.getCode())")
    public PersonDTO.Response update(@PathVariable UUID id, @Valid @RequestBody PersonDTO.UpdateRequest request) { return service.update(id, request); }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_DELETE.getCode())")
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @PostMapping(value = "/identify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_READ.getCode())")
    public PersonDTO.Response identify(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "personCode", required = false) String personCode
    ) {
        return service.identifyPerson(file, personCode);
    }

    @PatchMapping(value = "/{personId}/face-embedding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_UPDATE.getCode())")
    public PersonDTO.FaceEmbeddingUpdateResponse updateFaceEmbedding(
            @PathVariable UUID personId,
            @RequestPart("file") MultipartFile file
    ) {
        return service.updateFaceEmbedding(file, personId);
    }

    @GetMapping("/{personId}/face-image-url")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_READ.getCode())")
    public PersonDTO.FaceImageUrlResponse getFaceImageUrl(@PathVariable UUID personId) {
        return service.getFaceImageUrl(personId);
    }

    @DeleteMapping("/{personId}/face-embedding")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_UPDATE.getCode())")
    public void deleteFaceEmbedding(@PathVariable UUID personId) {
        service.deleteFaceEmbedding(personId);
    }
}


