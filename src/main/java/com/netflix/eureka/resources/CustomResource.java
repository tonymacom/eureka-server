/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.eureka.resources;

import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.registry.AbstractInstanceRegistry;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * 自定义upgrade端点, 用于关闭一次eureka自我保护机制.
 * @author www.yamibuy.com
 *
 */
@Slf4j
@Path("/{version}/upgrade")
@Produces({"application/xml", "application/json"})
public class CustomResource {

    private final PeerAwareInstanceRegistry registry;

    private final PeerAwareInstanceRegistryImpl registryImpl;



    @Inject
    CustomResource(EurekaServerContext eurekaServer) {
        this.registry = eurekaServer.getRegistry();

        if(this.registry instanceof PeerAwareInstanceRegistryImpl){
            registryImpl = (PeerAwareInstanceRegistryImpl)registry;
        }else{
            registryImpl = null;
        }

    }

    public CustomResource() {
        this(EurekaServerContextHolder.getInstance().getServerContext());
    }

    /**
     * 手动释放eureka自我保护
     *
     * expectedNumberOfRenewsPerMin值与numberOfRenewsPerMinThreshold 值变更规律:
     *
     * 初始化 :     {@link PeerAwareInstanceRegistryImpl#openForTraffic} 执行一次
     * expectedNumberOfRenewsPerMin = 2;
     * numberOfRenewsPerMinThreshold = (int) (expectedNumberOfRenewsPerMin * 2 * 0.85) = 1
     *
     * 更新 :      {@link PeerAwareInstanceRegistryImpl#updateRenewalThreshold} 每15分钟执行一次, 只有当 count *2 > numberOfRenewsPerMinThreshold * 0.85时执行
     * expectedNumberOfRenewsPerMin  = count * 2
     * numberOfRenewsPerMinThreshold = (int) ((count * 2) * 0.85 = 1
     *
     * 服务注册 :   {@link AbstractInstanceRegistry#register}
     *
     * expectedNumberOfRenewsPerMin = expectedNumberOfRenewsPerMin + 2;
     * numberOfRenewsPerMinThreshold = (int) expectedNumberOfRenewsPerMin * 0.85
     *
     *
     * 清除服务 :   {@link PeerAwareInstanceRegistryImpl#cancel}
     *
     * expectedNumberOfRenewsPerMin = expectedNumberOfRenewsPerMin - 2
     * numberOfRenewsPerMinThreshold = (int) (expectedNumberOfRenewsPerMin * 0.85)
     *
     */
    @POST
    public Response lastBucketRest() {
        String result;
        if(registryImpl!=null){
            registryImpl.upgrade.compareAndSet(false,true);
            result = "success";
        }else{
            result = "failure";
        }
        return Response.ok(result).build();
    }

}
