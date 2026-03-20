package cn.aitplus.wcs.core.spi.context;

import cn.aitplus.wcs.core.domain.model.Step;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExceptionContext extends BasePluginContext {

    private Step failedStep;
    private String errorCode;
    private String errorMessage;
    private boolean businessException;
}
