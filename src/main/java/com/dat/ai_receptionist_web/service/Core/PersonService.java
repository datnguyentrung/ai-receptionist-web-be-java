package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.Position;
import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import com.dat.ai_receptionist_web.enums.Finance.WalletStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.PositionRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import com.dat.ai_receptionist_web.util.AccountUtil;
import com.dat.ai_receptionist_web.util.converter.NameConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;
    private final WalletRepository walletRepository;
    private final PersonMapper personMapper;
    private final PersonCodePolicy personCodePolicy;
    private final PositionRepository positionRepository;

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận PersonDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public PersonDTO.Response create(PersonDTO.CreateRequest request) {
        if (request.nationalCode() != null && personRepository.existsByNationalCode(request.nationalCode())) {
            throw new ApiException(
                    CoreErrorCode.NATIONAL_CODE_ALREADY_EXISTS);
        }
        if (personRepository.existsByPersonCode(AccountUtil.getUserCode(request.fullName(), request.birthDate(), "VQ"))) {
            throw new ApiException(
                    CoreErrorCode.PERSON_CODE_ALREADY_EXISTS);
        }
        Position position = resolvePosition(request.positionId());
        Person person = personRepository.save(Person.builder()
                .fullName(NameConverter.formatVietnameseName(request.fullName()))
                .gender(request.gender())
                .birthDate(request.birthDate())
                .email(request.email())
                .nationalCode(request.nationalCode())
                .personCode(AccountUtil.getUserCode(request.fullName(), request.birthDate(), "VQ"))
                .currentBelt(request.currentBelt())
                .status(request.status())
                .startDate(request.startDate())
                .position(position)
                .build());
        walletRepository.save(Wallet.builder()
                .person(person)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build());
        return toResponse(person);
    }

    /**
     * Tác dụng: Thực hiện logic search của lớp hiện tại.
     * Input: Nhận String query, Pageable pageable từ caller hoặc request.
     * Output: Trả về Page<PersonDTO.Response> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public Page<PersonDTO.Response> search(String query, Pageable pageable) {
        Page<Person> people = query == null || query.isBlank()
                ? personRepository.findAll(pageable)
                : personRepository.findByFullNameContainingIgnoreCaseOrPersonCodeContainingIgnoreCase(
                        query.trim(), query.trim(), pageable);
        return people.map(this::toResponse);
    }

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<PersonDTO.Response> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<PersonDTO.Response> list(Pageable pageable) {
        return PageResponse.of(personRepository.findAll(pageable), personMapper::toResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PersonDTO.Response get(UUID id) {
        return toResponse(personRepository.findById(id)
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, PersonDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public PersonDTO.Response update(UUID id, PersonDTO.UpdateRequest request) {
        Person person = find(id);
        if (request.personCode() != null && !request.personCode().isBlank()) {
            personCodePolicy.validateFormat(request.personCode());
        }
        person.setPosition(resolvePosition(request.positionId()));
        personMapper.updateEntity(request, person);
        return personMapper.toResponse(personRepository.save(person));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        Person person = find(id);
        person.setStatus(PersonStatus.INACTIVE);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về Person theo kết quả xử lý.
     */
    private Person find(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
    }

    /**
     * Tác dụng: Chuyển đổi dữ liệu sang kiểu kết quả phù hợp cho lớp đang xử lý.
     * Input: Nhận Person value từ caller hoặc request.
     * Output: Trả về PersonDTO.Response theo kết quả xử lý.
     */
    private PersonDTO.Response toResponse(Person value) {
        return new PersonDTO.Response(value.getPersonId(), value.getFullName(), value.getGender(),
                value.getBirthDate(), value.getEmail(), value.getNationalCode(), value.getPersonCode(),
                value.getCurrentBelt(), value.getStatus(), value.getStartDate(),
                value.getPosition() == null ? null : value.getPosition().getPositionId(),
                value.getFaceImagePath(),
                value.getCreatedAt(), value.getUpdatedAt());
    }

    private Position resolvePosition(UUID positionId) {
        if (positionId == null) {
            return null;
        }
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new ApiException(CoreErrorCode.POSITION_NOT_FOUND));
    }
}


