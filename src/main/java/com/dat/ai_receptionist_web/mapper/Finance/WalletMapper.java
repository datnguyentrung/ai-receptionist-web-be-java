package com.dat.ai_receptionist_web.mapper.Finance;

import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import com.dat.ai_receptionist_web.dto.Finance.WalletDTO;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = PersonMapper.class)
public interface WalletMapper {
    WalletDTO.Response toResponse(Wallet entity);

    WalletDTO.SimpleResponse toSimpleResponse(Wallet entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "status", source = "status")
    void updateEntity(WalletDTO.UpdateRequest request, @MappingTarget Wallet entity);
}
