package it.unipr.advmobdev.creditcardreader;

import android.content.Context;
import android.widget.Toast;

public class Utility
{
    private Context c;

    public Utility(Context context)
    {
        c = context;
    }


    public void displayToast(String text)
    {
        Context context = c.getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }


    // some other utilities if needed...
    // here
}
