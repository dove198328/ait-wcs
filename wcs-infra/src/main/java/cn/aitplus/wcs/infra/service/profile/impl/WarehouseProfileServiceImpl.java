package cn.aitplus.wcs.infra.service.profile.impl;

import cn.aitplus.wcs.common.constant.WcsConstants;
import cn.aitplus.wcs.core.domain.model.profile.ProfileChainNode;
import cn.aitplus.wcs.core.domain.model.profile.WarehouseProfile;
import cn.aitplus.wcs.infra.persistence.profile.ProfileChainNodeMapper;
import cn.aitplus.wcs.infra.persistence.profile.WarehouseProfileMapper;
import cn.aitplus.wcs.infra.service.profile.WarehouseProfileService;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class WarehouseProfileServiceImpl implements WarehouseProfileService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseProfileServiceImpl.class);

    private final WarehouseProfileMapper profileMapper;
    private final ProfileChainNodeMapper chainNodeMapper;

    /**
     * 自引用代理：循环按链名失效时需走 AOP，
     */
    @Autowired
    @Lazy
    private WarehouseProfileServiceImpl self;

    public WarehouseProfileServiceImpl(WarehouseProfileMapper profileMapper,
                                       ProfileChainNodeMapper chainNodeMapper) {
        this.profileMapper = profileMapper;
        this.chainNodeMapper = chainNodeMapper;
    }

    @Override
    @Cached(
            name = WcsConstants.PROFILE_CACHE_NAME,
            key = "#warehouseId",
            cacheType = CacheType.BOTH,
            localExpire = 300,
            syncLocal = true)
    public WarehouseProfile getProfile(Long warehouseId) {
        return profileMapper.selectByWarehouseId(warehouseId);
    }

    @Override
    public List<WarehouseProfile> listAll() {
        return profileMapper.selectAll();
    }

    @Override
    @Transactional
    @CacheInvalidate(name = WcsConstants.PROFILE_CACHE_NAME,
            key = "#profile.warehouseId",
            condition = "#profile != null && #profile.warehouseId != null")
    public WarehouseProfile createProfile(WarehouseProfile profile) {
        if (profile.getVersion() == null) {
            profile.setVersion(1);
        }
        if (profile.getActive() == null) {
            profile.setActive(true);
        }
        profileMapper.insert(profile);
        return profile;
    }

    @Override
    @Transactional
    @CacheInvalidate(name = WcsConstants.PROFILE_CACHE_NAME,
            key = "#profile.warehouseId",
            condition = "#profile != null && #profile.warehouseId != null")
    public WarehouseProfile updateProfile(WarehouseProfile profile) {
        WarehouseProfile current = profileMapper.selectByWarehouseId(profile.getWarehouseId());
        if (current == null) {
            throw new IllegalArgumentException("Warehouse profile does not exist, warehouseId=" + profile.getWarehouseId());
        }
        profile.setVersion(current.getVersion() + 1);
        profileMapper.update(profile);
        return profileMapper.selectByWarehouseId(profile.getWarehouseId());
    }

    @Override
    @Transactional
    @CacheInvalidate(name = WcsConstants.PROFILE_CACHE_NAME,
            key = "#warehouseId",
            condition = "#warehouseId != null")
    public void deleteProfile(Long warehouseId) {
        List<String> chainNames = chainNodeMapper.selectChainNamesByWarehouseId(warehouseId);
        chainNodeMapper.deleteByWarehouseId(warehouseId);
        profileMapper.deleteByWarehouseId(warehouseId);
        evictChainCaches(warehouseId, chainNames);
    }

    @Override
    @Cached(
            name = WcsConstants.CHAIN_CACHE_NAME,
            key = "#warehouseId + ':' + #chainName",
            cacheType = CacheType.BOTH,
            localExpire = 300,
            syncLocal = true)
    public List<ProfileChainNode> getChainNodes(Long warehouseId, String chainName) {
        List<ProfileChainNode> result = chainNodeMapper.selectByChain(warehouseId, chainName);
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<ProfileChainNode> getAllChainNodes(Long warehouseId) {
        return chainNodeMapper.selectByWarehouseId(warehouseId);
    }

    @Override
    @Transactional
    @CacheInvalidate(name = WcsConstants.CHAIN_CACHE_NAME,
            key = "#warehouseId + ':' + #chainName",
            condition = "#warehouseId != null && T(org.springframework.util.StringUtils).hasText(#chainName)")
    public void replaceChainNodes(Long warehouseId, String chainName, List<ProfileChainNode> nodes) {
        chainNodeMapper.deleteByWarehouseIdAndChain(warehouseId, chainName);
        if (nodes != null && !nodes.isEmpty()) {
            for (int i = 0; i < nodes.size(); i++) {
                ProfileChainNode node = nodes.get(i);
                node.setWarehouseId(warehouseId);
                node.setChainName(chainName);
                node.setNodeOrder(i + 1);
                if (node.getEnabled() == null) {
                    node.setEnabled(true);
                }
            }
            chainNodeMapper.batchInsert(nodes);
        }
        log.info("Replace chain nodes done, warehouseId={}, chain={}, nodeCount={}", warehouseId, chainName,
                nodes == null ? 0 : nodes.size());
    }

    @Override
    @Transactional
    @CacheInvalidate(name = WcsConstants.CHAIN_CACHE_NAME,
            key = "#warehouseId + ':' + #chainName",
            condition = "#warehouseId != null && T(org.springframework.util.StringUtils).hasText(#chainName)")
    public void deleteChainNodes(Long warehouseId, String chainName) {
        chainNodeMapper.deleteByWarehouseIdAndChain(warehouseId, chainName);
    }

    @Override
    @CacheInvalidate(name = WcsConstants.PROFILE_CACHE_NAME,
            key = "#warehouseId",
            condition = "#warehouseId != null")
    public void evictCache(Long warehouseId) {
        List<String> chainNames = chainNodeMapper.selectChainNamesByWarehouseId(warehouseId);
        evictChainCaches(warehouseId, chainNames);
        log.info("Manual cache eviction done, warehouseId={}", warehouseId);
    }

    private void evictChainCaches(Long warehouseId, List<String> chainNames) {
        if (chainNames == null || chainNames.isEmpty()) {
            return;
        }
        for (String chain : chainNames) {
            self.evictChainCacheEntry(warehouseId, chain);
        }
    }

    /**
     * 仅供 {@link #evictChainCaches} 经代理调用以触发 {@link CacheInvalidate}，勿从接口对外暴露业务语义。
     */
    @CacheInvalidate(name = WcsConstants.CHAIN_CACHE_NAME,
            key = "#warehouseId + ':' + #chainName",
            condition = "#warehouseId != null && T(org.springframework.util.StringUtils).hasText(#chainName)")
    public void evictChainCacheEntry(Long warehouseId, String chainName) {
        // no-op
    }
}
