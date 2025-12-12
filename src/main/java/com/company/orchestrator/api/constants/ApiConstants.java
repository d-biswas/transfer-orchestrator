package com.company.orchestrator.api.constants;

public final class ApiConstants {
    public final static String API_VERSION = "/api/v1";

    public static class Path {
        public static final String TRANSFERS = "/transfers";

        public static final String INITIATE_TRANSFER = TRANSFERS;
        public static final String TRANSFER_BY_ID = TRANSFERS + "/{" + Variable.ID + "}";

        public static final String GET_AUDIT_LOGS = TRANSFERS + "/{" + Variable.ID + "}/audit";
        public static final String GET_TRANSFER_ANALYTICS = "/analytics/transfers";
        public static final String POLICY_EVALUATION = "/policies/evaluate";

    }

    public static class Variable {
        public static final String ID = "id";
    }
}
