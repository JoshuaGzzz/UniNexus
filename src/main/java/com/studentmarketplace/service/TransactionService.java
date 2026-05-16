package com.studentmarketplace.service;

import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.model.Post;
import com.studentmarketplace.util.CartManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final DatabaseManager dbManager;

    public TransactionService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public MarketplaceService.CheckoutResult checkout(int buyerId, List<Post> selectedPosts,
            java.util.function.Function<Integer, Optional<Post>> postLoader) {
        return processCheckout(buyerId, selectedPosts, "CASH", postLoader);
    }

    public MarketplaceService.CheckoutResult processCheckout(int buyerId, List<Post> selectedPosts,
            String paymentMethod, java.util.function.Function<Integer, Optional<Post>> postLoader) {

        System.out.println("[CHECKOUT] Starting checkout. buyerId=" + buyerId
                + ", items=" + (selectedPosts == null ? 0 : selectedPosts.size())
                + ", payment=" + paymentMethod);

        if (buyerId <= 0) {
            System.out.println("[CHECKOUT] FAIL: invalid buyer id");
            return MarketplaceService.CheckoutResult.failure("Invalid buyer session.");
        }
        if (selectedPosts == null || selectedPosts.isEmpty()) {
            System.out.println("[CHECKOUT] FAIL: cart is empty");
            return MarketplaceService.CheckoutResult.failure("Cart is empty.");
        }

        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        System.out.println("[CHECKOUT] Normalized payment method: " + normalizedPaymentMethod);

        try {
            dbManager.beginTransaction();
            System.out.println("[CHECKOUT] Transaction started");

            for (Post post : selectedPosts) {
                System.out.println("[CHECKOUT] Processing post_id=" + post.getPostId()
                        + " title=" + post.getTitle());

                Optional<Post> latest = postLoader.apply(post.getPostId());
                if (latest.isEmpty()) {
                    System.out.println("[CHECKOUT] FAIL: postLoader returned empty for post_id=" + post.getPostId());
                    dbManager.rollback();
                    return MarketplaceService.CheckoutResult.failure("One or more items are no longer available.");
                }

                Post latestPost = latest.get();
                System.out.println("[CHECKOUT] Latest post status=" + latestPost.getStatus()
                        + " type=" + latestPost.getPostType()
                        + " price=" + latestPost.getPrice()
                        + " sellerId=" + latestPost.getSellerId());

                if (latestPost.getStatus() != Post.PostStatus.ACTIVE) {
                    System.out.println("[CHECKOUT] FAIL: post is not ACTIVE, status=" + latestPost.getStatus());
                    dbManager.rollback();
                    return MarketplaceService.CheckoutResult.failure("One or more items are no longer available.");
                }

                String transactionType = switch (latestPost.getPostType()) {
                    case RENTAL -> "RENTAL_PAYMENT";
                    case RESOURCE -> "RESOURCE_PURCHASE";
                    default -> "SALE";
                };
                String newStatus = latestPost.getPostType() == Post.PostType.RENTAL ? "RENTED" : "SOLD";
                System.out.println("[CHECKOUT] transactionType=" + transactionType + " newStatus=" + newStatus);

                int inserted = dbManager.executeUpdate(
                    "INSERT INTO transactions (buyer_id, seller_id, post_id, amount, transaction_type, status, payment_method, completion_date) VALUES (?, ?, ?, ?, ?, 'COMPLETED', ?, CURRENT_TIMESTAMP)",
                    buyerId, latestPost.getSellerId(), latestPost.getPostId(),
                    latestPost.getPrice(), transactionType, normalizedPaymentMethod
                );
                System.out.println("[CHECKOUT] INSERT transactions result=" + inserted);

                if (inserted <= 0) {
                    System.out.println("[CHECKOUT] FAIL: INSERT returned 0");
                    dbManager.rollback();
                    return MarketplaceService.CheckoutResult.failure("Failed to save transaction.");
                }

                int updated = dbManager.executeUpdate(
                    "UPDATE posts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE post_id = ?",
                    newStatus, latestPost.getPostId()
                );
                System.out.println("[CHECKOUT] UPDATE posts result=" + updated);

                if (updated <= 0) {
                    System.out.println("[CHECKOUT] FAIL: UPDATE posts returned 0");
                    dbManager.rollback();
                    return MarketplaceService.CheckoutResult.failure("Failed to update post status.");
                }

                if (latestPost.getPostType() == Post.PostType.RESOURCE) {
                    dbManager.executeUpdate(
                        "UPDATE academic_resources SET download_count = COALESCE(download_count, 0) + 1 WHERE post_id = ?",
                        latestPost.getPostId()
                    );
                }
            }

            dbManager.commit();
            CartManager.getInstance().clear();
            System.out.println("[CHECKOUT] SUCCESS: committed and cart cleared");
            return MarketplaceService.CheckoutResult.success("Checkout completed successfully.");

        } catch (Exception e) {
            System.out.println("[CHECKOUT] EXCEPTION: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            try { dbManager.rollback(); } catch (Exception ignored) {}
            return MarketplaceService.CheckoutResult.failure("Checkout failed: " + e.getMessage());
        }
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) return "CASH";
        return switch (paymentMethod.trim().toUpperCase()) {
            case "ONLINE_TRANSFER" -> "ONLINE_TRANSFER";
            case "CREDIT_CARD" -> "CREDIT_CARD";
            default -> "CASH";
        };
    }
}
