package com.cyanoth.prgroup.config;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("PluginConfigDao")
public class PluginConfigDaoImpl implements PluginConfigDao {
    private static final Logger log = LoggerFactory.getLogger(PluginConfigDaoImpl.class);
    private static final Query SELECT_CONFIG_QUERY = Query.select().where("ID = ?", 1);

    private final ActiveObjects ao;


    @Autowired
    public PluginConfigDaoImpl(@ComponentImport ActiveObjects ao) {
        this.ao = ao;
    }

    @Override
    public PluginConfigEntity getPluginSettings() {
        PluginConfigEntity[] pluginConfigEntities = ao.find(PluginConfigEntity.class, SELECT_CONFIG_QUERY);

        if (pluginConfigEntities.length == 0) {
            PluginConfigEntity defaultConfig = ao.create(PluginConfigEntity.class);
            defaultConfig.setMaxGroupSize(PluginConfigDefaultValues.DEFAULT_MAX_GROUP_SIZE);
            defaultConfig.setHideTruncatedGroups(PluginConfigDefaultValues.DEFAULT_HIDE_TRUNCATED_GROUPS);
            updateSettings(defaultConfig);
            return defaultConfig;
        }
        else {
            // ID is the primary key, there can never be more than one record with ID=1. No need to check length >1
            return pluginConfigEntities[0];
        }
    }

    @Override
    public void updateSettings(PluginConfigEntity incomingConfig) {
        PluginConfigEntity[] existingConfig = ao.find(PluginConfigEntity.class, SELECT_CONFIG_QUERY);

        if (existingConfig.length == 0) {
            incomingConfig.save();
            log.info("New PluginConfig for Pull Request Group plugin has been created!");
        }
        else {
            PluginConfigEntity updatedConfig = existingConfig[0];
            updatedConfig.setMaxGroupSize(incomingConfig.getMaxGroupSize());
            updatedConfig.setHideTruncatedGroups(incomingConfig.getHideTruncatedGroups());
            updatedConfig.save();
            log.info("Pull Request Group plugin PluginConfig Updated!");
        }
    }
}
