package cn.aitplus.wcs.infra.persistence.task;

import cn.aitplus.wcs.core.domain.enums.DomainEnums.TaskStatus;
import cn.aitplus.wcs.core.domain.model.Task;
import java.util.Optional;

/**
 * Example repository contract. SQL layer should map warehouseId -> warehouse_id.
 */
public interface TaskRepository {

    Optional<Task> findByWarehouseIdAndId(String warehouseId, Long id);

    Optional<Task> findByWarehouseIdAndBusinessKey(String warehouseId, String businessKey);

    int updateStatusByWarehouseIdAndId(String warehouseId, Long id, TaskStatus status);
}

