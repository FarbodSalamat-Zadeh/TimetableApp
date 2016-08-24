package com.satsumasoftware.timetable.ui.adapter;

import android.app.Application;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.satsumasoftware.timetable.R;
import com.satsumasoftware.timetable.TimetableApplication;
import com.satsumasoftware.timetable.framework.Timetable;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;

public class TimetablesAdapter extends RecyclerView.Adapter<TimetablesAdapter.TimetablesViewHolder> {

    private Application mApplication;
    private ArrayList<Timetable> mTimetables;

    private boolean mBindingVH;

    public TimetablesAdapter(Application application, ArrayList<Timetable> timetables) {
        mApplication = application;
        mTimetables = timetables;
    }

    @Override
    public TimetablesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timetable, parent, false);
        return new TimetablesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TimetablesViewHolder holder, int position) {
        mBindingVH = true;

        Timetable timetable = mTimetables.get(position);

        holder.mName.setText(timetable.getDisplayedName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM uu");
        String detailText = timetable.getStartDate().format(formatter) + " - " +
                timetable.getEndDate().format(formatter);
        holder.mDetails.setText(detailText);

        Timetable currentTimetable = ((TimetableApplication) mApplication).getCurrentTimetable();
        assert currentTimetable != null;
        boolean isCurrent = currentTimetable.getId() == timetable.getId();

        holder.mRadioButton.setChecked(isCurrent);

        mBindingVH = false;
    }

    @Override
    public int getItemCount() {
        return mTimetables.size();
    }

    public class TimetablesViewHolder extends RecyclerView.ViewHolder {

        RadioButton mRadioButton;
        TextView mName, mDetails;

        TimetablesViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnEntryClickListener != null) {
                        mOnEntryClickListener.onEntryClick(view, getLayoutPosition());
                    }
                }
            });

            mRadioButton = (RadioButton) itemView.findViewById(R.id.radioButton);
            mRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (!mBindingVH) {
                        Timetable timetable = mTimetables.get(getLayoutPosition());
                        ((TimetableApplication) mApplication).setCurrentTimetable(timetable);
                        notifyDataSetChanged();
                    }
                }
            });

            mName = (TextView) itemView.findViewById(R.id.name);
            mDetails = (TextView) itemView.findViewById(R.id.details);
        }
    }

    private OnEntryClickListener mOnEntryClickListener;

    public interface OnEntryClickListener {
        void onEntryClick(View view, int position);
    }

    public void setOnEntryClickListener(OnEntryClickListener onEntryClickListener) {
        mOnEntryClickListener = onEntryClickListener;
    }

}
