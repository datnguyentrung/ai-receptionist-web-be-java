package com.dat.ai_receptionist_web.mapper.Finance;

import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.dto.Finance.CoursePurchaseDTO;
import com.dat.ai_receptionist_web.mapper.Catalog.CoursePriceMapper;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PersonMapper.class, CoursePriceMapper.class})
public interface CoursePurchaseMapper {
    @Mapping(target = "debitTransactionId", source = "debitTransaction.walletTransactionId")
    CoursePurchaseDTO.Response toResponse(CoursePurchase entity);

    @Mapping(target = "debitTransactionId", source = "debitTransaction.walletTransactionId")
    CoursePurchaseDTO.SimpleResponse toSimpleResponse(CoursePurchase entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "studentPerson", ignore = true)
    @Mapping(target = "coursePrice", ignore = true)
    @Mapping(target = "debitTransaction", ignore = true)
    void updateEntity(CoursePurchaseDTO.UpdateRequest request, @MappingTarget CoursePurchase entity);
}
