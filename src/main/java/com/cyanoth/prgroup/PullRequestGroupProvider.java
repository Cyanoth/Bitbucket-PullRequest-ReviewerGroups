package com.cyanoth.prgroup;

import com.atlassian.bitbucket.avatar.AvatarRequest;
import com.atlassian.bitbucket.avatar.AvatarService;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionAdminService;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.permission.PermittedGroup;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.bitbucket.util.PagedIterable;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.cyanoth.prgroup.config.PluginConfigDao;
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
    private final PluginConfigDao pluginConfigDao;

    private static final int AVATAR_SIZE_PX = 48;

    @Autowired
    public PullRequestGroupProvider(@ComponentImport UserService userService,
                                    @ComponentImport PermissionService permissionService,
                                    @ComponentImport PermissionAdminService permissionAdminService,
                                    @ComponentImport SecurityService securityService,
                                    @ComponentImport AvatarService avatarService,
                                    @ComponentImport NavBuilder navBuilder,
                                    PluginConfigDao pluginConfigDao) {
        this.userService = userService;
        this.permissionAdminService = permissionAdminService;
        this.permissionService = permissionService;
        this.securityService = securityService;
        this.avatarService = avatarService;
        this.navBuilder = navBuilder;
        this.pluginConfigDao =pluginConfigDao;
    }

    /**
     * @param repository Bitbucket Repository object to get groups details of.
     * @return Array of PullRequestGroup with details about groups (including members) that have permission to the repository (including project groups)
     * @throws Exception General exception is thrown by securityService, must be handled by the caller
     */
    public ArrayList<PullRequestGroup> getPullRequestGroups(Repository repository) throws Exception {
        ArrayList<PullRequestGroup> permittedGroups = new ArrayList<>();
        boolean configHideTruncatedGroups = pluginConfigDao.getPluginSettings().getHideTruncatedGroups();

        for (String groupName: getBitbucketPermittedGroupNames(repository)) {
            PullRequestGroup groupInfo = bitbucketGroupToPullRequestGroup(groupName);

            if (groupInfo.isMembersTruncated() && configHideTruncatedGroups)
                continue;

            permittedGroups.add(groupInfo);
        }
        return permittedGroups;
    }

    /**
     * @param repository Bitbucket Repository object to get groups names of.
     * @return Set of group names that have permission to the repository or the project
     * @throws Exception General exception is thrown by securityService, must be handled by the caller
     */
    private HashSet<String> getBitbucketPermittedGroupNames(Repository repository) throws Exception {
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

    /**
     * @param groupName Name of the Bitbucket group to get details for
     * @return Bitbucket group information with a collection of members belonging to the group (PullRequestGroup).
     * @throws Exception General exception is thrown by securityService, must be handled by the caller
     */
    private PullRequestGroup bitbucketGroupToPullRequestGroup(String groupName) throws Exception {
        return securityService.withPermission(Permission.ADMIN, "Escalate to get members of a group")
            .call((Operation<PullRequestGroup, Exception>) () -> {
                PullRequestGroup pullRequestGroup = new PullRequestGroup(groupName);
                boolean useHttps = navBuilder.buildBaseUrl().startsWith("https");
                int configMaxGroupSize = pluginConfigDao.getPluginSettings().getMaxGroupSize();
                int memberCount = 0;

                for (ApplicationUser applicationUser : new PagedIterable<>(request ->
                        userService.findUsersByGroup(groupName, request), (configMaxGroupSize + 1))) { // +1 to trip the truncated flag

                    if (memberCount >= configMaxGroupSize) {
                        pullRequestGroup.setIsMembersTruncated(true);
                        return pullRequestGroup;
                    }

                    // Some important considerations here: Users may exist in the AD group but they might not have access to Bitbucket.
                    // So we only want to users if they are permitted to access Bitbucket... isActive() is not enough on its own!
                    // Also use .getName() not .getSlug() because pull-request reviewer check does getUsersByName(...) and slug is always lowercase
                    // If we were to use slug instead of username it will cause issues if the user has a capital letter in their username
                    if (permissionService.hasGlobalPermission(applicationUser, Permission.LICENSED_USER) && applicationUser.isActive()) {
                        pullRequestGroup.addGroupMember(new PullRequestGroupMember(applicationUser.getId(), applicationUser.getDisplayName(),
                                applicationUser.getName(), avatarService.getUrlForPerson(applicationUser,
                                new AvatarRequest(useHttps, AVATAR_SIZE_PX, true))));
                        memberCount++;
                    }
                }
                return pullRequestGroup;
            });
    }
}
