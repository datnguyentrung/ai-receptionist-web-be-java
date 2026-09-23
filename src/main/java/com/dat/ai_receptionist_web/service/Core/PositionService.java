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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionService {
    private final PositionRepository repository;
    private final PositionMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<PositionDTO.SimpleResponse> list(Pageable pageable) {
        var positions = repository.findAll(pageable);
        Map<UUID, Long> personCounts = getPersonCounts(positions.getContent().stream()
                .map(Position::getPositionId)
                .collect(Collectors.toSet()));
        return PageResponse.of(positions, position -> mapper.toSimpleResponse(
                position,
                personCounts.getOrDefault(position.getPositionId(), 0L)));
    }

    @Transactional(readOnly = true)
    public PositionDTO.Response get(UUID id) {
        Position position = find(id);
        return mapper.toResponse(position, repository.countPersonsByPositionId(position.getPositionId()));
    }

    @Transactional
    public PositionDTO.Response create(PositionDTO.CreateRequest request) {
        requireUniqueCode(request.code());
        Position position = mapper.toEntity(request);
        position.setCode(normalizeCode(request.code()));
        return mapper.toResponse(repository.save(position), 0L);
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
        Position saved = repository.save(position);
        return mapper.toResponse(saved, repository.countPersonsByPositionId(saved.getPositionId()));
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

    private Map<UUID, Long> getPersonCounts(Set<UUID> positionIds) {
        if (positionIds.isEmpty()) {
            return Map.of();
        }
        return repository.countPersonsByPositionIds(positionIds).stream()
                .collect(Collectors.toMap(
                        PositionRepository.PersonCountByPosition::getPositionId,
                        PositionRepository.PersonCountByPosition::getPersonCount));
    }
}
