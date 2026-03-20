package cn.aitplus.wcs.core.spi.context;

import cn.aitplus.wcs.core.domain.model.Task;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StartContext extends BasePluginContext {

    private Task task;
}
