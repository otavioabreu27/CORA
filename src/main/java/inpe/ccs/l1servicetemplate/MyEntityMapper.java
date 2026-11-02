package inpe.ccs.l1servicetemplate;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MyEntityMapper {
    MyEntityResponse toResponse(MyEntity entity);

    @Mapping(target = "id", ignore = true)
    MyEntity toEntity(MyEntityResponse response);
}
