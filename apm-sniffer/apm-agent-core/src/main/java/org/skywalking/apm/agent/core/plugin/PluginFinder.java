package org.skywalking.apm.agent.core.plugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.skywalking.apm.agent.core.plugin.bytebuddy.AbstractJunction;
import org.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.skywalking.apm.agent.core.plugin.match.IndirectMatch;
import org.skywalking.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * The <code>PluginFinder</code> represents a finder , which assist to find the one
 * from the given {@link AbstractClassEnhancePluginDefine} list.
 *
 * @author wusheng
 */
public class PluginFinder {
    private final Map<String, AbstractClassEnhancePluginDefine> nameMatchDefine = new HashMap<String, AbstractClassEnhancePluginDefine>();
    private final List<AbstractClassEnhancePluginDefine> signatureMatchDefine = new LinkedList<AbstractClassEnhancePluginDefine>();

    public PluginFinder(List<AbstractClassEnhancePluginDefine> plugins) {
        for (AbstractClassEnhancePluginDefine plugin : plugins) {
            ClassMatch match = plugin.enhanceClass();

            if (match == null) {
                continue;
            }

            if (match instanceof NameMatch) {
                NameMatch nameMatch = (NameMatch)match;
                nameMatchDefine.put(nameMatch.getClassName(), plugin);
            } else {
                signatureMatchDefine.add(plugin);
            }
        }
    }

    public AbstractClassEnhancePluginDefine find(TypeDescription typeDescription,
        ClassLoader classLoader) {
        String typeName = typeDescription.getTypeName();
        if (nameMatchDefine.containsKey(typeName)) {
            return nameMatchDefine.get(typeName);
        }

        for (AbstractClassEnhancePluginDefine pluginDefine : signatureMatchDefine) {
            IndirectMatch match = (IndirectMatch)pluginDefine.enhanceClass();
            if (match.isMatch(typeDescription)) {
                return pluginDefine;
            }
        }

        return null;
    }

    public ElementMatcher<? super TypeDescription> buildMatch() {
        ElementMatcher.Junction judge = new AbstractJunction<NamedElement>() {
            @Override
            public boolean matches(NamedElement target) {
                return nameMatchDefine.containsKey(target.getActualName());
            }
        };
        judge = judge.and(not(isInterface()));
        for (AbstractClassEnhancePluginDefine define : signatureMatchDefine) {
            ClassMatch match = define.enhanceClass();
            if (match instanceof IndirectMatch) {
                judge = judge.or(((IndirectMatch)match).buildJunction());
            }
        }
        return judge;
    }
}
