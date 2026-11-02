package com.example.productcatalog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "product_catalog.db";
    private static final int DB_VERSION = 2;

    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_CATEGORIES = "categories";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE)");

        // Таблица товаров без артикула (sku) — только название, категория, остаток, активность и фото.
        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "category_id INTEGER, " +
                "stock INTEGER NOT NULL DEFAULT 0, " +
                "active INTEGER NOT NULL DEFAULT 1, " +
                "image_uri TEXT, " +
                "FOREIGN KEY(category_id) REFERENCES " + TABLE_CATEGORIES + "(id))");

        String[] categories = {"Электроника", "Одежда", "Дом и быт"};
        for (String category : categories) {
            ContentValues value = new ContentValues();
            value.put("name", category);
            db.insert(TABLE_CATEGORIES, null, value);
        }
        // Товары больше не создаются автоматически — остаются только категории.
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Миграция: удаляем старые таблицы (с артикулом и семенными товарами)
            // и пересоздаём структуру заново.
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
            onCreate(db);
        }
    }

    public List<String> getCategories() {
        List<String> result = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT name FROM " + TABLE_CATEGORIES + " ORDER BY name", null);
        while (cursor.moveToNext()) result.add(cursor.getString(0));
        cursor.close();
        return result;
    }

    public boolean addCategory(String name) {
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        return getWritableDatabase().insert(TABLE_CATEGORIES, null, values) != -1;
    }

    public List<Product> getProducts(String search, String category, String sort) {
        List<Product> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT p.id,p.name,COALESCE(c.name,'Без категории')," +
                "p.stock,p.active,COALESCE(p.image_uri,'') " +
                "FROM products p LEFT JOIN categories c ON p.category_id=c.id WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND p.name LIKE ?");
            args.add("%" + search.trim() + "%");
        }
        if (category != null && !category.equals("Все категории")) {
            sql.append(" AND c.name=?");
            args.add(category);
        }

        if ("Количество".equals(sort)) sql.append(" ORDER BY p.stock DESC");
        else sql.append(" ORDER BY p.name COLLATE NOCASE");

        Cursor cursor = getReadableDatabase().rawQuery(sql.toString(),
                args.toArray(new String[0]));
        while (cursor.moveToNext()) {
            result.add(new Product(cursor.getLong(0), cursor.getString(1),
                    cursor.getString(2), cursor.getInt(3),
                    cursor.getInt(4) == 1, cursor.getString(5)));
        }
        cursor.close();
        return result;
    }

    public Product getProduct(long id) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT p.id,p.name,COALESCE(c.name,'Без категории')," +
                "p.stock,p.active,COALESCE(p.image_uri,'') " +
                "FROM products p LEFT JOIN categories c ON p.category_id=c.id WHERE p.id=?",
                new String[]{String.valueOf(id)});
        Product product = null;
        if (cursor.moveToFirst()) {
            product = new Product(cursor.getLong(0), cursor.getString(1),
                    cursor.getString(2), cursor.getInt(3),
                    cursor.getInt(4) == 1, cursor.getString(5));
        }
        cursor.close();
        return product;
    }

    public boolean saveProduct(long id, String name, String category,
                               int stock, boolean active, String imageUri) {
        long categoryId = getCategoryId(getWritableDatabase(), category);
        if (categoryId < 0) return false;

        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("category_id", categoryId);
        values.put("stock", stock);
        values.put("active", active ? 1 : 0);
        values.put("image_uri", imageUri == null ? "" : imageUri);

        if (id == -1) {
            return getWritableDatabase().insert(TABLE_PRODUCTS, null, values) != -1;
        }
        return getWritableDatabase().update(TABLE_PRODUCTS, values, "id=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteProduct(long id) {
        return getWritableDatabase().delete(TABLE_PRODUCTS, "id=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    private long getCategoryId(SQLiteDatabase db, String name) {
        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + TABLE_CATEGORIES + " WHERE name=?",
                new String[]{name});
        long id = -1;
        if (cursor.moveToFirst()) id = cursor.getLong(0);
        cursor.close();
        return id;
    }
}
