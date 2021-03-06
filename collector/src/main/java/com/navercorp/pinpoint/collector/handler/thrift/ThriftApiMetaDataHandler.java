/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.collector.handler.thrift;

import com.navercorp.pinpoint.collector.handler.RequestResponseHandler;
import com.navercorp.pinpoint.collector.service.ApiMetaDataService;
import com.navercorp.pinpoint.common.server.bo.ApiMetaDataBo;
import com.navercorp.pinpoint.common.server.bo.MethodTypeEnum;
import com.navercorp.pinpoint.common.util.LineNumber;
import com.navercorp.pinpoint.io.request.ServerRequest;
import com.navercorp.pinpoint.io.request.ServerResponse;
import com.navercorp.pinpoint.thrift.dto.TApiMetaData;
import com.navercorp.pinpoint.thrift.dto.TResult;

import org.apache.thrift.TBase;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author emeroad
 */
@Service
public class ThriftApiMetaDataHandler implements RequestResponseHandler<TBase<?, ?>, TBase<?, ?>> {

    private final Logger logger = LogManager.getLogger(getClass());

    private final ApiMetaDataService apiMetaDataService;

    public ThriftApiMetaDataHandler(ApiMetaDataService apiMetaDataService) {
        this.apiMetaDataService = Objects.requireNonNull(apiMetaDataService, "apiMetaDataService");
    }

    @Override
    public void handleRequest(ServerRequest<TBase<?, ?>> serverRequest, ServerResponse<TBase<?, ?>> serverResponse) {
        final TBase<?, ?> data = serverRequest.getData();
        if (logger.isDebugEnabled()) {
            logger.debug("Handle request data={}", data);
        }

        if (data instanceof TApiMetaData) {
            TResult result = handleApiMetaData((TApiMetaData) data);
            serverResponse.write(result);
        } else {
            logger.warn("invalid serverRequest:{}", serverRequest);
        }
    }

    private TResult handleApiMetaData(TApiMetaData apiMetaData) {
        try {
            final String agentId = apiMetaData.getAgentId();
            final long agentStartTime = apiMetaData.getAgentStartTime();
            final int apiId = apiMetaData.getApiId();

            int lineNumber = LineNumber.NO_LINE_NUMBER;
            if (apiMetaData.isSetLine()) {
                lineNumber = apiMetaData.getLine();
            }

            MethodTypeEnum methodType = MethodTypeEnum.DEFAULT;
            if (apiMetaData.isSetType()) {
                methodType = MethodTypeEnum.valueOf(apiMetaData.getType());
            }

            final String apiInfo = apiMetaData.getApiInfo();
            final ApiMetaDataBo apiMetaDataBo = new ApiMetaDataBo(agentId, agentStartTime, apiId, lineNumber,
                    methodType, apiInfo);

            this.apiMetaDataService.insert(apiMetaDataBo);
        } catch (Exception e) {
            logger.warn("Failed to handle apiMetaData={}, Caused:{}", apiMetaData, e.getMessage(), e);
            final TResult result = new TResult(false);
            result.setMessage(e.getMessage());
            return result;
        }
        return new TResult(true);
    }
}