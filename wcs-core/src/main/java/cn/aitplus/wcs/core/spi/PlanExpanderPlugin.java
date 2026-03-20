package cn.aitplus.wcs.core.spi;

import cn.aitplus.wcs.core.spi.context.PlanContext;

public interface PlanExpanderPlugin {

    String pluginName();

    void expand(PlanContext context);
}
