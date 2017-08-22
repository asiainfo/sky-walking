package org.skywalking.apm.plugin.dubbox;

/**
 * {@link BugFixActive#active} is an flag that present the dubbox version is below 2.8.3, The version 2.8.3 of dubbox
 * don't support attachment. so skywalking provided another way to support the function that transport the serialized
 * context data. The way is that all parameters of dubbo service need to extend {@link SWBaseBean}, {@link
 * org.skywalking.apm.plugin.dubbo.DubboInterceptor} fetch the serialized context data by using {@link
 * SWBaseBean#getTraceContext()}.
 *
 * @author zhangxin
 */
public final class BugFixActive {

    private static boolean ACTIVE = false;

    /**
     * Set active status, before startup dubbo remote.
     */
    public static void active() {
        BugFixActive.ACTIVE = true;
    }

    public static boolean isActive() {
        return BugFixActive.ACTIVE;
    }

}
