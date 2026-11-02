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
    private static final int DB_VERSION = 1;

    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_USERS = "users";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE)");

        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sku TEXT NOT NULL UNIQUE, " +
                "name TEXT NOT NULL, " +
                "category_id INTEGER, " +
                "stock INTEGER NOT NULL DEFAULT 0, " +
                "active INTEGER NOT NULL DEFAULT 1, " +
                "image_uri TEXT, " +
                "FOREIGN KEY(category_id) REFERENCES " + TABLE_CATEGORIES + "(id))");

        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "login TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL)");

        ContentValues user = new ContentValues();
        user.put("login", "admin");
        user.put("password", "admin123");
        db.insert(TABLE_USERS, null, user);

        String[] categories = {"Электроника", "Одежда", "Дом и быт"};
        for (String category : categories) {
            ContentValues value = new ContentValues();
            value.put("name", category);
            db.insert(TABLE_CATEGORIES, null, value);
        }

        seedProduct(db, "EL-001", "Наушники", "Электроника", 15);
        seedProduct(db, "EL-002", "Портативная колонка", "Электроника", 8);
        seedProduct(db, "EL-003", "Компьютерная мышь", "Электроника", 21);
        seedProduct(db, "CL-001", "Футболка", "Одежда", 30);
        seedProduct(db, "CL-002", "Толстовка", "Одежда", 12);
        seedProduct(db, "HM-001", "Настольная лампа", "Дом и быт", 7);
        seedProduct(db, "HM-002", "Кружка", "Дом и быт", 40);
    }

    private void seedProduct(SQLiteDatabase db, String sku, String name,
                             String category, int stock) {
        long categoryId = getCategoryId(db, category);
        ContentValues value = new ContentValues();
        value.put("sku", sku);
        value.put("name", name);
        value.put("category_id", categoryId);
        value.put("stock", stock);
        value.put("active", 1);
        value.put("image_uri", "");
        db.insert(TABLE_PRODUCTS, null, value);
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

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Здесь добавляются миграции при изменении структуры БД.
    }

    public boolean checkLogin(String login, String password) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id FROM " + TABLE_USERS + " WHERE login=? AND password=?",
                new String[]{login, password});
        boolean result = cursor.moveToFirst();
        cursor.close();
        return result;
    }

    public boolean changePassword(String login, String newPassword) {
        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        return getWritableDatabase().update(TABLE_USERS, values, "login=?",
                new String[]{login}) > 0;
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
                "SELECT p.id,p.sku,p.name,COALESCE(c.name,'Без категории')," +
                "p.stock,p.active,COALESCE(p.image_uri,'') " +
                "FROM products p LEFT JOIN categories c ON p.category_id=c.id WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (p.name LIKE ? OR p.sku LIKE ?)");
            args.add("%" + search.trim() + "%");
            args.add("%" + search.trim() + "%");
        }
        if (category != null && !category.equals("Все категории")) {
            sql.append(" AND c.name=?");
            args.add(category);
        }

        if ("Артикул".equals(sort)) sql.append(" ORDER BY p.sku COLLATE NOCASE");
        else if ("Количество".equals(sort)) sql.append(" ORDER BY p.stock DESC");
        else sql.append(" ORDER BY p.name COLLATE NOCASE");

        Cursor cursor = getReadableDatabase().rawQuery(sql.toString(),
                args.toArray(new String[0]));
        while (cursor.moveToNext()) {
            result.add(new Product(cursor.getLong(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getInt(4),
                    cursor.getInt(5) == 1, cursor.getString(6)));
        }
        cursor.close();
        return result;
    }

    public Product getProduct(long id) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT p.id,p.sku,p.name,COALESCE(c.name,'Без категории')," +
                "p.stock,p.active,COALESCE(p.image_uri,'') " +
                "FROM products p LEFT JOIN categories c ON p.category_id=c.id WHERE p.id=?",
                new String[]{String.valueOf(id)});
        Product product = null;
        if (cursor.moveToFirst()) {
            product = new Product(cursor.getLong(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getInt(4),
                    cursor.getInt(5) == 1, cursor.getString(6));
        }
        cursor.close();
        return product;
    }

    public boolean saveProduct(long id, String sku, String name, String category,
                               int stock, boolean active, String imageUri) {
        long categoryId = getCategoryId(getWritableDatabase(), category);
        if (categoryId < 0) return false;

        ContentValues values = new ContentValues();
        values.put("sku", sku.trim());
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
}
