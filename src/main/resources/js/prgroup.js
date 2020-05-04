let templatePrGroupRow = '<tr>' +
                             '<td><input type="checkbox" class="checkbox-selected-prgroup" name="${group_display_name}" title="${checkbox_title}" ${disabled_flag}></td>' +
                             '<td><img class="prgroup-icon" src="${prgroup_icon_url}" alt="pr_group_icon"/></td>' +
                             '<td>${start_link}${group_display_name}${end_link}</td>' +
                             '<td>${start_link}${member_count}${end_link}</td>' +
                         '</tr>'

let templateMemberRow = '<tr>' +
                            '<td><img class="member-avatar" src="${user_avatar_url}" alt="pr_group_icon"/></td>' +
                            '<td><a href="${member_link}" target="_blank">${user_display_name}</a></td>' +
                        '</tr>'

define('PrGroup', [
    'jquery',
    'bitbucket/util/state',
    'bitbucket/util/navbuilder',
    'bitbucket/util/server',
    'exports'
], function($,
            state,
            navbuilder,
            server,
            exports) {
    'use strict';

    let loadedTargetProjectKey = null;
    let loadedTargetRepositorySlug = null;
    let loadedData = [];

    let isLoadingGroups = false;
    let hasGroupsLoaded = false;

    exports.onReady = function () {
        togglePrGroupSection(false)

        // This plugin is currently only supported on creating a pull-request. Not editing a pull-request.
        if ($(".page-panel-content-header").first().text() === "Create pull request") {
            addButton()
        }

        console.log("Client-sided JavaScript for PRGroup was loaded successfully!");
    };

    function togglePrGroupSection(visible) {
        $("#prgroups-section").toggle(visible);
        $("#loadspinner").toggle(!visible);
    }

    function addButton() {
        $("#reviewers").after('<br/><button class="aui-button" id="add-group-button">Add Reviewers From Group</button>');
        $("#add-group-button").on("click", function(e) {
            e.preventDefault();
            AJS.dialog2($("#select-group-dialog")).show();
            loadDialog();
        });

        $("#cancel-prgroup-dialog-button").on("click", function(e) {
            e.preventDefault();
            AJS.dialog2($("#select-group-dialog")).hide();
        });

        $("#confirm-prgroup-dialog-button").on("click", function(e) {
            e.preventDefault();
            addReviewers();
        });

        $("#select-all-prgroup-dialog-button").on("click", function(e) {
            e.preventDefault();
            $('.checkbox-selected-prgroup:not([disabled])').prop('checked', true);
        });

        $("#clear-all-prgroup-dialog-button").on("click", function(e) {
            e.preventDefault();
            $('.checkbox-selected-prgroup').prop('checked', false);
        });

        $("#prgroups-table").on("click", ".show-member-link", function() {
            showMemberInfoDialog($(this).attr("data-name"));
        });

        $("#member-info-close-dialog-button").on("click", function(e) {
            e.preventDefault();
            AJS.dialog2($("#member-info-dialog")).hide();
        });
    }

    function loadDialog() {
        if (isLoadingGroups)
            return;

        isLoadingGroups = true;

        // Handle the user changing their mind, going back and selecting a different target.
        let selectedTarget = JSON.parse($("#targetRepo .repository").attr("data-repository"));
        let selectedProjectKey = selectedTarget["project"]["key"];
        let selectedRepositorySlug = selectedTarget["slug"]

        if (loadedTargetProjectKey !== selectedProjectKey ||  loadedTargetRepositorySlug !== selectedRepositorySlug)
            resetDialog();

        if (hasGroupsLoaded)
            return;

        // Get a list of project & repository permission groups
        server.rest({
            url: navbuilder.rest("prgroup").addPathComponents(selectedProjectKey, selectedRepositorySlug).build(),
            type: 'GET',
            async: true,
            timeout: 60000,
            success: function (data, textStatus, jqXHR) {
                if (jqXHR.status === 200) {
                    loadedData = data;
                    populateDialog(loadedData)
                    loadedTargetProjectKey = selectedProjectKey
                    loadedTargetRepositorySlug = selectedRepositorySlug
                    hasGroupsLoaded = true;
                    isLoadingGroups = false;
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                AJS.dialog2($("#select-group-dialog")).hide();
                AJS.flag({
                    type: "error",
                    close: 'auto',
                    title: "Failed to load Pull Request Groups",
                    body: "An error occurred loading pull request groups. Please see browser console for more information"
                });
                resetDialog()
                isLoadingGroups = false;
                console.log("Error loading PR Groups: " + jqXHR.status + " || Error: " + jqXHR.responseText)
            },
            statusCode: {
                // Stop bitbucket default error handling (otherwise misleading dialog presented to the user)
                400: false,
                404: false,
                500: false
            }
        });
    }

    function populateDialog(data) {
        $(".no-groups-message").toggle((data.length === 0));
        $("#prgroups-table-container").toggle((data.length !== 0));

        // Add a table on the dialog, with each group. If a group has its members truncated, it cannot be selected. Disable checkbox & put a tooltip
        data.forEach(function(group) {
            group["member_count"] = group["group_members"].length;
            if (group["members_truncated"]) {
                group["member_count"] = "> " + group["group_members"].length + " <em>(not selectable)</em>"
                group["checkbox_title"] = "There are too many members in this group. It cannot be selected."
                group["disabled_flag"] = "disabled"
                group["start_link"] = ""
                group["end_link"] = ""
            }
            else {
                group["member_count"] = group["group_members"].length;
                group["checkbox_title"] = ""
                group["disabled_flag"] = ""
                group["start_link"] = '<a class="show-member-link" data-name="'+ group["group_display_name"] + '" href="#">'
                group["end_link"] = '</a>'
            }

            group["prgroup_icon_url"] = $("#prgroup-icon").attr("src");
            $('#prgroups-table-container').find('tbody:last').append(strTemplate(templatePrGroupRow, group));
        });
        togglePrGroupSection(true)
    }

    function addReviewers() {
        $("#confirm-prgroup-dialog-button").get(0).busy();
        let oldData = $("#reviewers").select2('data');

        try {
            // We combine all the individual reviewers already selected plus the members
            // of the group. Then replace the selected reviewers with the result.
            // Fortunately, select2 handles the set for us already - so any duplicates is handled (we don't need to check if already a reviewer)
            let selectedReviewers = [];
            selectedReviewers = getExistingReviewers();

            $('.checkbox-selected-prgroup:checked').each(function () { // For each selected group
                let selectedGroupName = $(this).attr("name");
                loadedData.forEach(function (loadedGroup) {
                    if (selectedGroupName === loadedGroup["group_display_name"]) {  // Find the group by name in the loaded data
                        loadedGroup["group_members"].forEach(function (member) {
                            // The current user cannot be a reviewer of the same PR. So if a selected group contains
                            // don't add them as a reviewer
                            if (member["user_name"] === state.getCurrentUser().name)
                                return;

                            console.log("Adding: " + member["user_name"] + " as a reviewer");
                            selectedReviewers.push(userInfoToAjsSelect2DataObj(member["user_name"], member["user_display_name"], member["user_avatar_url"]))
                        });
                    }
                });
            });

            $("#reviewers").select2('data', selectedReviewers);
        }
        catch (err) {
            AJS.flag({
                type: "error",
                close: 'auto',
                title: "Failed to add group reviewers",
                body: "An error occurred adding group reviewers. Please see browser console for more information."
            });
            $("#reviewers").select2('data', oldData);
        }

        $("#confirm-prgroup-dialog-button").get(0).idle();
        AJS.dialog2($("#select-group-dialog")).hide();
    }

    function resetDialog() {
        hasGroupsLoaded = false;
        $("#prgroups-table tbody").empty().append('<tr style="display: none;"></tr>');
        togglePrGroupSection(false)
    }

    function strTemplate(str, data) {
        var regex = new RegExp('\\${(' + Object.keys(data).join('|') + ')}', 'gi');
        str = str.replace(regex, function(matched) {
            return data[matched.replace("$", "").replace("{", "").replace("}", "")];
        });
        return str;
    }

    function getExistingReviewers() {
        let existingReviewers = [];
        $("#s2id_reviewers .select2-search-choice").each(function () {
            let displayName =  $(this).find(".avatar-with-name").attr("title");
            let slug = $(this).find(".user-avatar").attr("data-username");
            let avatarUrl = $(this).find(".aui-avatar-inner img").attr("src");
            existingReviewers.push(userInfoToAjsSelect2DataObj(slug, displayName, avatarUrl));
        });

        return existingReviewers;
    }

    function userInfoToAjsSelect2DataObj(userName, displayName, avatarUrl) {
        // Convert user information to an object that the reviewer box understands.
        // Some fields are not actually required, just its a common select box.
        // Reviewers only cares about slug, displayName and avatar
        return {
            id: userName,
            item: {
                active: "true",
                avatarUrl: avatarUrl,
                displayName: displayName,
                emailAddress: "not_applicable_for_reviewers",
                id: "not_applicable_for_reviewers",
                links: [], // Not applicable for reviewers
                name: displayName,
                slug: userName, // Yes, this is correct. slug === userName. See: PullRequestGroupProvider getPullRequestGroupWithMembers
                type: "NORMAL"
            },
            text: displayName
        }
    }

    function showMemberInfoDialog(groupName) {
        $("#member-info-table tbody").empty().append('<tr style="display: none;"></tr>');
        AJS.dialog2($("#member-info-dialog")).show();
        $("#member-info-heading-text").text("Group Members: " + groupName)

        loadedData.forEach(function (loadedGroup) {
            if (groupName === loadedGroup["group_display_name"]) {  // Find the group by name in the loaded data
                $(".no-members-info-message").toggle((loadedGroup["group_members"].length === 0));
                $("#member-info-table-container").toggle((loadedGroup["group_members"].length !== 0));

                loadedGroup["group_members"].forEach(function (member) {
                    member["member_link"] = navbuilder.user(member["user_name"]).buildAbsolute();
                    $('#member-info-table-container').find('tbody:last').append(strTemplate(templateMemberRow, member));
                });
            }
        });
    }
});

jQuery(document).ready(function () {
    console.log("Loading client-sided JavaScript for PRGroup...");
    require('PrGroup').onReady();
});