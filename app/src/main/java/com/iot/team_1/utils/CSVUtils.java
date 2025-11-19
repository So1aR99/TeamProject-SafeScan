package com.iot.team_1.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.iot.team_1.room.AppDatabase;
import com.iot.team_1.room.IngredientEntity;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVUtils {

    public interface OnIngredientsLoadedListener {
        void onLoaded(List<String> ingredientsList);
    }

    public static void importCSVToDB(Context context, int rawCsvResId, OnIngredientsLoadedListener listener) {
        new Thread(() -> {
            List<IngredientEntity> ingredients = new ArrayList<>();
            try {
                InputStream is = context.getResources().openRawResource(rawCsvResId);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if(!line.isEmpty()) {
                        ingredients.add(new IngredientEntity(line));
                    }
                }

                AppDatabase db = AppDatabase.getInstance(context);
                db.ingredientDao().insertIngredients(ingredients);

                List<String> names = new ArrayList<>();
                for(IngredientEntity e : ingredients) names.add(e.getName());

                new Handler(Looper.getMainLooper()).post(() -> listener.onLoaded(names));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
