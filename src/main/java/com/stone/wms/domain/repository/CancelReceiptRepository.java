package com.stone.wms.domain.repository;

public interface CancelReceiptRepository {
    class Receipt {
        public String operationId,requestFingerprint,businessState,recordedAt;
        public long warehouseId,taskId;
    }
    /** Internal audit metadata, deliberately excluded from the public receipt DTO. */
    final class ApprovalUse {
        public final String issuer, grantId, tenant, requester, reviewer;
        public final long validFromMs, expiresAtMs;
        public ApprovalUse(String issuer, String grantId, String tenant, String requester,
                String reviewer, long validFromMs, long expiresAtMs) {
            this.issuer=issuer; this.grantId=grantId; this.tenant=tenant;
            this.requester=requester; this.reviewer=reviewer;
            this.validFromMs=validFromMs; this.expiresAtMs=expiresAtMs;
        }
    }
    Receipt find(long warehouseId,String operationId);
    long currentEpochMillis();
    void insert(long warehouseId,String operationId,long taskId,String fingerprint,ApprovalUse approval);
}
