package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.dto.Finance.CoursePurchaseDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.FinanceErrorCode;
import com.dat.ai_receptionist_web.mapper.Finance.CoursePurchaseMapper;
import com.dat.ai_receptionist_web.repository.Finance.CoursePurchaseRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CoursePriceRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletTransactionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoursePurchaseService {
    private final CoursePurchaseRepository repository;
    private final CoursePurchaseMapper mapper;
    private final PersonRepository personRepository;
    private final CoursePriceRepository coursePriceRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<CoursePurchaseDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<CoursePurchaseDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CoursePurchaseDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public CoursePurchaseDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận CoursePurchaseDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về CoursePurchaseDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public CoursePurchaseDTO.Response create(CoursePurchaseDTO.CreateRequest request) {
        CoursePurchase entity = new CoursePurchase();
        entity.setStudentPerson(personRepository.findById(request.studentPersonId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setCoursePrice(coursePriceRepository.findById(request.coursePriceId()).orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_PRICE_NOT_FOUND)));
        entity.setDebitTransaction(walletTransactionRepository.findById(request.debitTransactionId()).orElseThrow(() -> new ApiException(FinanceErrorCode.WALLET_TRANSACTION_NOT_FOUND)));
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, CoursePurchaseDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về CoursePurchaseDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public CoursePurchaseDTO.Response update(UUID id, CoursePurchaseDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setStudentPerson(personRepository.findById(request.studentPersonId()).orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setCoursePrice(coursePriceRepository.findById(request.coursePriceId()).orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_PRICE_NOT_FOUND)));
        entity.setDebitTransaction(walletTransactionRepository.findById(request.debitTransactionId()).orElseThrow(() -> new ApiException(FinanceErrorCode.WALLET_TRANSACTION_NOT_FOUND)));
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
        repository.delete(entity);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CoursePurchase theo kết quả xử lý.
     */
    private CoursePurchase find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(FinanceErrorCode.COURSE_PURCHASE_NOT_FOUND));
    }
}


