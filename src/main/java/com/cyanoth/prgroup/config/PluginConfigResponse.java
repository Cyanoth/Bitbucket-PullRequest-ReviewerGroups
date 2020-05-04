package com.cyanoth.prgroup.config;

import com.google.gson.annotations.SerializedName;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

@JsonSerialize
public class PluginConfigResponse implements Serializable {
    private static final String JSON_MAX_GROUP_SIZE_PROPERTY_NAME = "max_group_size";
    private static final String JSON_HIDE_TRUNCATED_GROUPS_PROPERTY_NAME = "hide_truncated_groups";

    @JsonProperty(JSON_MAX_GROUP_SIZE_PROPERTY_NAME)
    @SerializedName(JSON_MAX_GROUP_SIZE_PROPERTY_NAME)
    private final int maxGroupSize;

    @JsonProperty(JSON_HIDE_TRUNCATED_GROUPS_PROPERTY_NAME)
    @SerializedName(JSON_HIDE_TRUNCATED_GROUPS_PROPERTY_NAME)
    private final boolean hideTruncatedGroups;


    public PluginConfigResponse(@JsonProperty(JSON_MAX_GROUP_SIZE_PROPERTY_NAME) int maxGroupSize,
                                @JsonProperty(JSON_HIDE_TRUNCATED_GROUPS_PROPERTY_NAME) boolean hideTruncatedGroups) {
        this.maxGroupSize = maxGroupSize;
        this.hideTruncatedGroups = hideTruncatedGroups;
    }

    public PluginConfigResponse(PluginConfigEntity entity) {
        this.maxGroupSize = entity.getMaxGroupSize();
        this.hideTruncatedGroups = entity.getHideTruncatedGroups();
    }

    public int getMaxGroupSize() {
        return maxGroupSize;
    }

    public PluginConfigEntity toEntity(PluginConfigEntity entity) {
        entity.setMaxGroupSize(this.getMaxGroupSize());
        entity.setHideTruncatedGroups(this.hideTruncatedGroups);
        return entity;
    }
}
