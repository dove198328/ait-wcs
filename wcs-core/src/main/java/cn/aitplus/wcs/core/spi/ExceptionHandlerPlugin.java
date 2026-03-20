package cn.aitplus.wcs.core.spi;

import cn.aitplus.wcs.core.spi.context.ExceptionContext;
import cn.aitplus.wcs.core.spi.model.PluginDecisions.ExceptionDecision;

public interface ExceptionHandlerPlugin {

    String pluginName();

    ExceptionDecision handle(ExceptionContext context);
}
