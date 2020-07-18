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

const add_group_button_element = '<br/><button class="aui-button" type="button" id="add-group-button">Add Reviewers From Group</button>'

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

    const PrModes = {
        CREATE: "CREATE",       // Creating a Pull-Request (Works both BBS6 & BBS7)
        EDIT_BB7: "EDIT_BB7",   // Editing a pull-request (Specific for Bitbucket 7)
        EDIT_BB6: "EDIT_BB6"    // Editing a pull-request (Specific for Bitbucket 6)
    }

    let PrMode;

    let loadedTargetProjectKey = null;
    let loadedTargetRepositorySlug = null;

    let loadedData = [];
    let isLoadingGroups = false;

    let hasGroupsLoaded = false;

    exports.onReady = function () {
        togglePrGroupSection(false)

        // Logic works the same in both Bitbucket 6 & 7: Add button to Create Pull Request Page
        let bb6_ele = $(".aui-page-header-main").children("h2").first().text()
        let bb7_ele = $(".page-panel-content-header").first().text()
        let find_text= "Create pull request"

        if (bb6_ele === find_text || bb7_ele === find_text) {
            $("#reviewers").after(add_group_button_element);
            addHandlers()
            PrMode = PrModes.CREATE
        }
        else {
            let bb_major_version = get_bitbucket_major_version()
            if (bb_major_version === "7") {
                watchForEditPrDialog("7")
                PrMode = PrModes.EDIT_BB7;
            }
            else if (bb_major_version === "6") {
                watchForEditPrDialog("6")
                PrMode = PrModes.EDIT_BB6;
            }
            else {
                console.log("PRGroup Reviewer - Unsupported Bitbucket Version.")
            }
        }
        console.log("Client-sided JavaScript for PRGroup was loaded successfully!");
    };

    function get_bitbucket_major_version() {
        if ($("#product-version").length ) {
            let val = $("#product-version").text()
            // v6.10.2 -> 6
            return val.trim().substr(1, (val.indexOf('.') - 2));
        }
        else {
            // This is not future proof; The new PR interface doesn't have a DOM element containing the
            // Bitbucket Version, so we _assume_ if that element is not there its bitbucket 7.
            return "7"
        }
    }

    function watchForEditPrDialog(bitbucket_version) {
        // Observe the DOM and wait for the edit dialog to show before injecting the 'Add Review Group' button.
        let watchClass = ((bitbucket_version === "6") ? "aui-layer" : "atlaskit-portal-container")

        let observer = new MutationObserver(function(mutations) {
            for(let mutation of mutations) {
                if (mutation.type === "childList" && mutation.target.className === watchClass) {
                    // A dialog has been added to the page. Check if its the edit pull request dialog
                    if (mutation.addedNodes.length === 1) {
                        let selector = ((bitbucket_version === "6") ? $("h2:contains('Edit Pull Request')") : $("span:contains('Edit Pull Request')"))
                        if (selector.length === 1) {
                            // This event was to show the edit pull request dialog, so add the pr group button
                            // Want to put the button in the same div as the reviewer help text (match by text, always last)
                            let addButtonTarget = $("div:contains('Reviewers can approve a pull request to let others know when it is good to merge')").last();
                            $(addButtonTarget).before(add_group_button_element);
                            addHandlers();
                         }
                     }
                }
            }
        })

        observer.observe(document.body, {
            childList: true,
            subtree: true
        })
    }

    function togglePrGroupSection(visible) {
        $("#prgroups-section").toggle(visible);
        $("#loadspinner").toggle(!visible);
    }

    function addHandlers() {
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
        let selectedProjectKey;
        let selectedRepositorySlug;

        if (PrMode === PrModes.CREATE) {
            // Handle the user changing their mind, going back and selecting a different target.
            let selectedTarget = JSON.parse($("#targetRepo .repository").attr("data-repository"));
            selectedProjectKey = selectedTarget["project"]["key"];
            selectedRepositorySlug = selectedTarget["slug"]

            if (loadedTargetProjectKey !== selectedProjectKey || loadedTargetRepositorySlug !== selectedRepositorySlug)
                resetDialog();
        }
        else if (PrMode === PrModes.EDIT_BB7 || PrMode === PrModes.EDIT_BB6) {
            // state.getPullRequest() - no longer exists in BBS 7.x, so don't use it.
            selectedProjectKey = state.getRepository()["project"]["key"];
            selectedRepositorySlug = state.getRepository()["slug"]
        }
        else {
            console.log("PRGroup-Reviewers: Error loading dialog, unknown PR mode!")
            AJS.dialog2($("#select-group-dialog")).hide();
            isLoadingGroups = false;
            return
        }

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
                // Stop bitbucket default error handling (otherwise a misleading dialog could be presented to the user)
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

    function getSelectedMembers() {
        let selectedReviewers = [];

        $('.checkbox-selected-prgroup:checked').each(function () { // For each selected group
            let selectedGroupName = $(this).attr("name");
            loadedData.forEach(function (loadedGroup) {
                if (selectedGroupName === loadedGroup["group_display_name"]) {  // Find the group by name in the loaded data
                    loadedGroup["group_members"].forEach(function (member) {
                        // The current user cannot be a reviewer of the same PR. So if a selected group contains don't add them as a reviewer
                        if (member["user_name"] === state.getCurrentUser().name)
                            return;

                        console.log("Adding: " + member["user_name"] + " as a reviewer");
                        if (PrMode === PrModes.CREATE || PrMode === PrModes.EDIT_BB6) {
                            selectedReviewers.push(userInfoToAjsSelect2DataObj(member["user_name"],
                                member["user_display_name"], member["user_avatar_url"]));
                        }
                        else if (PrMode === PrModes.EDIT_BB7) {
                            selectedReviewers.push(userInfoToReactSelect(member["user_id"], member["user_name"],
                                member["user_display_name"], member["user_avatar_url"]));
                        }
                        else {
                            throw new Error("PRGroup-Reviewers: Unknown PR Mode")
                        }
                    });
                }
            });
        });

        return selectedReviewers;
    }

    function addReviewersSelect2() {
        // This is supported both Bitbucket 6 & 7
        let oldData = $("#reviewers").select2('data');
        // We combine all the individual reviewers already selected plus the members
        // of the group. Then replace the selected reviewers with the result.
        // Fortunately, select2 handles the set for us already - so any duplicates is handled (we don't need to check if already a reviewer)
        try {
            let previousReviewers = [];
            $("#s2id_reviewers .select2-search-choice").each(function () {
                let displayName =  $(this).find(".avatar-with-name").attr("title");
                let slug = $(this).find(".user-avatar").attr("data-username");
                let avatarUrl = $(this).find(".aui-avatar-inner img").attr("src");
                previousReviewers.push(userInfoToAjsSelect2DataObj(slug, displayName, avatarUrl));
            });

            let newReviewers = getSelectedMembers()
            let allReviewers = previousReviewers.concat(newReviewers)
            $("#reviewers").select2('data', allReviewers);

            if (PrMode === PrModes.EDIT_BB6) {
                // On the Bitbucket PR Edit Modal Dialog for Bitbucket 6, the selected reviewer list
                //  is stored in some internal array (searchable-multi-selector.js _selectedItems)
                // The array is only updated on change events. The change event is only considered if the element
                // has the data property: select2-change-triggered set to true. Otherwise it clears the list (re-init?)
                // So set that data property, for each selected reviewer - fire the change event and unset the property.
                // Ref: https://bitbucket.org/atlassian/aui/src/master/packages/core/src/js-vendor/jquery/plugins/jquery.select2.js
                // Ref: /bitbucket-parent/webapp/default/src/main/frontend/static/bitbucket/internal/widget/searchable-multi-selector/searchable-multi-selector.js Line 29
                $("#reviewers").data("select2-change-triggered", true);
                allReviewers.forEach(function (reviewerSelect2Data) {
                    $("#reviewers").trigger(jQuery.Event("change", { added: reviewerSelect2Data }))
                });
                $("#reviewers").data("select2-change-triggered", false);
            }
        }
        catch (err) {
            // On error, revert back to the previous selected reviewers & let caller handle
            $("#reviewers").select2('data', oldData);
            if (PrMode === PrModes.EDIT_BB6)
                $("#reviewers").data("select2-change-triggered", false);

            throw err
        }
    }

    function addReviewersReact() {
        let reviewer_selector;
        let reviewer_select_state;
        let previousReviewers;

        try {
            reviewer_selector = $(".user-multi-select")[0] // can't use $("#reviewers-uidXX") ! Where XX is a seemingly a random number with no obvious pattern.
            // noinspection TypeScriptValidateTypes,TypeScriptValidateTypes,TypeScriptValidateJSTypes,JSValidateTypes
            reviewer_select_state = Object.keys($(reviewer_selector).parent()[0]).find(key => key.startsWith("__reactEventHandlers$"));
            previousReviewers = $(reviewer_selector)[0][reviewer_select_state]["children"][1]["props"].getValue()

            let newReviewers = getSelectedMembers()
            let allReviewers = previousReviewers.concat(newReviewers)
            $(reviewer_selector)[0][reviewer_select_state]["children"][1]["props"].setValue(allReviewers)
        }
        catch (err) {
            // On error, attempt to revert back to the previous selected reviewers & let caller handle exception
            try {
                $(reviewer_selector)[0][reviewer_select_state]["children"][1]["props"].setValue(previousReviewers)
            }
            catch (err){
                console.log("Failed to reviewers back to the previously selected reviewers!")
            }
            throw err
        }
    }

    function addReviewers() {
        try {
            $("#confirm-prgroup-dialog-button").get(0).busy();
            if (PrMode === PrModes.CREATE || PrMode === PrModes.EDIT_BB6) {
                addReviewersSelect2();
            }
            else if (PrMode === PrModes.EDIT_BB7) {
                addReviewersReact();
            }
            else {
                throw new Error("PRGroup-Reviewers: Unknown PR Mode")
            }
        }
        catch (err) {
            AJS.flag({
                type: "error",
                close: 'auto',
                title: "Failed to add group reviewers",
                body: "An error occurred adding group reviewers. Please see browser console for more information."
            });
            console.log(err)
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


    function userInfoToAjsSelect2DataObj(username, displayName, avatarUrl) {
        // Convert user information to an object that the reviewer box understands.
        // Some fields are not actually required, just its a common select box.
        // Reviewers only cares about slug, displayName and avatar
        return {
            id: username,
            item: {
                active: "true",
                avatarUrl: avatarUrl,
                displayName: displayName,
                emailAddress: "ignored_for_select2",
                id: "ignored_for_select2",
                links: [], // Not applicable for reviewers
                name: displayName,
                slug: username, // Yes, this is correct. slug === userName. See: PullRequestGroupProvider getPullRequestGroupWithMembers
                type: "NORMAL"
            },
            text: displayName
        }
    }

    function userInfoToReactSelect(userId, username, displayName, avatarUrl) {
        // Convert user information to an object that the react select box understands.
        return {
            label: username,
            user: {
                name: username,
                emailAddress: "ignored_for_react_select",
                id: userId,
                displayName: displayName,
                active: true,
                slug: username,
                type: "NORMAL",
                links: { "self": [ { } ]},
                avatarUrl: avatarUrl
            },
            value: userId
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