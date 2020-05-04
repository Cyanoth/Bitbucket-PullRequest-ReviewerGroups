package com.cyanoth.prgroup.rest;

import com.atlassian.bitbucket.rest.exception.UnhandledExceptionMapper;
import com.atlassian.bitbucket.rest.exception.UnhandledExceptionMapperHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
@Component
public class PullRequestGroupUnhandledRestException  extends UnhandledExceptionMapper {

    @Autowired
    public PullRequestGroupUnhandledRestException(@ComponentImport UnhandledExceptionMapperHelper helper) {
        super(helper);
    }
}