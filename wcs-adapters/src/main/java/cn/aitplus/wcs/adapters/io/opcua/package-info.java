/**
 * OPC UA（Eclipse Milo）集成，按职责分子包：
 * <ul>
 *   <li>{@link cn.aitplus.wcs.adapters.io.opcua.config} — 配置与 Spring 装配</li>
 *   <li>{@link cn.aitplus.wcs.adapters.io.opcua.session} — 会话池、探活、连接生命周期回调</li>
 *   <li>{@link cn.aitplus.wcs.adapters.io.opcua.transport} — {@code DeviceTransport} 同步 Read/Write</li>
 *   <li>{@link cn.aitplus.wcs.adapters.io.opcua.subscription} — MonitoredItem 与推送事件</li>
 * </ul>
 */
package cn.aitplus.wcs.adapters.io.opcua;
