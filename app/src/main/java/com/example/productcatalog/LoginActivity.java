package com.example.productcatalog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private DatabaseHelper database;
    private EditText loginInput, passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new DatabaseHelper(this);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (22 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Каталог товаров");
        title.setTextSize(26);
        title.setGravity(android.view.Gravity.CENTER);
        layout.addView(title);

        // SVG находится в app/src/main/assets/Logo.svg.
        android.webkit.WebView logo = new android.webkit.WebView(this);
        logo.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        logo.loadUrl("file:///android_asset/Logo.svg");
        layout.addView(logo, new android.widget.LinearLayout.LayoutParams(-1, pad * 5));

        loginInput = new EditText(this);
        loginInput.setSingleLine(true);
        loginInput.setHint("Логин");
        loginInput.setText("admin");
        layout.addView(loginInput);

        passwordInput = new EditText(this);
        passwordInput.setSingleLine(true);
        passwordInput.setHint("Пароль");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        Button enter = new Button(this);
        enter.setText("Войти");
        layout.addView(enter);

        TextView forgot = new TextView(this);
        forgot.setText("Забыли пароль?");
        forgot.setTextSize(16);
        forgot.setPadding(0, pad, 0, pad);
        layout.addView(forgot);

        setContentView(layout);

        enter.setOnClickListener(v -> {
            String login = loginInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            if (database.checkLogin(login, password)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show();
            }
        });

        forgot.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setPadding(24, 8, 24, 0);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        EditText login = new EditText(this);
        login.setHint("Логин");
        login.setText(loginInput.getText().toString().trim());
        EditText newPassword = new EditText(this);
        newPassword.setHint("Новый пароль");
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(login);
        box.addView(newPassword);

        new AlertDialog.Builder(this).setTitle("Смена пароля").setView(box)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String user = login.getText().toString().trim();
                    String pass = newPassword.getText().toString();
                    if (pass.length() < 4) {
                        Toast.makeText(this, "Пароль должен содержать минимум 4 символа",
                                Toast.LENGTH_LONG).show();
                    } else if (!database.changePassword(user, pass)) {
                        Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Пароль изменён", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Отмена", null).show();
    }
}
