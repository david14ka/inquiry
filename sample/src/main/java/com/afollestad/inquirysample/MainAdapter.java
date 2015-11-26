package com.afollestad.inquirysample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MainVH> {

    public MainAdapter() {
    }

    public void setPhotos(Photo[] photos) {
        mPhotos = photos;
        notifyDataSetChanged();
    }

    private Photo[] mPhotos;

    @Override
    public MainVH onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_entry, parent, false);
        return new MainVH(v);
    }

    @Override
    public void onBindViewHolder(MainVH holder, int position) {
        final Photo photo = mPhotos[position];
        holder.title.setText(photo.getTitle());

        Glide.with(holder.itemView.getContext())
                .load(photo.getPath())
                .into(holder.thumbnail);
    }

    @Override
    public int getItemCount() {
        return mPhotos != null ? mPhotos.length : 0;
    }

    public class MainVH extends RecyclerView.ViewHolder {

        final ImageView thumbnail;
        final TextView title;

        public MainVH(View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            title = (TextView) itemView.findViewById(R.id.title);
        }
    }
}