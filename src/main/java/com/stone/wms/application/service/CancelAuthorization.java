package com.stone.wms.application.service;

/**
 * Trusted adapter boundary, not an HTTP request model.
 * Implementations must resolve the actual authenticated subject, tenant/warehouse
 * permission and current authorization; verify the approval issuer, authenticity,
 * revocation status and exact request binding. No network I/O under warehouse lock.
 * No production implementation is supplied. Access alone permits receipt replay,
 * never a new cancellation.
 */
public interface CancelAuthorization {
    void requireAccess(CancelCommand command);
    VerifiedCancelApproval verifiedApproval(CancelCommand command);
}
