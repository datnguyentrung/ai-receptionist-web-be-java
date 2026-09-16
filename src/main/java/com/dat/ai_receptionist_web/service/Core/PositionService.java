package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Position;
import com.dat.ai_receptionist_web.dto.Core.PositionDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.mapper.Core.PositionMapper;
import com.dat.ai_receptionist_web.repository.Core.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PositionService {
    private final PositionRepository repository;
    private final PositionMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<PositionDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public PositionDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public PositionDTO.Response create(PositionDTO.CreateRequest request) {
        requireUniqueCode(request.code());
        Position position = mapper.toEntity(request);
        position.setCode(normalizeCode(request.code()));
        return mapper.toResponse(repository.save(position));
    }

    @Transactional
    public PositionDTO.Response update(UUID id, PositionDTO.UpdateRequest request) {
        Position position = find(id);
        String normalizedCode = normalizeCode(request.code());
        if (!position.getCode().equalsIgnoreCase(normalizedCode)) {
            requireUniqueCode(normalizedCode);
        }
        mapper.updateEntity(request, position);
        position.setCode(normalizedCode);
        return mapper.toResponse(repository.save(position));
    }

    @Transactional
    public void delete(UUID id) {
        Position position = find(id);
        position.setActive(false);
    }

    private Position find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(CoreErrorCode.POSITION_NOT_FOUND));
    }

    private void requireUniqueCode(String code) {
        if (repository.existsByCodeIgnoreCase(normalizeCode(code))) {
            throw new ApiException(CoreErrorCode.POSITION_CODE_ALREADY_EXISTS);
        }
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}
