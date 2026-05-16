package com.studentmarketplace.service;

import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.model.RefundRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for after-sale operations such as refund requests.
 */
public class AfterSaleService {
    private static final Logger logger = LoggerFactory.getLogger(AfterSaleService.class);

    private final DatabaseManager dbManager;

    public AfterSaleService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Submit a new refund request for a buyer.
     */
    public boolean submitRefund(int buyerId, int transactionId, String reason) {
        String sql = "INSERT INTO refund_requests (buyer_id, transaction_id, reason) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, buyerId);
            stmt.setInt(2, transactionId);
            stmt.setString(3, reason);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Failed to submit refund for buyer {} / transaction {}", buyerId, transactionId, e);
            return false;
        }
    }

    /**
     * Get all refund requests submitted by a specific buyer.
     */
    public List<RefundRequest> getBuyerRefunds(int buyerId) {
        String sql = "SELECT * FROM refund_requests WHERE buyer_id = ?";
        List<RefundRequest> refunds = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, buyerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    refunds.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load refunds for buyer {}", buyerId, e);
        }
        return refunds;
    }

    /**
     * Get all refund requests (admin view).
     */
    public List<RefundRequest> getAllRefunds() {
        String sql = "SELECT * FROM refund_requests";
        List<RefundRequest> refunds = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                refunds.add(mapRow(rs));
            }
        } catch (Exception e) {
            logger.error("Failed to load all refunds", e);
        }
        return refunds;
    }

    /**
     * Update the status of a refund request (e.g. APPROVED, REJECTED).
     */
    public boolean updateRefundStatus(int refundId, String status) {
        String sql = "UPDATE refund_requests SET status = ? WHERE refund_id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, refundId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Failed to update refund {} to status {}", refundId, status, e);
            return false;
        }
    }

    private RefundRequest mapRow(ResultSet rs) throws Exception {
        return new RefundRequest(
                rs.getInt("refund_id"),
                rs.getInt("buyer_id"),
                rs.getInt("transaction_id"),
                rs.getString("reason"),
                rs.getString("status"),
                rs.getString("created_at")
        );
    }
}
