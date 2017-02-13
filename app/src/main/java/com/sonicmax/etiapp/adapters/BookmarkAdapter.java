package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sonicmax.etiapp.objects.Bookmark;
import com.sonicmax.etiapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookmarkAdapter extends BaseAdapter {

    private List<Bookmark> boards = new ArrayList<>();

    private final Context context;

    public BookmarkAdapter(Context context) {
        this.context = context;
    }

    public void updateBoards(List<Bookmark> boards) {
        this.boards = boards;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return boards.size();
    }

    public void clearBoards() {
        boards = Collections.emptyList();
        notifyDataSetChanged();
    }

    @Override
    public Bookmark getItem(int position) {
        return boards.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        public final TextView nameView;

        public ViewHolder(TextView nameView) {
            this.nameView = nameView;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TextView nameView;

        if (convertView == null) {

            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_bookmark, parent, false);
            nameView = (TextView) convertView.findViewById(R.id.list_item_board_title);
            convertView.setTag(new ViewHolder(nameView));

        } else {

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            nameView = viewHolder.nameView;

        }

        Bookmark board = getItem(position);
        nameView.setText(board.getName());

        return convertView;
    }

}
