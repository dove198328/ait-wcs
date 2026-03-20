package cn.aitplus.wcs.core.spi;

import cn.aitplus.wcs.core.spi.context.StartContext;
import cn.aitplus.wcs.core.spi.model.PluginDecisions.StartDecision;

public interface StartGatePlugin {

    String pluginName();

    StartDecision evaluate(StartContext context);
}
