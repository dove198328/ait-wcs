package cn.aitplus.wcs.infra.service.profile;

import cn.aitplus.wcs.core.domain.model.profile.ProfileChainNode;
import cn.aitplus.wcs.core.domain.model.profile.WarehouseProfile;

import java.util.List;

public interface WarehouseProfileService {

    WarehouseProfile getProfile(Long warehouseId);

    List<WarehouseProfile> listAll();

    WarehouseProfile createProfile(WarehouseProfile profile);

    WarehouseProfile updateProfile(WarehouseProfile profile);

    void deleteProfile(Long warehouseId);

    List<ProfileChainNode> getChainNodes(Long warehouseId, String chainName);

    List<ProfileChainNode> getAllChainNodes(Long warehouseId);

    void replaceChainNodes(Long warehouseId, String chainName, List<ProfileChainNode> nodes);

    void deleteChainNodes(Long warehouseId, String chainName);

    void evictCache(Long warehouseId);
}
