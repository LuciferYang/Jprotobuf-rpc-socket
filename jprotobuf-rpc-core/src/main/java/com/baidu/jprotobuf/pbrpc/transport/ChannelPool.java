/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.jprotobuf.pbrpc.transport;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * Adapter for netty channel. Used by Mcpack Netty Client {@link NettyClient}.
 * 
 * @author xuyuepeng, sunzhongyi, lijianbin
 * 
 */
public class ChannelPool {
    private static final Logger LOGGER = Logger.getLogger(ChannelPool.class.getName());
    
    private final RpcClientOptions clientConfig;
    
    private final PooledObjectFactory<Connection> objectFactory;
    private final GenericObjectPool<Connection> pool;
    
    public ChannelPool(RpcClient rpcClient, String host, int port) {
        this.clientConfig = rpcClient.getRpcClientOptions();
        objectFactory = new ChannelPoolObjectFactory(rpcClient, host, port);
        pool = new GenericObjectPool<Connection>(objectFactory);
        pool.setMaxIdle(clientConfig.getMaxIdleSize());
        pool.setMaxTotal(clientConfig.getThreadPoolSize());
        pool.setMaxWaitMillis(clientConfig.getMaxWait());
        pool.setMinIdle(clientConfig.getMinIdleSize());
        pool.setMinEvictableIdleTimeMillis(clientConfig.getMinEvictableIdleTime());
        pool.setTestOnBorrow(clientConfig.isTestOnBorrow());
        pool.setTestOnReturn(clientConfig.isTestOnReturn());
    }
    
    public Connection getChannel() {
        Connection channel = null;
        try {
            if (!clientConfig.isShortConnection()) {
                channel = pool.borrowObject();
            } else {
                channel = objectFactory.makeObject().getObject();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return channel;
    }
    
    public void returnChannel(Connection channel) {
        try {
            if (!clientConfig.isShortConnection()) {
                pool.returnObject(channel);
            } else {
                if (channel.getFuture().channel().isOpen()) {
                    channel.getFuture().channel().close();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    
    public void stop() {
        try {
            if (pool != null) {
                pool.clear();
                pool.close();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "stop channel failed!", e);
        }
    }
}
