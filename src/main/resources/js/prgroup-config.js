define('PrGroup/Config', [
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
    'use strict'; {
    }

    let MAX_GROUP_SIZE_LOWER_LIMIT = 1;
    let MAX_GROUP_SIZE_UPPER_LIMIT = 5000;

    exports.onReady = function () {
        console.log("Client-sided JavaScript for PRGroup was loaded successfully!");
        setClickHandlers();
    };

    function setClickHandlers() {
        $("#prgroup-cancel-button").on("click", function(e) {
            e.preventDefault();
            window.history.back();
        })

        $("#prgroup-save-button").on("click", function(e) {
            e.preventDefault();
            saveSettings();
        })
    }


    function saveSettings() {
        $("#prgroup-save-button").get(0).busy();
        let maxGroupSizeVal = $("#prgroup-config-maxgroup-size-text").val();
        let hideTruncatedGroups = $("#prgroup-config-hide-truncated-checkbox").is(":checked")

        // this is server-side checked also.. It would be good to replace this with the message from 400 response.
        // that way we don't need to duplicate the range in the javascript & handle it all server-side...
        if (isNaN(maxGroupSizeVal) || (maxGroupSizeVal < MAX_GROUP_SIZE_LOWER_LIMIT || maxGroupSizeVal > MAX_GROUP_SIZE_UPPER_LIMIT)) {
            AJS.flag({
                type: 'warning',
                close: 'auto',
                title: 'Invalid Value!',
                body: 'The value for Max Group Size must be between ' + MAX_GROUP_SIZE_LOWER_LIMIT + "-" + MAX_GROUP_SIZE_UPPER_LIMIT
            });
            $("#prgroup-save-button").get(0).idle();
            return;
        }

        $.ajax({
            url: navbuilder.rest("prgroup").addPathComponents("config").build(),
            type: "POST",
            data: JSON.stringify({
                "max_group_size": maxGroupSizeVal,
                "hide_truncated_groups": hideTruncatedGroups
            }),
            dataType: "json",
            contentType: "application/json",
            success: function () {
                AJS.flag({
                    type: 'success',
                    close: 'auto',
                    title: "Success!",
                    body: 'Configuration has been saved!',
                });
            },
            error: function(jqxhr, textStatus, error) {
                AJS.flag({
                    type: 'error',
                    close: 'auto',
                    title: 'Error!',
                    body: 'Failed to save configuration! Please consult the browser console or server logs for more information'
                });
                console.log("Error occurred during saving Config. Status Code: " + jqxhr.status + " || Error: " + error)
            },
            complete: function() {
                $("#prgroup-save-button").get(0).idle();
            }
        });
    }

});

jQuery(document).ready(function () {
    console.log("Loading client-sided JavaScript for PRGroup...");
    require('PrGroup/Config').onReady();
});