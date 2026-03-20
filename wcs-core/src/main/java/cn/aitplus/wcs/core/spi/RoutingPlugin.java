package cn.aitplus.wcs.core.spi;

import cn.aitplus.wcs.core.spi.context.RoutingContext;
import cn.aitplus.wcs.core.spi.model.PluginDecisions.RoutingDecision;

public interface RoutingPlugin {

    String pluginName();

    RoutingDecision route(RoutingContext context);
}
