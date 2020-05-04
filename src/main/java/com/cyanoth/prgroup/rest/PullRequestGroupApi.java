package com.cyanoth.prgroup.rest;

import com.atlassian.bitbucket.AuthorisationException;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.rest.RestErrorMessage;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.cyanoth.prgroup.PullRequestGroupProvider;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Component
public class PullRequestGroupApi {
    private static final Logger log = LoggerFactory.getLogger(PullRequestGroupApi.class);

    private final RepositoryService repositoryService;
    private final PullRequestGroupProvider pullRequestGroupProvider;
    private final I18nService i18nService;

    @Autowired
    public PullRequestGroupApi(@ComponentImport RepositoryService repositoryService,
                               @ComponentImport I18nService i18nService,
                               PullRequestGroupProvider pullRequestGroupProvider) {
        this.repositoryService = repositoryService;
        this.i18nService = i18nService;
        this.pullRequestGroupProvider = pullRequestGroupProvider;
    }

    /**
     * <p>Get a list of bitbucket groups names with active & licensed members that have permission to a repository.</p>
     * <p>This information is used for the Pull Request Group Reviewer Plugin.</p>
     *
     * <p>Additionally, there is limit of the amount of members each group can return. Set by the global administrator.
     * If a group has more than members than this limit, the response property members_truncated will be true.</p>
     *
     * <p>Requires <strong>read permission</strong> to the repository given in the args.
     *
     * @response.representation.200.mediaType application/json
     * @response.representation.200.doc JSON object with a list of bitbucket groups names with active & licensed members
     *
     * @response.representation.401.mediaType application/json
     * @response.representation.401.doc User does not have permission to the repository to get group information of.
     *
     * @response.representation.500.mediaType application/json
     * @response.representation.500.doc An unhandled exception occurred. See Server logs for more information
     *
     * @return HTTP Response
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/{projectKey}/{repositorySlug}")
    public Response getPermittedGroupsWithMembers(@PathParam("projectKey") String projectKey, @PathParam("repositorySlug") String repositorySlug) {
        try {
            // Bitbucket will check whether or not the calling user has access to get the repository object. If not, throws AuthorisationException
            Repository repository =  repositoryService.getBySlug(projectKey, repositorySlug);

            if (repository == null)
                return Response.status(Response.Status.NOT_FOUND).entity(
                        new RestErrorMessage("getPermittedGroupsWithMembers", i18nService.getMessage("com.cyanoth.prgroup.api.resource_not_found")))
                        .build();

            return Response.ok(new Gson().toJson(pullRequestGroupProvider.getPullRequestGroups(repository))).build();
        }
        catch(AuthorisationException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(
                    new RestErrorMessage("getPermittedGroupsWithMembers", i18nService.getMessage("com.cyanoth.prgroup.api.not_authorised")))
                    .build();
        }
        catch (Exception e) {
            log.error("PRGroup - Unhandled Exception occurred!", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new RestErrorMessage("getPermittedGroupsWithMembers", i18nService.getMessage("com.cyanoth.prgroup.api.unhandled_exception")))
                    .build();
        }
    }
}

