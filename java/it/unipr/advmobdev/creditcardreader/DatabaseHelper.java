package it.unipr.advmobdev.creditcardreader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.Map;

/* DATABASE STRUCTURE

    TABLE NAME: credit_cards_table:
    COLUMNS: ID (INT) | CARD NUMBER (STRING) | EXPIRATION MONTH (STRING) | EXPIRATION YEAR (STRING) | IMAGE PATH (STRING)

*/

public class DatabaseHelper extends SQLiteOpenHelper
{

    private static final String TAG = "DatabaseHelper";

    private static final String TABLE_NAME = "credit_cards_table";
    private static final String COL1 = "ID";
    private static final String COL2 = "card_number";
    private static final String COL3 = "expiration_month";
    private static final String COL4 = "expiration_year";
    private static final String COL5 = "imagePath";


    public DatabaseHelper(Context context) {
        super(context, TABLE_NAME, null, 1);
    }


    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL2 + " TEXT," + COL3 + " TEXT," + COL4 + " TEXT," + COL5 + " TEXT)";
        db.execSQL(createTable);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }


    public boolean addData(CreditCard c)
    {
        Map<String, String> data = c.getData();
        String number = data.get("number");
        String expMonth = data.get("expMonth");
        String expYear = data.get("expYear");
        String imgPath = data.get("imgPath");

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL2, number);
        contentValues.put(COL3, expMonth);
        contentValues.put(COL4, expYear);
        contentValues.put(COL5, imgPath);

        Log.d(TAG, "addData: Adding " + number + " to " + TABLE_NAME); // Debug

        long result = db.insert(TABLE_NAME, null, contentValues);

        // if data are inserted incorrectly it will return -1
        if (result == -1)
            return false;
        else
            return true;

    }


    public boolean isDuplicate(CreditCard c)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL2 + " FROM " + TABLE_NAME + ";",null);

        boolean cardPresent = false;
        while(cursor.moveToNext())
        {
            String recordedCard = cursor.getString(cursor.getColumnIndex("card_number"));
            if(recordedCard.equals(c.getCardNumber()))
            {
                cardPresent = true;
                break;
            }
        }

        return cardPresent;
    }


    public Cursor getData()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);

        return data;
    }


    public void deleteCard(CreditCard c)
    {
        String cardNumber = c.getCardNumber();
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "DELETE FROM " + TABLE_NAME + " WHERE "
                + COL2 + " = '" + cardNumber + "'";

        Log.d(TAG, "deleteName: Deleting " + cardNumber + " from database."); // Debug

        db.execSQL(query);
    }

}
