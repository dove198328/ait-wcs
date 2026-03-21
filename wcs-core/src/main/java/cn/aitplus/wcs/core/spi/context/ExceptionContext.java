package cn.aitplus.wcs.core.spi.context;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExceptionContext extends BasePluginContext {
    private String errorCode;
    private String errorMessage;
    private boolean businessException;
}
