package cn.aitplus.wcs.core.spi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class PluginDecisions {

    private PluginDecisions() {
    }

    public enum StartAction {
        START,
        DEFER,
        REJECT
    }

    public enum ExceptionAction {
        RETRY,
        SUSPEND,
        REDIRECT,
        MANUAL_INTERVENTION
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartDecision {
        private StartAction action;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutingDecision {
        private boolean accepted;
        private String targetPort;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchDecision {
        private boolean executable;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExceptionDecision {
        private ExceptionAction action;
        private String reason;
    }
}
