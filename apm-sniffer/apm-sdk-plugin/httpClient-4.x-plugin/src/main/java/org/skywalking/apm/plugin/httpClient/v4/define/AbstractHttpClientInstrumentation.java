package org.skywalking.apm.plugin.httpClient.v4.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * {@link AbstractHttpClientInstrumentation} presents that skywalking intercepts
 * {@link org.apache.http.impl.client.AbstractHttpClient#doExecute(HttpHost, HttpRequest, HttpContext)}
 * by using {@link HttpClientInstrumentation#INTERCEPT_CLASS}.
 *
 * @author zhangxin
 */
public class AbstractHttpClientInstrumentation extends HttpClientInstrumentation {

    private static final String ENHANCE_CLASS = "org.apache.http.impl.client.AbstractHttpClient";

    @Override
    public ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    /**
     * version 4.2, intercept method: execute, intercept<br/>
     * public final HttpResponse execute(HttpHost target, HttpRequest request,
     * HttpContext context)<br/>
     */
    @Override
    protected InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("doExecute");
                }

                @Override
                public String getMethodsInterceptor() {
                    return getInstanceMethodsInterceptor();
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
