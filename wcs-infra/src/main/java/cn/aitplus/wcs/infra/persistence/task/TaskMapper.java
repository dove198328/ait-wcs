package cn.aitplus.wcs.infra.persistence.task;

import cn.aitplus.wcs.core.domain.enums.DomainEnums.TaskStatus;
import cn.aitplus.wcs.core.domain.model.Task;

/**
 * MyBatis mapper example for table wcs_task.
 */
public interface TaskMapper {

    Task selectByWarehouseIdAndId(String warehouseId, Long id);

    Task selectByWarehouseIdAndBusinessKey(String warehouseId, String businessKey);

    int updateStatusByWarehouseIdAndId(String warehouseId, Long id, TaskStatus status);
}

