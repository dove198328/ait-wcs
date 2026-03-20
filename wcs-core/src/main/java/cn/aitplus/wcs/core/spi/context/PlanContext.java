package cn.aitplus.wcs.core.spi.context;

import cn.aitplus.wcs.core.domain.model.Task;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlanContext extends BasePluginContext {

    private Task task;
    private Plan plan;
    private List<String> steps = new ArrayList<String>();
}
