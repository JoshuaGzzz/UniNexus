package com.studentmarketplace.model;

public class RefundRequest {

    private int refundId;
    private int buyerId;
    private int transactionId;
    private String reason;
    private String status;
    private String createdAt;

    public RefundRequest(int refundId, int buyerId, int transactionId, String reason, String status, String createdAt) {
        this.refundId = refundId;
        this.buyerId = buyerId;
        this.transactionId = transactionId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getRefundId() {
        return refundId;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
