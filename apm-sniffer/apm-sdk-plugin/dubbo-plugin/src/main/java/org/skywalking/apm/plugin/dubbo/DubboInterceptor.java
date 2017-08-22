package org.skywalking.apm.plugin.dubbo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import java.lang.reflect.Method;
import org.skywalking.apm.agent.core.conf.Config;
import org.skywalking.apm.agent.core.context.ContextCarrier;
import org.skywalking.apm.agent.core.context.ContextManager;
import org.skywalking.apm.agent.core.context.tag.Tags;
import org.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.skywalking.apm.network.trace.component.ComponentsDefine;
import org.skywalking.apm.plugin.dubbox.BugFixActive;
import org.skywalking.apm.plugin.dubbox.SWBaseBean;

/**
 * {@link DubboInterceptor} define how to enhance class {@link com.alibaba.dubbo.monitor.support.MonitorFilter#invoke(Invoker,
 * Invocation)}. the trace context transport to the provider side by {@link RpcContext#attachments}.but all the version
 * of dubbo framework below 2.8.3 don't support {@link RpcContext#attachments}, we support another way to support it. it
 * is that all request parameters of dubbo service need to extend {@link SWBaseBean}, and {@link DubboInterceptor} will
 * inject the trace context data to the {@link SWBaseBean} bean and extract the trace context data from {@link
 * SWBaseBean}, or the trace context data will not transport to the provider side.
 *
 * @author zhangxin
 */
public class DubboInterceptor implements InstanceMethodsAroundInterceptor {

    /**
     * <h2>Consumer:</h2> The serialized trace context data will inject the first param that extend {@link SWBaseBean}
     * of dubbo service if the method {@link BugFixActive#active()} be called. or the serialized context data will
     * inject to the {@link RpcContext#attachments} for transport to provider side.
     * <p>
     * <h2>Provider:</h2> The serialized trace context data will extract from the first param that extend {@link
     * SWBaseBean} of dubbo service if the method {@link BugFixActive#active()} be called. or it will extract from
     * {@link RpcContext#attachments}. current trace segment will ref if the serialize context data is not null.
     */
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Invoker invoker = (Invoker)allArguments[0];
        Invocation invocation = (Invocation)allArguments[1];
        RpcContext rpcContext = RpcContext.getContext();
        boolean isConsumer = rpcContext.isConsumerSide();
        URL requestURL = invoker.getUrl();

        AbstractSpan span;

        final String host = requestURL.getHost();
        final int port = requestURL.getPort();
        if (isConsumer) {
            final ContextCarrier contextCarrier = new ContextCarrier();
            span = ContextManager.createExitSpan(generateOperationName(requestURL, invocation), contextCarrier, host + ":" + port);
            if (!BugFixActive.isActive()) {
                //invocation.getAttachments().put("contextData", contextDataStr);
                //@see https://github.com/alibaba/dubbo/blob/dubbo-2.5.3/dubbo-rpc/dubbo-rpc-api/src/main/java/com/alibaba/dubbo/rpc/RpcInvocation.java#L154-L161
                rpcContext.getAttachments().put(Config.Plugin.Propagation.HEADER_NAME, contextCarrier.serialize());
            } else {
                fix283SendNoAttachmentIssue(invocation, contextCarrier);
            }
        } else {

            ContextCarrier contextCarrier;
            if (!BugFixActive.isActive()) {
                contextCarrier = new ContextCarrier().deserialize(rpcContext.getAttachment(Config.Plugin.Propagation.HEADER_NAME));
            } else {
                contextCarrier = fix283RecvNoAttachmentIssue(invocation);
            }

            span = ContextManager.createEntrySpan(generateOperationName(requestURL, invocation), contextCarrier);
        }

        Tags.URL.set(span, generateRequestURL(requestURL, invocation));
        span.setComponent(ComponentsDefine.DUBBO);
        SpanLayer.asRPCFramework(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) throws Throwable {
        Result result = (Result)ret;
        if (result != null && result.getException() != null) {
            dealException(result.getException());
        }

        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        dealException(t);
    }

    /**
     * Log the throwable, which occurs in Dubbo RPC service.
     */
    private void dealException(Throwable throwable) {
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(throwable);
    }

    /**
     * Format operation name. e.g. org.skywalking.apm.plugin.test.Test.test(String)
     *
     * @return operation name.
     */
    private String generateOperationName(URL requestURL, Invocation invocation) {
        StringBuilder operationName = new StringBuilder();
        operationName.append(requestURL.getPath());
        operationName.append("." + invocation.getMethodName() + "(");
        for (Class<?> classes : invocation.getParameterTypes()) {
            operationName.append(classes.getSimpleName() + ",");
        }

        if (invocation.getParameterTypes().length > 0) {
            operationName.delete(operationName.length() - 1, operationName.length());
        }

        operationName.append(")");

        return operationName.toString();
    }

    /**
     * Format request url.
     * e.g. dubbo://127.0.0.1:20880/org.skywalking.apm.plugin.test.Test.test(String).
     *
     * @return request url.
     */
    private String generateRequestURL(URL url, Invocation invocation) {
        StringBuilder requestURL = new StringBuilder();
        requestURL.append(url.getProtocol() + "://");
        requestURL.append(url.getHost());
        requestURL.append(":" + url.getPort() + "/");
        requestURL.append(generateOperationName(url, invocation));
        return requestURL.toString();
    }

    /**
     * Set the trace context.
     *
     * @param contextCarrier {@link ContextCarrier}.
     */
    private void fix283SendNoAttachmentIssue(Invocation invocation, ContextCarrier contextCarrier) {
        for (Object parameter : invocation.getArguments()) {
            if (parameter instanceof SWBaseBean) {
                ((SWBaseBean)parameter).setTraceContext(contextCarrier.serialize());
                return;
            }
        }
    }

    /**
     * Fetch the trace context by using {@link Invocation#getArguments()}.
     *
     * @return trace context data.
     */
    private ContextCarrier fix283RecvNoAttachmentIssue(Invocation invocation) {
        for (Object parameter : invocation.getArguments()) {
            if (parameter instanceof SWBaseBean) {
                return new ContextCarrier().deserialize(((SWBaseBean)parameter).getTraceContext());
            }
        }

        return null;
    }
}
