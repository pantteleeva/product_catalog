package com.example.productcatalog;

public class Product {
    public long id;
    public String sku;
    public String name;
    public String category;
    public int stock;
    public boolean active;
    public String imageUri;

    public Product(long id, String sku, String name, String category,
                   int stock, boolean active, String imageUri) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.stock = stock;
        this.active = active;
        this.imageUri = imageUri;
    }
}
