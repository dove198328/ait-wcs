package cn.aitplus.wcs.infra.service.profile.impl;

import cn.aitplus.wcs.common.constant.ProfileCacheConstants;
import cn.aitplus.wcs.core.domain.model.ProfileChainNode;
import cn.aitplus.wcs.core.domain.model.WarehouseProfile;
import cn.aitplus.wcs.infra.persistence.profile.ProfileChainNodeMapper;
import cn.aitplus.wcs.infra.persistence.profile.WarehouseProfileMapper;
import cn.aitplus.wcs.infra.service.profile.WarehouseProfileService;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.template.QuickConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class WarehouseProfileServiceImpl implements WarehouseProfileService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseProfileServiceImpl.class);

    private final WarehouseProfileMapper profileMapper;
    private final ProfileChainNodeMapper chainNodeMapper;
    private final Cache<Long, WarehouseProfile> profileCache;
    private final Cache<String, List<ProfileChainNode>> chainCache;

    public WarehouseProfileServiceImpl(WarehouseProfileMapper profileMapper,
                                       ProfileChainNodeMapper chainNodeMapper,
                                       CacheManager cacheManager) {
        this.profileMapper = profileMapper;
        this.chainNodeMapper = chainNodeMapper;
        this.profileCache = cacheManager.getOrCreateCache(
                QuickConfig.newBuilder(ProfileCacheConstants.PROFILE_CACHE_NAME)
                        .cacheType(CacheType.BOTH)
                        .expire(Duration.ofSeconds(300))
                        .syncLocal(true)
                        .build());
        this.chainCache = cacheManager.getOrCreateCache(
                QuickConfig.newBuilder(ProfileCacheConstants.CHAIN_CACHE_NAME)
                        .cacheType(CacheType.BOTH)
                        .expire(Duration.ofSeconds(300))
                        .syncLocal(true)
                        .build());
    }

    @Override
    public WarehouseProfile getProfile(Long warehouseId) {
        return profileCache.computeIfAbsent(warehouseId,
                id -> profileMapper.selectByWarehouseId(id),
                true);
    }

    @Override
    public List<WarehouseProfile> listAll() {
        return profileMapper.selectAll();
    }

    @Override
    @Transactional
    public WarehouseProfile createProfile(WarehouseProfile profile) {
        if (profile.getVersion() == null) {
            profile.setVersion(1);
        }
        if (profile.getActive() == null) {
            profile.setActive(true);
        }
        profileMapper.insert(profile);
        evictAfterCommit(profile.getWarehouseId());
        return profile;
    }

    @Override
    @Transactional
    public WarehouseProfile updateProfile(WarehouseProfile profile) {
        WarehouseProfile current = profileMapper.selectByWarehouseId(profile.getWarehouseId());
        if (current == null) {
            throw new IllegalArgumentException("Warehouse profile does not exist, warehouseId=" + profile.getWarehouseId());
        }
        profile.setVersion(current.getVersion() + 1);
        profileMapper.update(profile);
        evictAfterCommit(profile.getWarehouseId());
        return profileMapper.selectByWarehouseId(profile.getWarehouseId());
    }

    @Override
    @Transactional
    public void deleteProfile(Long warehouseId) {
        List<String> chainNames = chainNodeMapper.selectChainNamesByWarehouseId(warehouseId);
        chainNodeMapper.deleteByWarehouseId(warehouseId);
        profileMapper.deleteByWarehouseId(warehouseId);
        evictAfterCommit(warehouseId, chainNames);
    }

    @Override
    public List<ProfileChainNode> getChainNodes(Long warehouseId, String chainName) {
        String key = ProfileCacheConstants.buildChainCacheKey(warehouseId, chainName);
        List<ProfileChainNode> nodes = chainCache.computeIfAbsent(key,
                k -> {
                    List<ProfileChainNode> result = chainNodeMapper.selectByChain(warehouseId, chainName);
                    return result == null ? Collections.emptyList() : result;
                });
        return nodes == null ? Collections.emptyList() : nodes;
    }

    @Override
    public List<ProfileChainNode> getAllChainNodes(Long warehouseId) {
        return chainNodeMapper.selectByWarehouseId(warehouseId);
    }

    @Override
    @Transactional
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
        evictChainAfterCommit(warehouseId, chainName);
        log.info("Replace chain nodes done, warehouseId={}, chain={}, nodeCount={}", warehouseId, chainName,
                nodes == null ? 0 : nodes.size());
    }

    @Override
    @Transactional
    public void deleteChainNodes(Long warehouseId, String chainName) {
        chainNodeMapper.deleteByWarehouseIdAndChain(warehouseId, chainName);
        evictChainAfterCommit(warehouseId, chainName);
    }

    @Override
    public void evictCache(Long warehouseId) {
        List<String> chainNames = chainNodeMapper.selectChainNamesByWarehouseId(warehouseId);
        profileCache.remove(warehouseId);
        evictChainCaches(warehouseId, chainNames);
        log.info("Manual cache eviction done, warehouseId={}", warehouseId);
    }

    private void evictAfterCommit(Long warehouseId) {
        List<String> chainNames = chainNodeMapper.selectChainNamesByWarehouseId(warehouseId);
        evictAfterCommit(warehouseId, chainNames);
    }

    private void evictAfterCommit(Long warehouseId, List<String> chainNames) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    profileCache.remove(warehouseId);
                    evictChainCaches(warehouseId, chainNames);
                    log.info("Evict profile cache after commit, warehouseId={}", warehouseId);
                }
            });
        } else {
            profileCache.remove(warehouseId);
            evictChainCaches(warehouseId, chainNames);
        }
    }

    private void evictChainAfterCommit(Long warehouseId, String chainName) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    chainCache.remove(ProfileCacheConstants.buildChainCacheKey(warehouseId, chainName));
                    log.info("Evict chain cache after commit, warehouseId={}, chain={}", warehouseId, chainName);
                }
            });
        } else {
            chainCache.remove(ProfileCacheConstants.buildChainCacheKey(warehouseId, chainName));
        }
    }

    private void evictChainCaches(Long warehouseId, List<String> chainNames) {
        if (chainNames == null || chainNames.isEmpty()) {
            return;
        }
        for (String chain : chainNames) {
            chainCache.remove(ProfileCacheConstants.buildChainCacheKey(warehouseId, chain));
        }
    }
}
