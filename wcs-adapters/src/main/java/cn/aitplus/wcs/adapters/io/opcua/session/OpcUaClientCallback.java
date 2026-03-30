package cn.aitplus.wcs.adapters.io.opcua.session;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

@FunctionalInterface
public interface OpcUaClientCallback<T> {

    T apply(OpcUaClient client) throws Exception;
}
