package com.cyanoth.prgroup.rest;

import com.atlassian.bitbucket.AuthorisationException;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.rest.RestErrorMessage;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.cyanoth.prgroup.PullRequestGroupProvider;
import com.cyanoth.prgroup.exceptions.RepositoryDoesNotExistException;
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

    @Autowired
    public PullRequestGroupApi(@ComponentImport RepositoryService repositoryService,
                               PullRequestGroupProvider pullRequestGroupProvider) {
        this.repositoryService = repositoryService;
        this.pullRequestGroupProvider = pullRequestGroupProvider;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/{projectKey}/{repositorySlug}")
    public Response getPermittedGroupsWithMembers(@PathParam("projectKey") String projectKey, @PathParam("repositorySlug") String repositorySlug) {
        try {
            Repository repository = getRepository(projectKey, repositorySlug);
            return Response.ok(new Gson().toJson(pullRequestGroupProvider.getPermittedGroups(repository))).build();
        }
        catch (Exception e) {
            return commonApiExceptionResponse(e);
        }
    }

    private Repository getRepository(String projectKey, String repositorySlug) throws AuthorisationException, RepositoryDoesNotExistException {
        // Bitbucket will check whether or not the user has access to get the repository object
        Repository repository =  repositoryService.getBySlug(projectKey, repositorySlug);

        if (repository == null)
            throw new RepositoryDoesNotExistException();

        return repository;
    }

    private Response commonApiExceptionResponse(Exception e) {
        if (e instanceof AuthorisationException)
            return Response.status(401).entity(new RestErrorMessage("You do not have permission to perform this operation or access a resource")).build();
        else if (e instanceof RepositoryDoesNotExistException)
            return Response.status(404).entity(new RestErrorMessage("The resource does not exist")).build();
        else {
            log.error("PRGroup - Unhandled Exception occurred!", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new RestErrorMessage("Internal Server Error. Please contact your administrator & check server logs for details"))
                    .build();
        }
    }
}

