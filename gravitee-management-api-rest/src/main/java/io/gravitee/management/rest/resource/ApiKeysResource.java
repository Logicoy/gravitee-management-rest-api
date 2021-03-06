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
package io.gravitee.management.rest.resource;

import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.KeysByApplicationEntity;
import io.gravitee.management.service.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiKeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @PathParam("api")
    private String api;

    @Inject
    private ApiService apiService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private PermissionService permissionService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, KeysByApplicationEntity> keys() {
        apiService.findById(this.api);

        permissionService.hasPermission(getAuthenticatedUser(), api, PermissionType.EDIT_API);

        Map<String, List<ApiKeyEntity>> keys = apiKeyService.findByApi(api);
        Map<String, KeysByApplicationEntity> keysByApplication = new HashMap<>(keys.size());

        keys.forEach((application, apiKeyEntities) -> {
            ApplicationEntity applicationEntity = applicationService.findById(application);
            KeysByApplicationEntity keysByApplicationEntity = new KeysByApplicationEntity();

            keysByApplicationEntity.setName(applicationEntity.getName());
            keysByApplicationEntity.setKeys(apiKeyEntities);

            keysByApplication.put(application, keysByApplicationEntity);
        });

        return keysByApplication;
    }

    @DELETE
    @Path("{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revoke(@PathParam("key") String apiKey) {
        apiService.findById(this.api);

        permissionService.hasPermission(getAuthenticatedUser(), api, PermissionType.EDIT_API);

        apiKeyService.revoke(apiKey);

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    @PUT
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApiKeyEntity update(@PathParam("key") String apiKey, @Valid @NotNull ApiKeyEntity apiKeyEntity) {
        apiService.findById(this.api);

        permissionService.hasPermission(getAuthenticatedUser(), api, PermissionType.EDIT_API);

        return apiKeyService.update(apiKey, apiKeyEntity);
    }

    @Path("{key}/analytics")
    public ApiKeyAnalyticsResource getApiKeyAnalyticsResource() {
        return resourceContext.getResource(ApiKeyAnalyticsResource.class);
    }
}
