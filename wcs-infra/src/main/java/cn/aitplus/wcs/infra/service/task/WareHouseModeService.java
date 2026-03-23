package cn.aitplus.wcs.infra.service.task;

public interface WareHouseModeService {

    String getCurrentMode(Integer warehouseId);

    String changeMode(Integer warehouseId, int mode);
}
