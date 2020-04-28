package it.unipr.advmobdev.creditcardreader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder>
{
    private static final String TAG = "RecyclerViewAdapter";
    private Context context;
    private int iconWidth = 200;
    private int iconHeight = 200;

    private ArrayList<CreditCard> cardDataset;

    // ITEM LISTENER
    private OnItemClickListener mListener;
    public interface OnItemClickListener { void onItemClick(int position); }
    public void setOnItemClickListener(OnItemClickListener listener) { mListener = listener; }
    // END OF ITEM LISTENER


    // Provide a reference to the views for each data item
    public static class MyViewHolder extends RecyclerView.ViewHolder
    {
        ImageView cardPicture;
        TextView cardNumberTV;
        TextView expirationDateTV;
        ImageButton shareButton;

        public MyViewHolder(View itemView, final OnItemClickListener listener)
        {
            super(itemView);
            this.cardPicture = (ImageView) itemView.findViewById(R.id.card_picture);
            this.cardNumberTV = (TextView) itemView.findViewById(R.id.cardNumberTV);
            this.expirationDateTV = (TextView) itemView.findViewById(R.id.expirationDateTV);
            this.shareButton = (ImageButton) itemView.findViewById(R.id.shareButton);

            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (listener != null)
                    {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(Context c, ArrayList<CreditCard> myDataset)
    {
        context = c;
        cardDataset = myDataset;
    }


    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType)
    {

        CardView v = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_view_layout, parent, false);

        MyViewHolder vh = new MyViewHolder(v, mListener);
        return vh;
    }


    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position)
    {
        ImageView cardPicture = holder.cardPicture;
        TextView cardNumberTV = holder.cardNumberTV;
        TextView expirationDateTV = holder.expirationDateTV;
        ImageButton shareButton = holder.shareButton;


        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(cardDataset.get(position).getImagePath(), bmOptions);
        bitmap = Bitmap.createScaledBitmap(bitmap, iconWidth, iconHeight,true);
        cardPicture.setImageBitmap(bitmap);

        final String cNumber = cardDataset.get(position).getCardNumber();
        cardNumberTV.setText(cNumber);

        final String expDate = cardDataset.get(position).getExpirationMonth() + "/" + cardDataset.get(position).getExpirationYear();
        expirationDateTV.setText(expDate);

        // SHARE
        shareButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String shareString = "My card info are:\n" +
                                    "Card number: " + cNumber + "\n" +
                                    "Expires: " + expDate;
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareString);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(shareIntent);
            }
        });
    }


    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount()
    {
        return cardDataset.size();
    }
}
