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

package org.apache.dolphinscheduler.api.security.impl.ldap;

import org.apache.dolphinscheduler.api.dto.LdapLoginResult;
import org.apache.dolphinscheduler.api.security.LdapUserNotExistActionType;
import org.apache.dolphinscheduler.common.enums.UserType;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Component;

@Component
@Configuration
@Slf4j
public class LdapService {

    @Value("${security.authentication.ldap.user.admin-username:#{null}}")
    private String ldapAdminUserName;

    @Value("${security.authentication.ldap.user.admin-user-filter:#{null}}")
    private String ldapAdminUserFilter;

    @Value("${security.authentication.ldap.url:#{null}}")
    private String ldapUrl;

    @Value("${security.authentication.ldap.base-dn:#{null}}")
    private String ldapBaseDn;

    @Value("${security.authentication.ldap.username:#{null}}")
    private String ldapSecurityPrincipal;

    @Value("${security.authentication.ldap.password:#{null}}")
    private String ldapPrincipalPassword;

    @Value("${security.authentication.ldap.user.identity-attribute:#{null}}")
    private String ldapUserIdentifyingAttribute;

    @Value("${security.authentication.ldap.user.email-attribute:#{null}}")
    private String ldapEmailAttribute;

    @Value("${security.authentication.ldap.user.not-exist-action:DENY}")
    private String ldapUserNotExistAction;

    @Value("${security.authentication.ldap.ssl.enable:false}")
    private Boolean sslEnable;

    @Value("${security.authentication.ldap.ssl.trust-store:#{null}}")
    private String trustStore;

    @Value("${security.authentication.ldap.ssl.trust-store-password:#{null}}")
    private String trustStorePassword;

    /**
     * login by userName and return LdapLoginResult
     */
    public LdapLoginResult ldapLogin(String userName, String userPwd) {
        Properties searchEnv = getManagerLdapEnv();
        LdapContext ctx = null;
        LdapLoginResult ldapLoginResult = new LdapLoginResult();
        ldapLoginResult.setSuccess(false);
        if (StringUtils.isEmpty(ldapEmailAttribute)) {
            log.warn("ldap email attribute is empty, skipping ldap authentication");
            return ldapLoginResult;
        }

        try {
            // Connect to the LDAP server and Authenticate with a service user of whom we know the DN and credentials
            ctx = new InitialLdapContext(searchEnv, null);
            SearchControls sc = new SearchControls();
            sc.setReturningAttributes(new String[]{ldapEmailAttribute});
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            EqualsFilter userFilter = new EqualsFilter(ldapUserIdentifyingAttribute, userName);
            String userSearchEmail = ldapSearch(ctx, null, userPwd, userFilter.toString(), sc, searchEnv);

            if (StringUtils.isNotEmpty(ldapAdminUserFilter)) {
                String adminFilterSearchEmail = ldapSearch(ctx, userName, userPwd, ldapAdminUserFilter, sc, searchEnv);
                if (adminFilterSearchEmail != null) {
                    ldapLoginResult.setLdapEmail(adminFilterSearchEmail);
                    ldapLoginResult.setUserType(UserType.ADMIN_USER);
                    ldapLoginResult.setUserName(userName);
                    ldapLoginResult.setSuccess(true);
                    return ldapLoginResult;
                }
            } else {
                log.debug("ldap admin user filter is empty, skipping admin user filter search");
            }

            if (userSearchEmail != null) {
                if (Objects.equals(ldapAdminUserName, userName)) {
                    ldapLoginResult.setUserType(UserType.ADMIN_USER);
                } else {
                    ldapLoginResult.setUserType(UserType.GENERAL_USER);
                }

                ldapLoginResult.setLdapEmail(userSearchEmail);
                ldapLoginResult.setUserName(userName);
                ldapLoginResult.setSuccess(true);
                return ldapLoginResult;
            } else {
                log.debug("user email attribute {} not found in ldap", ldapEmailAttribute);
            }
        } catch (NamingException e) {
            log.error("ldap search error", e);
            return ldapLoginResult;
        } finally {
            try {
                if (ctx != null) {
                    ctx.close();
                }
            } catch (NamingException e) {
                log.error("ldap context close error", e);
            }
        }

        return ldapLoginResult;
    }

    /***
     * get ldap env fot ldap server search
     * @return Properties
     */
    Properties getManagerLdapEnv() {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, ldapSecurityPrincipal);
        env.put(Context.SECURITY_CREDENTIALS, ldapPrincipalPassword);
        env.put(Context.PROVIDER_URL, ldapUrl);

        if (sslEnable) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            System.setProperty("javax.net.ssl.trustStore", trustStore);
            if (StringUtils.isNotEmpty(trustStorePassword)) {
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            }
        }
        return env;
    }

    private String ldapSearch(LdapContext ctx,
                              String userName,
                              String userPwd,
                              String filter,
                              SearchControls sc,
                              Properties searchEnv) throws NamingException {
        NamingEnumeration<SearchResult> results;
        if (userName == null) {
            results = ctx.search(ldapBaseDn, filter, sc);
        } else {
            results = ctx.search(ldapBaseDn, filter, new Object[]{userName}, sc);
        }
        if (results.hasMore()) {
            // get the users DN (distinguishedName) from the result
            SearchResult result = results.next();
            NamingEnumeration<? extends Attribute> attrs = result.getAttributes().getAll();
            while (attrs.hasMore()) {
                // Open another connection to the LDAP server with the found DN and the password
                searchEnv.put(Context.SECURITY_PRINCIPAL, result.getNameInNamespace());
                searchEnv.put(Context.SECURITY_CREDENTIALS, userPwd);
                try {
                    new InitialDirContext(searchEnv);
                } catch (Exception e) {
                    log.warn("invalid ldap credentials or ldap search error", e);
                    return null;
                }
                Attribute attr = attrs.next();
                if (attr.getID().equals(ldapEmailAttribute)) {
                    return (String) attr.get();
                }
            }
        }

        return null;
    }

    public LdapUserNotExistActionType getLdapUserNotExistAction() {
        if (StringUtils.isBlank(ldapUserNotExistAction)) {
            log.info(
                    "security.authentication.ldap.user.not.exist.action configuration is empty, the default value 'CREATE'");
            return LdapUserNotExistActionType.CREATE;
        }

        return LdapUserNotExistActionType.valueOf(ldapUserNotExistAction);
    }

    public boolean createIfUserNotExists() {
        return getLdapUserNotExistAction() == LdapUserNotExistActionType.CREATE;
    }
}
