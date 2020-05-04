package com.cyanoth.prgroup.config.ui;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserRole;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.cyanoth.prgroup.config.PluginConfigDao;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginConfigServlet extends HttpServlet
{
    private final UserManager userManager;
    private final LoginUriProvider loginUriProvider;
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final PageBuilderService pageBuilderService;
    private final PluginConfigDao pluginConfigDao;

    @Autowired
    public PluginConfigServlet(@ComponentImport UserManager userManager,
                               @ComponentImport LoginUriProvider loginUriProvider,
                               @ComponentImport SoyTemplateRenderer soyTemplateRenderer,
                               @ComponentImport PageBuilderService pageBuilderService,
                               PluginConfigDao pluginConfigDao) {
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.pageBuilderService = pageBuilderService;
        this.pluginConfigDao = pluginConfigDao;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        UserKey userKey = userManager.getRemoteUserKey(request);
        if (userKey == null || !userManager.isAdmin(userKey))
        {
            response.sendRedirect(this.loginUriProvider.getLoginUriForRole(this.getUri(request), UserRole.ADMIN).toASCIIString());
        }

        response.setContentType("text/html;charset=UTF-8");
        this.pageBuilderService.assembler().resources().requireContext("com.cyanoth.prgroup.config");

        soyTemplateRenderer.render(response.getWriter(), "com.cyanoth.prgroup:prgroup-config-ui-res",
                "com.cyanoth.prgroup.configPage",
                ImmutableMap.<String, Object>builder()
                        .put("configValMaxGroupSize",pluginConfigDao.getPluginSettings().getMaxGroupSize())
                        .put("configValHideTruncatedGroups", (pluginConfigDao.getPluginSettings().getHideTruncatedGroups() ? "checked" : ""))
                        .build());
    }

    private URI getUri(HttpServletRequest request)
    {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null)
        {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }
}