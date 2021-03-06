package org.zimmob.zimlx.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.zimmob.zimlx.activity.HomeActivity;
import org.zimmob.zimlx.config.Config;
import org.zimmob.zimlx.manager.Setup;
import org.zimmob.zimlx.model.App;
import org.zimmob.zimlx.model.Item;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper implements Setup.DataManager {
    private static final String DATABASE_HOME = "home.db";
    private static final String TABLE_HOME = "home";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_LABEL = "label";
    private static final String COLUMN_X_POS = "x";
    private static final String COLUMN_Y_POS = "y";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_PAGE = "page";
    private static final String COLUMN_DESKTOP = "desktop";
    private static final String COLUMN_STATE = "state";

    private static final String SQL_CREATE_HOME =
            "CREATE TABLE " + TABLE_HOME + " (" +
                    COLUMN_TIME + " INTEGER PRIMARY KEY," +
                    COLUMN_TYPE + " VARCHAR," +
                    COLUMN_LABEL + " VARCHAR," +
                    COLUMN_X_POS + " INTEGER," +
                    COLUMN_Y_POS + " INTEGER," +
                    COLUMN_DATA + " VARCHAR," +
                    COLUMN_PAGE + " INTEGER," +
                    COLUMN_DESKTOP + " INTEGER," +
                    COLUMN_STATE + " INTEGER)";
    private static final String SQL_DELETE = "DROP TABLE IF EXISTS ";
    private static final String SQL_QUERY = "SELECT * FROM ";
    private SQLiteDatabase db;
    private Context context;

    public DatabaseHelper(Context c) {
        super(c, DATABASE_HOME, null, 1);
        db = getWritableDatabase();
        context = c;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_HOME);
        db.execSQL(SQL_CREATE_COUNT);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // discard the data and start over
        db.execSQL(SQL_DELETE + TABLE_HOME);
        db.execSQL(SQL_DELETE + TABLE_APP_COUNT);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void createItem(Item item, int page, Config.ItemPosition itemPosition) {
        ContentValues itemValues = new ContentValues();
        itemValues.put(COLUMN_TIME, item.getId());
        itemValues.put(COLUMN_TYPE, item.getType().toString());
        itemValues.put(COLUMN_LABEL, item.getLabel());
        itemValues.put(COLUMN_X_POS, item.getX());
        itemValues.put(COLUMN_Y_POS, item.getY());

        Setup.logger().log(this, Log.INFO, null, "createItem: %s (ID: %d)", item != null ? item.getLabel() : "NULL", item != null ? item.getId() : -1);

        String concat = "";
        switch (item.getType()) {
            case APP:
                if (Setup.appSettings().enableImageCaching()) {
                    Tool.saveIcon(context, Tool.drawableToBitmap(item.getIcon()), Integer.toString(item.getId()));
                }
                itemValues.put(COLUMN_DATA, Tool.getIntentAsString(item.getIntent()));
                break;
            case GROUP:
                for (Item tmp : item.getItems()) {
                    if (tmp != null) {
                        concat += tmp.getId() + Config.INT_SEP;
                    }
                }
                itemValues.put(COLUMN_DATA, concat);
                break;
            case ACTION:
                itemValues.put(COLUMN_DATA, item.getActionValue());
                break;
            case WIDGET:
                concat = Integer.toString(item.getWidgetValue()) + Config.INT_SEP
                        + Integer.toString(item.getSpanX()) + Config.INT_SEP
                        + Integer.toString(item.getSpanY());
                itemValues.put(COLUMN_DATA, concat);
                break;
        }
        itemValues.put(COLUMN_PAGE, page);
        itemValues.put(COLUMN_DESKTOP, itemPosition.ordinal());

        // item will always be visible when first added
        itemValues.put(COLUMN_STATE, 1);
        db.insert(TABLE_HOME, null, itemValues);
    }

    @Override
    public void saveItem(Item item) {
        updateItem(item);
    }

    @Override
    public void saveItem(Item item, Config.ItemState state) {
        updateItem(item, state);
    }

    @Override
    public void saveItem(Item item, int page, Config.ItemPosition itemPosition) {
        String SQL_QUERY_SPECIFIC = SQL_QUERY + TABLE_HOME + " WHERE " + COLUMN_TIME + " = " + item.getId();
        Cursor cursor = db.rawQuery(SQL_QUERY_SPECIFIC, null);
        if (cursor.getCount() == 0) {
            createItem(item, page, itemPosition);
        } else if (cursor.getCount() == 1) {
            updateItem(item, page, itemPosition);
        }
    }

    @Override
    public void deleteItem(Item item, boolean deleteSubItems) {
        // if the item is a group then remove all entries
        if (deleteSubItems && item.getType() == Item.Type.GROUP) {
            for (Item i : item.getGroupItems()) {
                deleteItem(i, deleteSubItems);
            }
        }
        // delete the item itself
        db.delete(TABLE_HOME, COLUMN_TIME + " = ?", new String[]{String.valueOf(item.getId())});
    }

    @Override
    public List<List<Item>> getDesktop() {
        String SQL_QUERY_DESKTOP = SQL_QUERY + TABLE_HOME;
        Cursor cursor = db.rawQuery(SQL_QUERY_DESKTOP, null);
        List<List<Item>> desktop = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                int page = Integer.parseInt(cursor.getString(6));
                int desktopVar = Integer.parseInt(cursor.getString(7));
                int stateVar = Integer.parseInt(cursor.getString(8));
                while (page >= desktop.size()) {
                    desktop.add(new ArrayList<>());
                }
                if (desktopVar == 1 && stateVar == 1) {
                    desktop.get(page).add(getSelection(cursor));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return desktop;
    }

    @Override
    public List<Item> getDock() {
        String SQL_QUERY_DESKTOP = SQL_QUERY + TABLE_HOME;
        Cursor cursor = db.rawQuery(SQL_QUERY_DESKTOP, null);
        List<Item> dock = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                int desktopVar = Integer.parseInt(cursor.getString(7));
                int stateVar = Integer.parseInt(cursor.getString(8));
                if (desktopVar == 0 && stateVar == 1) {
                    dock.add(getSelection(cursor));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        Log.i("DB Helper","database : dock size is "+ dock.size());
        return dock;
    }

    @Override
    public Item getItem(int id) {
        String SQL_QUERY_SPECIFIC = SQL_QUERY + TABLE_HOME + " WHERE " + COLUMN_TIME + " = " + id;
        Cursor cursor = db.rawQuery(SQL_QUERY_SPECIFIC, null);
        Item item = null;
        if (cursor.moveToFirst()) {
            item = getSelection(cursor);
        }
        cursor.close();
        return item;
    }

    // update data attribute for an item
    public void updateItem(Item item) {
        ContentValues itemValues = new ContentValues();
        itemValues.put(COLUMN_LABEL, item.getLabel());
        itemValues.put(COLUMN_X_POS, item.getX());
        itemValues.put(COLUMN_Y_POS, item.getY());

        Setup.logger().log(this, Log.INFO, null, "updateItem: %s (ID: %d)", item != null ? item.getLabel() : "NULL", item != null ? item.getId() : -1);

        String concat = "";
        switch (item.getType()) {
            case APP:
                if (Setup.appSettings().enableImageCaching()) {
                    Tool.saveIcon(context, Tool.drawableToBitmap(item.getIcon()), Integer.toString(item.getId()));
                }
                itemValues.put(COLUMN_DATA, Tool.getIntentAsString(item.getIntent()));
                break;
            case GROUP:
                for (Item tmp : item.getItems()) {
                    concat += tmp.getId() + Config.INT_SEP;
                }
                itemValues.put(COLUMN_DATA, concat);
                break;
            case ACTION:
                itemValues.put(COLUMN_DATA, item.getActionValue());
                break;
            case WIDGET:
                concat = Integer.toString(item.getWidgetValue()) + Config.INT_SEP
                        + Integer.toString(item.getSpanX()) + Config.INT_SEP
                        + Integer.toString(item.getSpanY());
                itemValues.put(COLUMN_DATA, concat);
                break;
        }
        db.update(TABLE_HOME, itemValues, COLUMN_TIME + " = " + item.getId(), null);
    }

    // update the state of an item
    private void updateItem(Item item, Config.ItemState state) {
        ContentValues itemValues = new ContentValues();
        Setup.logger().log(this, Log.INFO, null, "updateItem (state): %s (ID: %d)", item != null ? item.getLabel() : "NULL", item != null ? item.getId() : -1);
        itemValues.put(COLUMN_STATE, state.ordinal());
        db.update(TABLE_HOME, itemValues, COLUMN_TIME + " = " + item.getId(), null);
    }

    // update the fields only used by the database
    private void updateItem(Item item, int page, Config.ItemPosition itemPosition) {
        Setup.logger().log(this, Log.INFO, null, "updateItem (delete + create): %s (ID: %d)", item != null ? item.getLabel() : "NULL", item != null ? item.getId() : -1);
        deleteItem(item, false);
        createItem(item, page, itemPosition);
    }

    private Item getSelection(Cursor cursor) {
        Item item = new Item();
        int id = Integer.parseInt(cursor.getString(0));
        Item.Type type = Item.Type.valueOf(cursor.getString(1));
        String label = cursor.getString(2);
        int x = Integer.parseInt(cursor.getString(3));
        int y = Integer.parseInt(cursor.getString(4));
        String data = cursor.getString(5);

        item.setItemId(id);
        item.setLabel(label);
        item.setX(x);
        item.setY(y);
        item.setType(type);

        String[] dataSplit;
        switch (type) {
            case APP:
            case SHORTCUT:
                item.setIntent(Tool.getIntentFromString(data));
                if (Setup.appSettings().enableImageCaching()) {
                    item.setIcon(Tool.getIcon(HomeActivity.Companion.getLauncher(), Integer.toString(id)));
                } else {
                    switch (type) {
                        case APP:
                        case SHORTCUT:
                            App app = Setup.get().getAppLoader().findItemApp(item);
                            item.setIcon(app != null ? app.getIcon() : null);
                            break;
                        default:
                            break;
                    }
                }
                break;
            case GROUP:
                item.setItems(new ArrayList<>());
                dataSplit = data.split(Config.INT_SEP);
                for (String s : dataSplit) {
                    item.getItems().add(getItem(Integer.parseInt(s)));
                }
                break;
            case ACTION:
                item.setActionValue(Integer.parseInt(data));
                break;
            case WIDGET:
                dataSplit = data.split(Config.INT_SEP);
                item.setWidgetValue(Integer.parseInt(dataSplit[0]));
                item.setSpanX(Integer.parseInt(dataSplit[1]));
                item.setSpanY(Integer.parseInt(dataSplit[2]));
                break;
        }
        return item;
    }

    private static final String TABLE_APP_COUNT="app_count";
    private static final String COLUMN_PACKAGE_NAME = "package_name";
    private static final String COLUMN_PACKAGE_COUNT = "package_count";
    private static final String COLUMN_PACKAGE_ID = "count_id";
    private static final String SQL_CREATE_COUNT=
            "CREATE TABLE "+TABLE_APP_COUNT +" ("
                    +COLUMN_PACKAGE_ID + " INTEGER PRIMARY KEY,"
                    +COLUMN_PACKAGE_NAME + " VARCHAR, "
                    +COLUMN_PACKAGE_COUNT+ " INTEGER)";

    public void saveAppCount(String packageName){
        ContentValues itemValues = new ContentValues();
        itemValues.put(COLUMN_PACKAGE_NAME, packageName);
        itemValues.put(COLUMN_PACKAGE_COUNT, 0);
        Log.i("APP COUNT","Save App"+packageName);
        db.insert(TABLE_APP_COUNT, null, itemValues);
    }

    public void updateAppCount(String packageName) {
        String SQL_QUERY = "SELECT package_count FROM app_count WHERE package_name='" + packageName + "';";
        Cursor cursor = db.rawQuery(SQL_QUERY, null);
        int appCount = 0;
        if (cursor.moveToFirst()) {
                Log.i("APP COUNT", String.format("Cursor qty: %d", cursor.getInt(0)));
                appCount = cursor.getInt(0);
        }
        else{
            saveAppCount(packageName);
        }

        cursor.close();
        appCount++;
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PACKAGE_COUNT,appCount);
        Log.i("APP COUNT", String.format("Update App: %s, Count: %d", packageName, appCount));
        db.update(TABLE_APP_COUNT,cv,"package_name='"+packageName+"'",null);

    }

    @Override
    public int getAppCount(String packageName){
        String SQL_QUERY="SELECT package_count FROM app_count WHERE package_name='"+packageName+"';";
        Cursor cursor = db.rawQuery(SQL_QUERY, null);
        int appCount=0;
        if (cursor.moveToFirst()) {
           appCount = cursor.getInt(0);
        }
        cursor.close();
        return appCount;
    }

    @Override
    public void deleteApp(String packageName){
        db.delete(TABLE_APP_COUNT,"package_name='"+packageName+"'",null);
    }


}
