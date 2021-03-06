/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.grpc.client;

import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.grpc.client.config.ClientOption;
import com.navercorp.pinpoint.grpc.client.config.SslOption;
import com.navercorp.pinpoint.grpc.security.SslClientConfig;
import com.navercorp.pinpoint.grpc.util.Resource;

import io.grpc.ClientInterceptor;
import io.grpc.NameResolverProvider;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.LinkedList;
import java.util.Objects;

/**
 * @author Woonduk Kang(emeroad)
 */
public class DefaultChannelFactoryBuilder implements ChannelFactoryBuilder {

    private final Logger logger = LogManager.getLogger(this.getClass());

    private final String factoryName;

    private int executorQueueSize = 1024;
    private HeaderFactory headerFactory;

    private ClientOption clientOption;
    private SslOption sslOption;

    private final LinkedList<ClientInterceptor> clientInterceptorList = new LinkedList<>();
    private NameResolverProvider nameResolverProvider;

    public DefaultChannelFactoryBuilder(String factoryName) {
        this.factoryName = Objects.requireNonNull(factoryName, "factoryName");
    }

    @Override
    public void setExecutorQueueSize(int executorQueueSize) {
        Assert.isTrue(executorQueueSize > 0, "must be `executorQueueSize > 0`");
        this.executorQueueSize = executorQueueSize;
    }

    @Override
    public void setHeaderFactory(HeaderFactory headerFactory) {
        this.headerFactory = Objects.requireNonNull(headerFactory, "headerFactory");
    }

    @Override
    public void addFirstClientInterceptor(ClientInterceptor clientInterceptor) {
        Objects.requireNonNull(clientInterceptor, "clientInterceptor");
        this.clientInterceptorList.addFirst(clientInterceptor);
    }

    @Override
    public void addClientInterceptor(ClientInterceptor clientInterceptor) {
        Objects.requireNonNull(clientInterceptor, "clientInterceptor");
        this.clientInterceptorList.add(clientInterceptor);
    }

    @Override
    public void setClientOption(ClientOption clientOption) {
        this.clientOption = Objects.requireNonNull(clientOption, "clientOption");
    }

    @Override
    public void setSslOption(SslOption sslOption) {
        // nullable
        this.sslOption = sslOption;
    }

    @Override
    public void setNameResolverProvider(NameResolverProvider nameResolverProvider) {
        this.nameResolverProvider = Objects.requireNonNull(nameResolverProvider, "nameResolverProvider");
    }

    @Override
    public ChannelFactory build() {
        logger.info("build ChannelFactory:{}", factoryName);
        Objects.requireNonNull(headerFactory, "headerFactory");
        Objects.requireNonNull(clientOption, "clientOption");

        SslClientConfig sslClientConfig = SslClientConfig.DISABLED_CONFIG;
        if (sslOption != null && sslOption.isEnable()) {
            String providerType = sslOption.getProviderType();
            Resource trustCertResource = sslOption.getTrustCertResource();
            sslClientConfig = new SslClientConfig(true, providerType, trustCertResource);
        }

        return new DefaultChannelFactory(factoryName, executorQueueSize,
                headerFactory, nameResolverProvider,
                clientOption, sslClientConfig, clientInterceptorList);
    }
}
