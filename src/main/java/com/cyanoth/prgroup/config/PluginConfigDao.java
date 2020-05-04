package com.cyanoth.prgroup.config;

import com.atlassian.activeobjects.tx.Transactional;
import net.java.ao.Implementation;

@Transactional
@Implementation(PluginConfigDaoImpl.class)
public interface PluginConfigDao {

    PluginConfigEntity getPluginSettings();

    void updateSettings(PluginConfigEntity pluginConfigEntity);
}
