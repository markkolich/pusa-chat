package com.kolich.pusachat.spring;

import static com.google.common.base.Preconditions.checkNotNull;

import com.kolich.spring.beans.KolichWebAppProperties;

public final class PusaChatProperties extends KolichWebAppProperties {
	
	private static final String APP_VERSION_PROPERTY = "pusachat.app.version";
	private static final String APP_MODE_PROPERTY = "pusachat.app.mode";
	private static final String CONTEXT_PATH_PROPERTY = "pusachat.context.path";
	
	private static final String MODE_PRODUCTION = "production";

	@Override
	public void afterPropertiesSet() throws Exception {
		checkNotNull(getAppVersion(), "Application version cannot be null.");
		checkNotNull(getAppMode(), "Application mode cannot be null.");
		checkNotNull(getContextPath(), "Context path cannot be null.");
	}
	
	public final String getAppVersion() {
		return (String)getProperty(APP_VERSION_PROPERTY);
	}
	
	public final String getAppMode() {
		return (String)getProperty(APP_MODE_PROPERTY);
	}
	
	public final boolean isProductionMode() {
		return MODE_PRODUCTION.equals(getAppMode());
	}
	
	public final String getContextPath() {
		return (String)getProperty(CONTEXT_PATH_PROPERTY);
	}

}