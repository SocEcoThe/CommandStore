package org.cockshott.commandshop.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopItem {
    private int id;
    private final String name;
    private final Integer price;
    private final String currency;
    private int stock;
    private final String hash;
    private final String type;
    private final String SellName;
    

    public ShopItem(int id, String name, Integer price, String currency, int stock,String hash,String type,String SellName) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.currency = currency;
        this.stock = stock;
        this.hash = hash;
        this.type = type;
        this.SellName = SellName;
    }

    // Getters and setters

    public String getFormattedPrice() {
        return price + " " + currency;
    }
}
