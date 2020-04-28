package it.unipr.advmobdev.creditcardreader;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;


public class ListDataActivity extends AppCompatActivity
{

    private static final String TAG = "ListDataActivity";
    private RecyclerView recyclerView;
    private MyAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<CreditCard> cardDataSet;

    DatabaseHelper mDatabaseHelper = new DatabaseHelper(this); // DB
    Utility util;

    private int verticalSpaceHeight = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_layout);

        // Utility
        util = new Utility(getApplicationContext());

        // DB
        mDatabaseHelper = new DatabaseHelper(this);

        // Recycler View
        recyclerViewSetup();
    }

    @Override
    // Menu Options: 1) Delete card list
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.list_data_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.action_delete_card_list: // Delete cards list
                getApplicationContext().deleteDatabase("credit_cards_table");
                cardDataSet.clear();
                mAdapter.notifyDataSetChanged(); // updating view
                util.displayToast("All cards deleted successfully");
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    // Card display view
    public void recyclerViewSetup()
    {
        recyclerView = (RecyclerView) findViewById(R.id.cards_recycler_view);

        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        VerticalSpacingItemDecorator itemDecorator = new VerticalSpacingItemDecorator(verticalSpaceHeight);
        recyclerView.addItemDecoration(itemDecorator);
        recyclerView.setItemAnimator(new DefaultItemAnimator());


        // read Data from DB get the data and append to a list
        Cursor data = mDatabaseHelper.getData();
        cardDataSet = new ArrayList<>();
        CreditCard c;
        while(data.moveToNext())
        {
            c = new CreditCard(data.getString(1), data.getString(2),
                    data.getString(3), data.getString(4));
            cardDataSet.add(c);
        }

        // Adapter
        adapterSetup();
    }


    private void adapterSetup()
    {
        //create the list adapter and set the adapter
        mAdapter = new MyAdapter(this, cardDataSet);
        recyclerView.setAdapter(mAdapter);

        // On Click Listener if needed
        mAdapter.setOnItemClickListener(new MyAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(final int position)
            {
                final CharSequence[] options = {"Delete card"}; // expandable if needed...

                AlertDialog.Builder builder = new AlertDialog.Builder(ListDataActivity.this);
                builder.setTitle("Options");

                builder.setItems(options, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int item)
                    {
                        if (options[item].equals("Delete card"))
                        {
                            mDatabaseHelper.deleteCard(cardDataSet.get(position));
                            cardDataSet.remove(position);
                            mAdapter.notifyItemRemoved(position);
                            util.displayToast("Card deleted");
                        }
                    }
                });
                builder.show();
            }
        });
    }

}
