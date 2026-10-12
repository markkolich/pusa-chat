/*
 * Copyright (c) 2026 Mark S. Kolich
 * https://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package pusachat.components.freemarker;

import com.google.common.collect.ImmutableMap;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.core.servlet.ServletContext;
import curacao.freemarker.components.AbstractFreeMarkerContentRenderer;
import com.kolich.buildversion.BuildVersion;
import pusachat.components.config.PusaChatConfig;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Renders Pusa Chat's templates, adding the attributes every page can use.
 */
@Component
public final class FreeMarkerContentRenderer extends AbstractFreeMarkerContentRenderer {

    private static final String BUILD_VERSION_ATTR = "buildVersion";
    private static final String CONTEXT_PATH_ATTR = "contextPath";
    private static final String DEV_MODE_ATTR = "devMode";

    private final Map<String, Object> globalDataMap_;

    @Injectable
    public FreeMarkerContentRenderer(
            final ServletContext servletContext,
            final PusaChatConfig pusaChatConfig,
            final PusaChatFreeMarkerConfig pusaChatFreeMarkerConfig) {
        super(pusaChatFreeMarkerConfig.getFreeMarkerConfig());
        globalDataMap_ = ImmutableMap.of(
                BUILD_VERSION_ATTR, BuildVersion.getInstance(),
                CONTEXT_PATH_ATTR, servletContext.getContextPath(),
                DEV_MODE_ATTR, pusaChatConfig.isDevMode());
    }

    @Nonnull
    @Override
    public Map<String, Object> getGlobalDataMap() {
        return globalDataMap_;
    }

}
