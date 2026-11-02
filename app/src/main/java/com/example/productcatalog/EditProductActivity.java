package com.example.productcatalog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.util.List;

public class EditProductActivity extends Activity {
    private static final int PICK_IMAGE = 200;
    private DatabaseHelper database;
    private EditText skuInput, nameInput, stockInput;
    private Spinner categorySpinner;
    private CheckBox activeCheck;
    private ImageView productImage;
    private String imageUri = "";
    private long productId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);
        database = new DatabaseHelper(this);
        productId = getIntent().getLongExtra("product_id", -1);

        skuInput = findViewById(R.id.skuInput);
        nameInput = findViewById(R.id.nameInput);
        stockInput = findViewById(R.id.stockInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        activeCheck = findViewById(R.id.activeCheck);
        productImage = findViewById(R.id.productImage);

        loadCategories();
        if (productId != -1) fillProduct();

        findViewById(R.id.chooseImageButton).setOnClickListener(v -> chooseImage());
        findViewById(R.id.saveProductButton).setOnClickListener(v -> saveProduct());
        findViewById(R.id.deleteProductButton).setVisibility(
                productId == -1 ? View.GONE : View.VISIBLE);
        findViewById(R.id.deleteProductButton).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.cancelButton).setOnClickListener(v -> finish());
    }

    private void loadCategories() {
        List<String> categories = database.getCategories();
        if (categories.isEmpty()) {
            Toast.makeText(this, "Сначала добавьте категорию", Toast.LENGTH_LONG).show();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void fillProduct() {
        Product p = database.getProduct(productId);
        if (p == null) {
            Toast.makeText(this, "Товар не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        skuInput.setText(p.sku);
        nameInput.setText(p.name);
        stockInput.setText(String.valueOf(p.stock));
        activeCheck.setChecked(p.active);
        imageUri = p.imageUri;
        int index = ((ArrayAdapter) categorySpinner.getAdapter()).getPosition(p.category);
        if (index >= 0) categorySpinner.setSelection(index);
        showImage();
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null &&
                data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) { }
            imageUri = uri.toString();
            showImage();
        }
    }

    private void showImage() {
        productImage.setImageResource(android.R.drawable.ic_menu_gallery);
        if (imageUri != null && !imageUri.isEmpty()) {
            try {
                productImage.setImageURI(Uri.parse(imageUri));
            } catch (Exception ignored) { }
        }
    }

    private void saveProduct() {
        String sku = skuInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String stockValue = stockInput.getText().toString().trim();

        if (sku.isEmpty() || name.isEmpty() || stockValue.isEmpty()) {
            Toast.makeText(this, "Заполните артикул, название и остаток", Toast.LENGTH_SHORT).show();
            return;
        }
        if (categorySpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Добавьте категорию товара", Toast.LENGTH_SHORT).show();
            return;
        }

        int stock;
        try {
            stock = Integer.parseInt(stockValue);
            if (stock < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Остаток должен быть целым числом не меньше 0",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean saved = database.saveProduct(productId, sku, name,
                categorySpinner.getSelectedItem().toString(), stock,
                activeCheck.isChecked(), imageUri);
        if (saved) {
            Toast.makeText(this, "Товар сохранён", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Не удалось сохранить. Проверьте, не занят ли артикул.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление товара")
                .setMessage("Вы действительно хотите удалить этот товар?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    database.deleteProduct(productId);
                    finish();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
