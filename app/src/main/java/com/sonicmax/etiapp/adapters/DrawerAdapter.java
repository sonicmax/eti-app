package com.sonicmax.etiapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sonicmax.etiapp.R;
import com.sonicmax.etiapp.objects.Bookmark;

import java.util.List;


public class DrawerAdapter extends BaseAdapter {
    private Context mContext;
    private List<Bookmark> mBookmarks;

    public DrawerAdapter(Context context) {
        this.mContext = context;
    }

    public void setBookmarks(List<Bookmark> bookmarks) {
        mBookmarks = bookmarks;
    }

    @Override
    public int getCount() {
        return mBookmarks.size();
    }

    @Override
    public Bookmark getItem(int position) {
        return mBookmarks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_bookmark, parent, false);
            setViewHolder(convertView);
        }

        populateDrawerView((ViewHolder) convertView.getTag(), position);

        return convertView;
    }

    private void setViewHolder(View view) {
        TextView nameView = (TextView) view.findViewById(R.id.list_item_board_title);
        view.setTag(new ViewHolder(nameView));
    }

    private void populateDrawerView(ViewHolder viewHolder, int position) {
        Bookmark bookmark = mBookmarks.get(position);

        if (viewHolder != null) {
            viewHolder.nameView.setText(bookmark.getName());
        }
    }

    private class ViewHolder {
        final TextView nameView;

        ViewHolder(TextView nameView) {
            this.nameView = nameView;
        }
    }
}
