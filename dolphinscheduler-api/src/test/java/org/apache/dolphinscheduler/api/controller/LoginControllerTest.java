/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.Session;
import org.apache.dolphinscheduler.dao.repository.SessionDao;

import org.apache.http.HttpStatus;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * login controller test
 */
public class LoginControllerTest extends AbstractControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(LoginControllerTest.class);

    @Autowired
    private SessionDao sessionDao;

    @Test
    public void testLogin() throws Exception {
        MultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();
        paramsMap.add("userName", "admin");
        paramsMap.add("userPassword", "dolphinscheduler123");

        MvcResult mvcResult = mockMvc.perform(post("/login")
                .params(paramsMap))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Result result = JSONUtils.parseObject(mvcResult.getResponse().getContentAsString(), Result.class);
        Assertions.assertEquals(Status.SUCCESS.getCode(), result.getCode().intValue());
        logger.info(mvcResult.getResponse().getContentAsString());
        Map<String, String> data = (Map<String, String>) result.getData();
        Assertions.assertEquals(Constants.SECURITY_CONFIG_TYPE_PASSWORD, data.get(Constants.SECURITY_CONFIG_TYPE));
        Assertions.assertNotEquals(Constants.SECURITY_CONFIG_TYPE_LDAP, data.get(Constants.SECURITY_CONFIG_TYPE));
    }

    @Test
    public void testSignOut() throws Exception {
        MultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();

        MvcResult mvcResult = mockMvc.perform(post("/signOut")
                .header("sessionId", sessionId)
                .params(paramsMap))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Result result = JSONUtils.parseObject(mvcResult.getResponse().getContentAsString(), Result.class);
        Assertions.assertEquals(Status.SUCCESS.getCode(), result.getCode().intValue());
        logger.info(mvcResult.getResponse().getContentAsString());
    }

    @Test
    void testSignOutWithExpireSession() throws Exception {
        final Session session = sessionDao.queryById(sessionId);
        session.setLastLoginTime(new Date(System.currentTimeMillis() - Constants.SESSION_TIME_OUT * 1000 - 1));
        sessionDao.updateById(session);

        mockMvc.perform(post("/signOut")
                .header("sessionId", sessionId))
                .andExpect(status().is(HttpStatus.SC_UNAUTHORIZED))
                .andReturn();
    }

    @Test
    void testClearCookie() throws Exception {
        MvcResult mvcResult = mockMvc.perform(delete("/cookies")
                .header("sessionId", sessionId)
                .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        Cookie[] cookies = response.getCookies();
        for (Cookie cookie : cookies) {
            Assertions.assertEquals(0, cookie.getMaxAge());
            Assertions.assertNull(cookie.getValue());
        }
    }

    @Test
    void testGetOauth2Provider() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/oauth2-provider"))
                .andExpect(status().isOk())
                .andReturn();
        Result result = JSONUtils.parseObject(mvcResult.getResponse().getContentAsString(), Result.class);
        Assertions.assertEquals(Status.SUCCESS.getCode(), result.getCode().intValue());
    }
}
