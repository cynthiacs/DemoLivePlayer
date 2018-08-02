package com.leadcore.demoliveplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cmteam.cloudmedia.CloudMedia;
import com.cmteam.cloudmedia.Node;
import com.leadcore.demoliveplayer.customviews.SecondaryListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cs on 2018/3/20.
 */

public class RecyclerAdapter extends SecondaryListAdapter<RecyclerAdapter.GroupItemViewHolder, RecyclerAdapter.SubItemViewHolder> {
    private final static String TAG = "RecyclerAdapter";
    private Context context;
    private SubItemClickInterface mSubItemClickInterface;

    private List<DataTree<GroupItem, Node>> dts = new ArrayList<>();

    public RecyclerAdapter(Context context) {
        this.context = context;
    }

    public void setData(List datas) {
        Log.d(TAG, "setData");
        dts = datas;
        notifyNewData(dts);
    }

    public void setSubItemClickInterface(SubItemClickInterface subItemInterface) {
        mSubItemClickInterface = subItemInterface;
    }

    @Override
    public RecyclerView.ViewHolder groupItemViewHolder(ViewGroup parent) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_item, parent, false);

        return new GroupItemViewHolder(v);
    }

    @Override
    public RecyclerView.ViewHolder subItemViewHolder(ViewGroup parent) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.sub_item, parent, false);
        return new SubItemViewHolder(v);
    }

    @Override
    public void onGroupItemBindViewHolder(RecyclerView.ViewHolder holder, int groupItemIndex) {
        Log.d(TAG, "onGroupItemBindViewHolder:dts.size = "+dts.size()+"groupItemIndex = "+groupItemIndex);
        ((GroupItemViewHolder) holder).tvGroup.setText(dts.get(groupItemIndex).getGroupItem().groupNick);

    }

    @Override
    public void onSubItemBindViewHolder(RecyclerView.ViewHolder holder, int groupItemIndex, int subItemIndex) {

        ((SubItemViewHolder) holder).tvSub.setText(dts.get(groupItemIndex).getSubItems().get(subItemIndex).getDeviceName());

        if (dts.get(groupItemIndex).getSubItems().get(subItemIndex).getStreamStatus().equals(CloudMedia.CMStreamStatus.PUSHING.str())) {
            ((SubItemViewHolder) holder).ivItemRight.setVisibility(View.VISIBLE);
        }else {
            ((SubItemViewHolder) holder).ivItemRight.setVisibility(View.GONE);
        }

    }

    @Override
    public void onGroupItemClick(Boolean isExpand, GroupItemViewHolder holder, int groupItemIndex) {
        Log.d(TAG, "onGroupItemClick");
        if (!isExpand) {
            holder.ivGroupright.setImageResource(R.drawable.icon_expand);
        }else {
            holder.ivGroupright.setImageResource(R.drawable.icon_up);
        }

//        Toast.makeText(context, "group item " + String.valueOf(groupItemIndex) + " is expand " +
//                String.valueOf(isExpand), Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onSubItemClick(SubItemViewHolder holder, int groupItemIndex, int subItemIndex) {
        Log.d(TAG, "onSubItemClick");
        mSubItemClickInterface.onSubItemClick(groupItemIndex, subItemIndex);
//        Toast.makeText(context, "sub item " + String.valueOf(subItemIndex) + " in group item " +
//                String.valueOf(groupItemIndex), Toast.LENGTH_SHORT).show();

    }

    public static class GroupItemViewHolder extends RecyclerView.ViewHolder {

        TextView tvGroup;
        ImageView ivGroupright;

        public GroupItemViewHolder(View itemView) {
            super(itemView);

           tvGroup = itemView.findViewById(R.id.group_title);
            ivGroupright = itemView.findViewById(R.id.gpicon_right);
        }
    }

    public static class SubItemViewHolder extends RecyclerView.ViewHolder {

        TextView tvSub;
        ImageView ivItemRight;

        public SubItemViewHolder(View itemView) {
            super(itemView);
            tvSub = itemView.findViewById(R.id.sub_title);
            ivItemRight = itemView.findViewById(R.id.subicon_right);
        }
    }

    public interface SubItemClickInterface {
        void onSubItemClick(int groupIndext, int subIndex);
    }
}

