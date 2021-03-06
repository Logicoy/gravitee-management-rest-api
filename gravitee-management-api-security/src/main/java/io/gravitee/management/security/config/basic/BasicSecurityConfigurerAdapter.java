/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.security.config.basic;

import io.gravitee.management.providers.core.authentication.AuthenticationManager;
import io.gravitee.management.security.config.basic.filter.CORSFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Titouan COMPIEGNE
 *
 */
@Configuration
@Profile("basic")
@EnableWebSecurity
public class BasicSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicSecurityConfigurerAdapter.class);

	@Autowired
	private Environment environment;

	@Autowired
	private Collection<AuthenticationManager> authenticationManagers;

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		LOGGER.info("Loading security providers for basic authentication");

		List<String> providers = getSecurityProviders();

		for (int idx = 0 ; idx < providers.size() ; idx++) {
			String providerType = providers.get(idx);

			boolean found = false;
			for (AuthenticationManager authenticationManager : authenticationManagers) {
				if (authenticationManager.canHandle(providerType)) {
					authenticationManager.configure(auth, idx);
					found = true;
					break;
				}
			}

			if (! found) {
				LOGGER.error("No authentication provider found for type: {}", providerType);
			}
		}
	}

	private List<String> getSecurityProviders() {
		LOGGER.debug("Looking for security provider...");
		List<String> providers = new ArrayList<>();

		boolean found = true;
		int idx = 0;

		while (found) {
			String type = environment.getProperty("security.providers[" + (idx++) + "].type");
			found = (type != null);
			if (found) {
				LOGGER.debug("\tSecurity type {} has been defined", type);
				providers.add(type);
			}
		}

		return providers;
	}

	/*
     * TODO : fix filter order between Jersey Filter (CORSResponseFilter) and Spring Security Filter
     * TODO : remove this filter or CORSResponseFilter when the problem will be solved
     */
    @Bean
	public Filter corsFilter() {
		return new CORSFilter();
	}
    
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic()
				.realmName("Gravitee.io Management API")
			.and()
				.sessionManagement()
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
				.authorizeRequests()
					.antMatchers(HttpMethod.OPTIONS, "**").permitAll()
					.antMatchers(HttpMethod.GET, "/apis/**").permitAll()
					.anyRequest().authenticated()
			.and()
				.csrf()
					.disable()
			.addFilterAfter(corsFilter(), AbstractPreAuthenticatedProcessingFilter.class);
	}
}
