package cn.aitplus.wcs.core.spi;

import cn.aitplus.wcs.core.spi.context.DispatchContext;
import cn.aitplus.wcs.core.spi.model.PluginDecisions.DispatchDecision;

public interface DispatchPolicyPlugin {

    String pluginName();

    DispatchDecision decide(DispatchContext context);
}
