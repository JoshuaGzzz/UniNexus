package com.studentmarketplace.util;

import com.studentmarketplace.model.Post;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * In-memory buyer cart backed by an observable list.
 */
public final class CartManager {
    private static CartManager instance;

    private final ObservableList<Post> cartItems = FXCollections.observableArrayList();

    private CartManager() {
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public ObservableList<Post> getCartItems() {
        return cartItems;
    }

    public boolean add(Post post) {
        if (post == null) {
            return false;
        }

        for (Post existing : cartItems) {
            if (existing.getPostId() == post.getPostId()) {
                return false;
            }
        }
        return cartItems.add(post);
    }

    public boolean remove(Post post) {
        return cartItems.remove(post);
    }

    public void clear() {
        cartItems.clear();
    }

    public double getTotalAmount() {
        return cartItems.stream().mapToDouble(Post::getPrice).sum();
    }
}
