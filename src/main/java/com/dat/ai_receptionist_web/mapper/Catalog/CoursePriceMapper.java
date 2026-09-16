package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CoursePrice;
import com.dat.ai_receptionist_web.dto.Catalog.CoursePriceDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CourseMapper.class)
public interface CoursePriceMapper {
    CoursePriceDTO.Response toResponse(CoursePrice entity);

    CoursePriceDTO.SimpleResponse toSimpleResponse(CoursePrice entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "durationMonths", source = "durationMonths")
    @Mapping(target = "sessionCount", source = "sessionCount")
    @Mapping(target = "basePrice", source = "basePrice")
    @Mapping(target = "finalPrice", source = "finalPrice")
    @Mapping(target = "status", source = "status")
    void updateEntity(CoursePriceDTO.UpdateRequest request, @MappingTarget CoursePrice entity);
}
