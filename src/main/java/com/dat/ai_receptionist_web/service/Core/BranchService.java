package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.dto.Core.BranchDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Core.BranchStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.mapper.Core.BranchMapper;
import com.dat.ai_receptionist_web.repository.Core.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchService{
    private final BranchRepository branchRepository;

    private final BranchMapper branchMapper;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<BranchDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<BranchDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(branchRepository.findAll(pageable), branchMapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận Long id từ caller hoặc request.
     * Output: Trả về BranchDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public BranchDTO.Response get(Long id) {
        return branchMapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận BranchDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về BranchDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public BranchDTO.Response create(BranchDTO.CreateRequest request) {
        Branch branch = branchMapper.toEntity(request);
        return branchMapper.toResponse(branchRepository.save(branch));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận Long id, BranchDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về BranchDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public BranchDTO.Response update(Long id, BranchDTO.UpdateRequest request) {
        Branch branch = find(id);
        branchMapper.updateEntity(request, branch);
        return branchMapper.toResponse(branchRepository.save(branch));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận Long id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(Long id) {
        Branch branch = find(id);
        branch.setStatus(BranchStatus.CLOSED);
    }

    /**
     * Tác dụng: Thực hiện logic getAllBranches của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về List<BranchDTO.Response> theo kết quả xử lý.
     */
    public List<BranchDTO.Response> getAllBranches() {
        List<Branch> branches = branchRepository.findAll();
        return branchMapper.toResponseList(branches);
    }

    /**
     * Tác dụng: Thực hiện logic getBranchById của lớp hiện tại.
     * Input: Nhận Long idBranch từ caller hoặc request.
     * Output: Trả về BranchDTO.Response theo kết quả xử lý.
     */
    public BranchDTO.Response getBranchById(Long idBranch) {
        Branch branch = branchRepository.findById(idBranch)
                .orElseThrow(() ->
                        new ApiException(
                                CoreErrorCode.BRANCH_NOT_FOUND,
                                "Branch with id " + idBranch + " not found"
                        ));

        return branchMapper.toResponse(branch);
    }

    /**
     * Tác dụng: Thực hiện logic createBranch của lớp hiện tại.
     * Input: Nhận BranchDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về BranchDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public BranchDTO.Response createBranch(BranchDTO.CreateRequest request) {
        Branch branch = branchMapper.toEntity(request);
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }

    /**
     * Tác dụng: Thực hiện logic updateBranch của lớp hiện tại.
     * Input: Nhận Long idBranch, BranchDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về BranchDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public BranchDTO.Response updateBranch(Long idBranch, BranchDTO.CreateRequest request) {
        Optional<Branch> optionalBranch = branchRepository.findById(idBranch);
        if (optionalBranch.isEmpty()) {
            throw new ApiException(
                    CoreErrorCode.BRANCH_NOT_FOUND,
                    "Branch with id " + idBranch + " not found");
        }

        Branch branchToUpdate = optionalBranch.get();
        branchToUpdate.setName(request.name());
        branchToUpdate.setAddress(request.address());
        branchToUpdate.setHotline(request.hotline());
        branchToUpdate.setOpenedDate(request.openedDate());
        branchToUpdate.setStatus(request.status());

        Branch updatedBranch = branchRepository.save(branchToUpdate);
        return branchMapper.toResponse(updatedBranch);
    }

    /**
     * Tác dụng: Thực hiện logic deleteBranch của lớp hiện tại.
     * Input: Nhận Long idBranch từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void deleteBranch(Long idBranch) {
        delete(idBranch);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận Long id từ caller hoặc request.
     * Output: Trả về Branch theo kết quả xử lý.
     */
    private Branch find(Long id) {
        return branchRepository.findById(id).orElseThrow(() -> new ApiException(CoreErrorCode.BRANCH_NOT_FOUND));
    }
}


