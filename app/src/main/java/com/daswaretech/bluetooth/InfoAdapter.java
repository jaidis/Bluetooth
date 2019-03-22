package com.daswaretech.bluetooth;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;


public class InfoAdapter extends RecyclerView.Adapter<ViewHolder> {

    @NonNull
    private final ArrayList items;
    @NonNull
    private final Context context;

    public InfoAdapter(@NonNull ArrayList items, @NonNull Context context) {
        super();
        this.items = items;
        this.context = context;
    }

    @NonNull
    public final ArrayList getItems() {
        return this.items;
    }

    @NonNull
    public final Context getContext() {
        return this.context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.activity_adapter, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {

        TextView showInfo = viewHolder.getShowInfo();
        TextView extraInfo = viewHolder.getExtraInfo();

        if (showInfo != null)
            showInfo.setText(this.items.get(i).toString());

        if(extraInfo != null){
            extraInfo.setText(this.items.get(i).toString());
        }
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }
}


