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
package io.gravitee.management.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.gravitee.management.model.PolicyEntity;
import io.gravitee.management.service.impl.PolicyServiceImpl;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyDefinition;
import io.gravitee.plugin.policy.PolicyManager;
import io.gravitee.repository.exceptions.TechnicalException;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyServiceTest {

    private static final String POLICY_ID = "myPolicy";

    @InjectMocks
    private PolicyService policyService = new PolicyServiceImpl();

    @Mock
    private PolicyManager policyManager;

    @Mock
    private PolicyDefinition policyDefinition;
    @Mock
    private Plugin plugin;
    @Mock
    private PluginManifest manifest;

    @Test
    public void shouldFindAll() throws TechnicalException {
        when(policyDefinition.id()).thenReturn(POLICY_ID);
        when(policyManager.getPolicyDefinitions()).thenReturn(Collections.singletonList(policyDefinition));
        when(policyDefinition.plugin()).thenReturn(plugin);
        when(plugin.manifest()).thenReturn(manifest);

        final Set<PolicyEntity> policies = policyService.findAll();

        assertNotNull(policies);
        assertEquals(POLICY_ID, policies.iterator().next().getId());
    }
}
