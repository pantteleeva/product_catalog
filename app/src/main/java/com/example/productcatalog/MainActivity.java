package com.example.productcatalog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private DatabaseHelper database;
    private EditText searchInput;
    private Spinner categoryFilter, sortSpinner;
    private ListView productList;
    private ProductAdapter adapter;
    private final List<Product> displayedProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        database = new DatabaseHelper(this);

        searchInput = findViewById(R.id.searchInput);
        categoryFilter = findViewById(R.id.categoryFilter);
        sortSpinner = findViewById(R.id.sortSpinner);
        productList = findViewById(R.id.productList);

        findViewById(R.id.addProductButton).setOnClickListener(v -> openEditor(-1));
        findViewById(R.id.addCategoryButton).setOnClickListener(v -> addCategory());

        adapter = new ProductAdapter(this, displayedProducts);
        productList.setAdapter(adapter);
        productList.setOnItemClickListener((parent, view, position, id) ->
                openEditor(displayedProducts.get(position).id));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadProducts();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        categoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                loadProducts();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                loadProducts();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        loadFilters();
    }

    @Override protected void onResume() {
        super.onResume();
        if (database != null && categoryFilter != null) {
            loadFilters();
        }
    }

    private void loadFilters() {
        List<String> categories = new ArrayList<>();
        categories.add("Все категории");
        categories.addAll(database.getCategories());
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilter.setAdapter(categoryAdapter);

        String[] sorts = {"Название", "Количество"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sorts);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        loadProducts();
    }

    private void loadProducts() {
        if (categoryFilter.getSelectedItem() == null || sortSpinner.getSelectedItem() == null) return;
        String category = categoryFilter.getSelectedItem().toString();
        String sort = sortSpinner.getSelectedItem().toString();
        List<Product> result = database.getProducts(searchInput.getText().toString(), category, sort);
        displayedProducts.clear();
        displayedProducts.addAll(result);
        adapter.notifyDataSetChanged();

        TextView empty = findViewById(R.id.emptyText);
        empty.setVisibility(displayedProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openEditor(long productId) {
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra("product_id", productId);
        startActivity(intent);
    }

    private void addCategory() {
        EditText input = new EditText(this);
        input.setHint("Название категории");
        new AlertDialog.Builder(this).setTitle("Новая категория").setView(input)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Введите название категории", Toast.LENGTH_SHORT).show();
                    } else if (!database.addCategory(name)) {
                        Toast.makeText(this, "Такая категория уже существует", Toast.LENGTH_SHORT).show();
                    } else {
                        loadFilters();
                        Toast.makeText(this, "Категория добавлена", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Отмена", null).show();
    }
}
