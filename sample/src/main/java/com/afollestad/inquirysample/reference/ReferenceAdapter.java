package com.afollestad.inquirysample.reference;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * @author Heinrich Reimer (HeinrichReimer)
 */
public class ReferenceAdapter extends RecyclerView.Adapter<ReferenceAdapter.ReferenceVH> {

    public ReferenceAdapter() {
    }

    public void setPersons(Person[] persons) {
        this.persons = persons;
        notifyDataSetChanged();
    }

    private Person[] persons;

    @Override
    public ReferenceVH onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ReferenceVH(v);
    }

    @Override
    public void onBindViewHolder(ReferenceVH holder, int position) {
        final Person person = persons[position];
        holder.text.setText(person.toString());
    }

    @Override
    public int getItemCount() {
        return persons != null ? persons.length : 0;
    }

    public class ReferenceVH extends RecyclerView.ViewHolder {

        final TextView text;

        public ReferenceVH(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }
}