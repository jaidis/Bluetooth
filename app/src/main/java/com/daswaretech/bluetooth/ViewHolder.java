package com.daswaretech.bluetooth;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class ViewHolder extends RecyclerView.ViewHolder {
    private final TextView showInfo;
    private final TextView extraInfo;
    @NonNull
    private final View view;

    public final TextView getShowInfo() {
        return this.showInfo;
    }

    public final TextView getExtraInfo() {
        return this.extraInfo;
    }

    @NonNull
    public final View getView() {
        return this.view;
    }

    public ViewHolder(@NonNull View view) {
        super(view);
        this.view = view;
        this.showInfo = (TextView) view.findViewById(R.id.activity_info);
        Log.d("DASDEBUG", "ViewHolder: " + this.showInfo.toString());
        this.extraInfo = (TextView) view.findViewById(R.id.activity_extraInfo);
        Log.d("DASDEBUG", "ViewHolder: " + this.extraInfo.toString());
    }
}
