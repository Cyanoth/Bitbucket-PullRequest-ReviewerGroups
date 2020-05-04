package com.cyanoth.prgroup.config.rest;

import com.atlassian.bitbucket.AuthorisationException;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionValidationService;
import com.atlassian.bitbucket.rest.RestErrorMessage;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.cyanoth.prgroup.config.PluginConfigDao;
import com.cyanoth.prgroup.config.PluginConfigDefaultValues;
import com.cyanoth.prgroup.config.PluginConfigResponse;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Path("/config")
public class PluginConfigApi {
    private static final Logger log = LoggerFactory.getLogger(PluginConfigApi.class);
    private final PermissionValidationService permissionValidationService;
    private final PluginConfigDao pluginConfigDao;
    private final I18nService i18nService;

    public static final int MAX_GROUP_SIZE_LOWER_LIMIT = 1;
    public static final int MAX_GROUP_SIZE_UPPER_LIMIT = 5000;

    @SuppressWarnings("unused") // Used in REST API docs for an example
    public static final PluginConfigResponse PLUGIN_CONFIG_RESPONSE_EXAMPLE = new PluginConfigResponse(
            PluginConfigDefaultValues.DEFAULT_MAX_GROUP_SIZE, PluginConfigDefaultValues.DEFAULT_HIDE_TRUNCATED_GROUPS);

    @Autowired
    public PluginConfigApi(@ComponentImport PermissionValidationService permissionValidationService,
                           @ComponentImport I18nService i18nService,
                           PluginConfigDao pluginConfigDao) {
        this.permissionValidationService = permissionValidationService;
        this.i18nService =i18nService;
        this.pluginConfigDao = pluginConfigDao;
    }

    /**
     * <p>Get Global Configuration of the Pull Request Group plugin.
     * <p>Requires <strong>global administrator</strong> permission.
     *
     * @response.representation.200.mediaType application/json
     * @response.representation.200.doc JSON object with each config property
     *
     * @response.representation.401.mediaType application/json
     * @response.representation.401.doc User is not authorised to use this API.
     *
     * @response.representation.500.mediaType application/json
     * @response.representation.500.doc An unhandled exception occurred
     *
     * @return HTTP Response
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getPluginConfig() {
        final String context = "getPluginConfig";
        try {
            this.permissionValidationService.validateForGlobal(Permission.ADMIN);
            return Response.ok(new Gson().toJson(new PluginConfigResponse(pluginConfigDao.getPluginSettings()))).build();
        }
        catch (Exception e) {
            return commonApiExceptionResponse(context, e);
        }
    }

    /**
     * <p>Updates Global Configuration of the Pull Request Group plugin.
     * <p>Requires <strong>global administrator</strong> permission.</p>
     *
     * @request.representation.mediaType application/json
     * @request.representation.example {@link PluginConfigApi#PLUGIN_CONFIG_RESPONSE_EXAMPLE}
     *
     * @response.representation.204.mediaType application/json
     * @response.representation.204.doc Global Configuration updated successfully
     *
     * @response.representation.400.mediaType application/json
     * @response.representation.400.doc One or more fields had an invalid value.
     *
     * @response.representation.401.mediaType application/json
     * @response.representation.401.doc User is not authorised to use this API.
     *
     * @response.representation.500.mediaType application/json
     * @response.representation.500.doc An unhandled exception occurred
     *
     * @return HTTP Response
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response updatePluginConfig(PluginConfigResponse res) {
        final String context = "updatePluginConfig";
        try {
            this.permissionValidationService.validateForGlobal(Permission.ADMIN);

            if (res.getMaxGroupSize() < MAX_GROUP_SIZE_LOWER_LIMIT || res.getMaxGroupSize() > MAX_GROUP_SIZE_UPPER_LIMIT)
                return Response.status(Response.Status.BAD_REQUEST).entity(
                        new RestErrorMessage(context,i18nService.getMessage("com.cyanoth.prgroup.api.bad_request.max_group_size",
                                        MAX_GROUP_SIZE_LOWER_LIMIT, MAX_GROUP_SIZE_UPPER_LIMIT)))
                        .build();

            pluginConfigDao.updateSettings(res.toEntity(pluginConfigDao.getPluginSettings()));
            return Response.noContent().build();
        }
        catch (Exception e) {
            return commonApiExceptionResponse(context, e);
        }
    }

    private Response commonApiExceptionResponse(String context, Exception e) {
        if (e instanceof AuthorisationException) {
            return Response.status(Response.Status.FORBIDDEN).entity(
                    new RestErrorMessage(context, i18nService.getMessage("com.cyanoth.prgroup.api.not_authorised")))
                    .build();
        }
        else {
            log.error("PRGroup {} - Unhandled Exception occurred!", context, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new RestErrorMessage(context, i18nService.getMessage("com.cyanoth.prgroup.api.unhandled_exception")))
                    .build();
        }
    }
}
