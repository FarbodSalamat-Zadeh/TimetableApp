package com.satsumasoftware.timetable.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.satsumasoftware.timetable.R;
import com.satsumasoftware.timetable.framework.Term;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;

public class TermsAdapter extends RecyclerView.Adapter<TermsAdapter.TermsViewHolder> {

    private ArrayList<Term> mTerms;

    public TermsAdapter(ArrayList<Term> terms) {
        mTerms = terms;
    }

    @Override
    public TermsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class_time, parent, false);
        return new TermsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TermsViewHolder holder, int position) {
        Term term = mTerms.get(position);

        holder.mName.setText(term.getName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
        String datesText = term.getStartDate().format(formatter) + " - " +
                term.getEndDate().format(formatter);
        holder.mDates.setText(datesText);
    }

    @Override
    public int getItemCount() {
        return mTerms.size();
    }

    public class TermsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView mName, mDates;

        TermsViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mName = (TextView) itemView.findViewById(R.id.time);
            mDates = (TextView) itemView.findViewById(R.id.day);
        }

        @Override
        public void onClick(View view) {
            if (mOnEntryClickListener != null) {
                mOnEntryClickListener.onEntryClick(view, getLayoutPosition());
            }
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
