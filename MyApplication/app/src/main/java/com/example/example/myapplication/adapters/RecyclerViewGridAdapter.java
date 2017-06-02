package com.example.example.myapplication.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.example.myapplication.R;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.ImageTile;
import com.example.example.myapplication.utils.Utils;
import com.google.common.io.Files;

import org.crypto.sse.CryptoPrimitives;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Adapter for recycler view to make it look like a grid
 */
public class RecyclerViewGridAdapter extends RecyclerView.Adapter<RecyclerViewGridAdapter.ViewHolder> {

    private List<ImageTile> mImageTiles;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Activity mActivity;
    private boolean encrypted = false;

    // data is passed into the constructor
    public RecyclerViewGridAdapter(Context context, List<ImageTile> data, boolean encrypted) {
        this.mInflater = LayoutInflater.from(context);
        this.mImageTiles = data;
        this.mActivity = (Activity) context;
        this.encrypted = encrypted;
    }

    // inflates the cell layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.image_list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ImageTile tile = this.mImageTiles.get(position);
        if (encrypted) {
            byte[] sk = null;
            try {
                sk = Utils.getSk(mActivity);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            try {
                byte[] encThumb = Files.toByteArray(tile.file);
                byte[] decThumb = CryptoPrimitives.decryptAES_CTR_String(encThumb, sk);
                Bitmap thumbBitmap = BitmapFactory.decodeByteArray(decThumb, 0, decThumb.length);
                holder.thumbnail.setImageBitmap(Utils.cropBitmap(thumbBitmap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Glide.with(mActivity)
                    .load(tile.file)
                    .centerCrop()
                    .into(holder.thumbnail);
        }
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return this.mImageTiles.size();
    }

    public void clear() {
        mImageTiles.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<ImageTile> tiles) {
        mImageTiles.addAll(tiles);
        notifyDataSetChanged();
    }

    // convenience method for getting data at click position
    public ImageTile getItem(int id) {
        return this.mImageTiles.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public List<ImageTile> getTiles() {
        return this.mImageTiles;
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ImageView thumbnail;

        public ViewHolder(View itemView) {
            super(itemView);
            this.thumbnail = (ImageView) itemView.findViewById(R.id.img);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }
}

