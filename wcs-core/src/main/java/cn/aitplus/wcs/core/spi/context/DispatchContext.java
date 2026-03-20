package cn.aitplus.wcs.core.spi.context;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DispatchContext extends BasePluginContext {

    private Step currentStep;
}
