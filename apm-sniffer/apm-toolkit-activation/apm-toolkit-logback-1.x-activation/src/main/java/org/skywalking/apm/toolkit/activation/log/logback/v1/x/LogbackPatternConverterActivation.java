package org.skywalking.apm.toolkit.activation.log.logback.v1.x;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * Active the toolkit class "org.skywalking.apm.toolkit.log.logback.v1.x.LogbackPatternConverter".
 * Should not dependency or import any class in "skywalking-toolkit-logback-1.x" module.
 * Activation's classloader is diff from "org.skywalking.apm.toolkit.log.logback.v1.x.LogbackPatternConverter",
 * using direct will trigger classloader issue.
 * <p>
 * Created by wusheng on 2016/12/7.
 */
public class LogbackPatternConverterActivation extends ClassInstanceMethodsEnhancePluginDefine {
    /**
     * @return the target class, which needs active.
     */
    @Override
    protected ClassMatch enhanceClass() {
        return byName("org.skywalking.apm.toolkit.log.logback.v1.x.LogbackPatternConverter");
    }

    /**
     * @return null, no need to intercept constructor of enhance class.
     */
    @Override
    protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return null;
    }

    /**
     * @return the collection of {@link StaticMethodsInterceptPoint}, represent the intercepted methods and their
     * interceptors.
     */
    @Override
    protected InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("convert");
                }

                @Override
                public String getMethodsInterceptor() {
                    return "org.skywalking.apm.toolkit.activation.log.logback.v1.x.PrintTraceIdInterceptor";
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
