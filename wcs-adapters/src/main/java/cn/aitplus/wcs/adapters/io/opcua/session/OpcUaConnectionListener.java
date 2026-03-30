package cn.aitplus.wcs.adapters.io.opcua.session;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

/**
 * {@link OpcUaClientRegistry} 在创建/拆除 Milo 客户端时回调，供订阅等模块挂接监听器或在断线后失效本地状态。
 */
public interface OpcUaConnectionListener {

    /**
     * 新建客户端并成功 {@link OpcUaClient#connect()} 之后调用（在持有该连接 {@code lock} 的线程上）。
     */
    default void onClientConnected(ConnectionKey key, OpcUaClient client) {
    }

    /**
     * Session 恢复活跃后回调；同一个 client 在重连后可能多次触发。
     */
    default void onSessionActive(ConnectionKey key, OpcUaClient client) {
    }

    /**
     * Session 进入 inactive 状态时回调；不意味着此时要主动 disconnect。
     */
    default void onSessionInactive(ConnectionKey key, OpcUaClient client) {
    }

    /**
     * 即将断开并丢弃客户端引用之前调用（在持有该连接 {@code lock} 的线程上）；{@code client} 可能为 null。
     */
    default void beforeClientTeardown(ConnectionKey key, OpcUaClient client) {
    }

    /**
     * 客户端已从池中拆除并 {@link OpcUaClient#disconnect()} 尝试完成之后调用。
     */
    default void afterClientTeardown(ConnectionKey key) {
    }
}
