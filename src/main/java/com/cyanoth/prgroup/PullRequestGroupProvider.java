package com.cyanoth.prgroup;

import com.atlassian.bitbucket.avatar.AvatarRequest;
import com.atlassian.bitbucket.avatar.AvatarService;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionAdminService;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.permission.PermittedGroup;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.rest.enrich.AvatarEnricher;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.bitbucket.util.PagedIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;

@Component("PullRequestGroupProvider")
public class PullRequestGroupProvider {
    private final UserService userService;
    private final PermissionService permissionService;
    private final PermissionAdminService permissionAdminService;
    private final SecurityService securityService;
    private final AvatarService avatarService;
    private final NavBuilder navBuilder;

    @Autowired
    public PullRequestGroupProvider(@ComponentImport UserService userService,
                                    @ComponentImport PermissionService permissionService,
                                    @ComponentImport PermissionAdminService permissionAdminService,
                                    @ComponentImport SecurityService securityService,
                                    @ComponentImport AvatarService avatarService,
                                    @ComponentImport NavBuilder navBuilder) {
        this.userService = userService;
        this.permissionAdminService = permissionAdminService;
        this.permissionService = permissionService;
        this.securityService = securityService;
        this.avatarService = avatarService;
        this.navBuilder = navBuilder;
    }

    public ArrayList<PullRequestGroup> getPermittedGroups(Repository repository) throws Exception {
        ArrayList<PullRequestGroup> permittedGroups = new ArrayList<>();
        for (String groupName: getPermittedGroupNames(repository)) {
            permittedGroups.add(getPullRequestGroupWithMembers(groupName));
        }
        return permittedGroups;
    }

    private HashSet<String> getPermittedGroupNames(Repository repository) throws Exception {
        return securityService.withPermission(Permission.PROJECT_ADMIN, "Getting groups with permission to repository.")
            .call((Operation<HashSet<String>, Exception>) () -> {
                HashSet<String> groups = new HashSet<>();
                // Project Groups
                for (PermittedGroup permittedGroup : new PagedIterable<>(request ->
                        permissionAdminService.findGroupsWithProjectPermission(repository.getProject(), null, request),
                        new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT))) {
                    groups.add(permittedGroup.getGroup());
                }

                // Repository Groups
                for (PermittedGroup permittedGroup : new PagedIterable<>(request ->
                        permissionAdminService.findGroupsWithRepositoryPermission(repository, null, request),
                        new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT))) {
                    groups.add(permittedGroup.getGroup());
                }

                return groups;
            });
    }

    private PullRequestGroup getPullRequestGroupWithMembers(String groupName) throws Exception {
        PullRequestGroup pullRequestGroup = new PullRequestGroup(groupName);
        boolean useHttps = navBuilder.buildBaseUrl().startsWith("https");

        securityService.withPermission(Permission.ADMIN, "Getting members of a group")
                .call((Operation<Boolean, Exception>) () -> {
                    for (ApplicationUser applicationUser : new PagedIterable<>(request ->
                            userService.findUsersByGroup(groupName, request), (PluginProperties.MAX_MEMBERS_IN_GROUP + 1))) { // +1 to trip the truncated flag

                        if (pullRequestGroup.isTruncated())
                            break;

                        // Special case: Users may exist in the AD group but they might not have access to Bitbucket.
                        // So we only want to users if they are permitted to access Bitbucket... isActive() is not enough on its own!
                        // Also use .getName() ! NOT ! .getSlug() because pull-request reviewer does getUsersByName(...) and slug is always lowercase
                        // Using slug instead of name will cause issues if the user has a capital letter in their name
                        if (permissionService.hasGlobalPermission(applicationUser, Permission.LICENSED_USER) && applicationUser.isActive()) {
                            pullRequestGroup.addGroupMember(new PullRequestGroupMember(applicationUser.getDisplayName(), applicationUser.getName(),
                                    avatarService.getUrlForPerson(applicationUser, new AvatarRequest(useHttps, 48, true))));
                        }
                    }
                    return true;
            });
        return pullRequestGroup;
    }
}
