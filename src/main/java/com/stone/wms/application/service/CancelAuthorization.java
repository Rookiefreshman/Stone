package com.stone.wms.application.service;

/** No production implementation supplied. A future trusted local policy must verify
 * authenticated subject, tenant/warehouse, permission, exact approval hash and expiry.
 * Must not make external network calls while the warehouse row is locked.
 * Replay requires current permission; new submit additionally requires valid approval.
 */
public interface CancelAuthorization {
    void requireAuthorized(CancelCommand command, boolean replay);
}
