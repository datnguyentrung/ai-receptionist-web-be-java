package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import com.dat.ai_receptionist_web.dto.Finance.WalletDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.FinanceErrorCode;
import com.dat.ai_receptionist_web.mapper.Finance.WalletMapper;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.enums.Finance.WalletStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository repository;
    private final WalletMapper mapper;
    private final PersonRepository personRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<WalletDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<WalletDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAllDetailed(pageable), mapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về WalletDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public WalletDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận WalletDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về WalletDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public WalletDTO.Response create(WalletDTO.CreateRequest request) {
        Wallet entity = new Wallet();
        entity.setPerson(personRepository.findById(request.personId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setBalance(request.balance());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, WalletDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về WalletDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public WalletDTO.Response update(UUID id, WalletDTO.UpdateRequest request) {
        var entity = find(id);
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
        entity.setStatus(WalletStatus.CLOSED);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về Wallet theo kết quả xử lý.
     */
    private Wallet find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(FinanceErrorCode.WALLET_NOT_FOUND));
    }
}


