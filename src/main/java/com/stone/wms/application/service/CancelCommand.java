package com.stone.wms.application.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import com.stone.wms.domain.model.dispatch.DispatchTask;

/** Internal immutable command. Identity and approval must be verified by a trusted policy port. */
public final class CancelCommand {
    public final String operationId, tenant, actor, taskKey, workType, lotNo, reason, approvalHash;
    public final long warehouseId,taskId,skuId,destinationId;
    public final Long sourceId;
    public final int quantity;
    public CancelCommand(String operationId,long warehouseId,long taskId,String tenant,String actor,
            String taskKey,String workType,long skuId,String lotNo,Long sourceId,long destinationId,
            int quantity,String reason,String approvalHash) {
        if(operationId==null || !operationId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            || warehouseId<=0 || taskId<=0 || skuId<=0 || destinationId<=0 || quantity<=0
            || tenant==null || tenant.isEmpty() || tenant.codePointCount(0,tenant.length())>128
            || actor==null || actor.isEmpty() || actor.codePointCount(0,actor.length())>128
            || taskKey==null || !taskKey.matches("[A-Za-z0-9._:/-]{1,64}")
            || lotNo==null || !lotNo.matches("[A-Za-z0-9._-]{1,64}")
            || reason==null || reason.trim().isEmpty() || reason.codePointCount(0,reason.length())>240
            || approvalHash==null || !approvalHash.matches("[a-f0-9]{64}")
            || !("PUTAWAY".equals(workType) && sourceId==null || "TRANSFER".equals(workType) && sourceId!=null && sourceId>0 && sourceId!=destinationId))
            throw new IllegalArgumentException("INVALID_CANCEL_COMMAND");
        this.operationId=operationId;this.warehouseId=warehouseId;this.taskId=taskId;this.tenant=tenant;this.actor=actor;
        this.taskKey=taskKey;this.workType=workType;this.skuId=skuId;this.lotNo=lotNo;this.sourceId=sourceId;
        this.destinationId=destinationId;this.quantity=quantity;this.reason=reason;this.approvalHash=approvalHash;
    }
    public boolean matches(DispatchTask t) {
        return t!=null && t.work!=null && t.id==taskId && taskKey.equals(t.taskKey)
            && workType.equals(t.work.type.name()) && skuId==t.work.skuId && lotNo.equals(t.work.lotNo)
            && Objects.equals(sourceId,t.work.sourceId) && destinationId==t.work.destinationId && quantity==t.work.quantity;
    }
    public String fingerprint() {
        String[] fields={"CANCEL_READY_V1",operationId,Long.toString(warehouseId),Long.toString(taskId),tenant,actor,taskKey,workType,
            Long.toString(skuId),lotNo,sourceId==null?"":sourceId.toString(),Long.toString(destinationId),Integer.toString(quantity),reason,approvalHash};
        try {
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            for(String field:fields) {
                byte[] raw=field.getBytes(StandardCharsets.UTF_8);
                md.update(ByteBuffer.allocate(4).putInt(raw.length).array());md.update(raw);
            }
            StringBuilder out=new StringBuilder();
            for(byte v:md.digest()) out.append(String.format("%02x",v&255));
            return out.toString();
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
