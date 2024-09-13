package org.cockshott.commandshop.models;

import java.util.List;

import lombok.Getter;

@Getter
public class SearchResult {
    private final List<ShopItem> items;
    private final int totalItems;

    public SearchResult(List<ShopItem> items, int totalItems) {
        this.items = items;
        this.totalItems = totalItems;
    }
}
