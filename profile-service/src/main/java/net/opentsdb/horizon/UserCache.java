/*
 * This file is part of OpenTSDB.
 *  Copyright (C) 2021 Yahoo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express  implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.opentsdb.horizon;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.opentsdb.horizon.model.User;
import net.opentsdb.horizon.store.UserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.concurrent.ExecutionException;

public class UserCache {

    private static Logger logger = LoggerFactory.getLogger(UserCache.class);

    private Cache<String, User> userCache;
    private UserStore userStore;

    public UserCache(CacheConfig cacheConfig, UserStore userStore) {
        this.userStore = userStore;
        this.userCache = CacheBuilder.newBuilder().expireAfterWrite(cacheConfig.userTTL, cacheConfig.userTTLUnit).build();
    }

    public User getById(String id) {
        try {
            return userCache.get(id, () -> loadUser(id));
        } catch (ExecutionException e) {
            logger.error("Error reading user cache", e);
            return null;
        }
    }

    private User loadUser(String id) throws Exception {
        try (Connection connection = userStore.getReadOnlyConnection()) {
            User user = userStore.getById(id, connection);
            if (user == null) {
                throw new Exception("User not found with id: " + id);
            }
            logger.debug("User cache refreshed with id {}", id);
            return user;
        }
    }

}
