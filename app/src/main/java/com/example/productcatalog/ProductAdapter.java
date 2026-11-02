package com.example.productcatalog;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ProductAdapter extends BaseAdapter {
    private final Context context;
    private final List<Product> products;

    public ProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
    }

    @Override public int getCount() { return products.size(); }
    @Override public Object getItem(int position) { return products.get(position); }
    @Override public long getItemId(int position) { return products.get(position).id; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        }

        Product product = products.get(position);
        TextView name = view.findViewById(R.id.productName);
        TextView details = view.findViewById(R.id.productDetails);
        TextView status = view.findViewById(R.id.productStatus);
        ImageView image = view.findViewById(R.id.productThumbnail);

        name.setText(product.name);
        details.setText("Артикул: " + product.sku + " • " + product.category +
                " • Остаток: " + product.stock);
        status.setText(product.active ? "Активна" : "Неактивна");

        image.setImageResource(android.R.drawable.ic_menu_gallery);
        if (product.imageUri != null && !product.imageUri.isEmpty()) {
            try {
                image.setImageURI(Uri.parse(product.imageUri));
            } catch (Exception ignored) {
                image.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
        return view;
    }
}
