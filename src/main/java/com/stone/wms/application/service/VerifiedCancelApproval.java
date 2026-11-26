package com.stone.wms.application.service;

import com.stone.wms.domain.repository.CancelReceiptRepository.ApprovalUse;

/** Assertion returned only by a trusted authorization adapter; not a signature verifier. */
public final class VerifiedCancelApproval {
    public final String issuer, grantId, tenant, requester, reviewer, fingerprint;
    public final long validFromMs, expiresAtMs;

    public VerifiedCancelApproval(String issuer, String grantId, String tenant,
            String requester, String reviewer, String fingerprint, long validFromMs, long expiresAtMs) {
        if (!key(issuer) || !key(grantId) || !subject(tenant) || !subject(requester) || !subject(reviewer)
                || requester.equals(reviewer) || fingerprint == null || !fingerprint.matches("[a-f0-9]{64}")
                || validFromMs < 0 || expiresAtMs <= validFromMs) {
            throw new SecurityException("INVALID_VERIFIED_APPROVAL");
        }
        this.issuer = issuer;
        this.grantId = grantId;
        this.tenant = tenant;
        this.requester = requester;
        this.reviewer = reviewer;
        this.fingerprint = fingerprint;
        this.validFromMs = validFromMs;
        this.expiresAtMs = expiresAtMs;
    }
    private static boolean key(String value) {
        return value != null && value.matches("[A-Za-z0-9._:/-]{1,128}");
    }
    private static boolean subject(String value) {
        return value != null && !value.isEmpty() && value.equals(value.trim())
                && value.codePointCount(0, value.length()) <= 128
                && value.codePoints().noneMatch(cp -> Character.isISOControl(cp)
                        || Character.isWhitespace(cp) || Character.isSpaceChar(cp)
                        || (cp >= 0xD800 && cp <= 0xDFFF));
    }
    ApprovalUse bind(CancelCommand command, long nowMs) {
        if (!tenant.equals(command.tenant) || !requester.equals(command.actor)
                || !fingerprint.equals(command.fingerprint())) {
            throw new SecurityException("APPROVAL_BINDING_MISMATCH");
        }
        if (nowMs < validFromMs || nowMs >= expiresAtMs) {
            throw new SecurityException("APPROVAL_OUTSIDE_VALIDITY");
        }
        return new ApprovalUse(issuer, grantId, tenant, requester, reviewer, validFromMs, expiresAtMs);
    }
}
