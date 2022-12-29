package com.loblaw.metrics.helper;

import org.mapstruct.Mapper;

import com.loblaw.metrics.shared.model.AppReq;
import com.loblaw.metrics.shared.model.OutAppReq;

@Mapper(componentModel = "spring")
public interface AppRequestMapper {
	OutAppReq appReqToOutAppReq(AppReq appReq);

	AppReq outAppReqToAppReq(OutAppReq outAppReq);
}
