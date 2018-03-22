package com.leadcore.demoliveplayer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.cmteam.cloudmedia.CloudMedia;
import com.cmteam.cloudmedia.Node;
import com.cmteam.cloudmedia.NodesList;
import com.cmteam.cloudmedia.PullNode;
import com.leadcore.demoliveplayer.customviews.CustomDialog;
import com.leadcore.demoliveplayer.customviews.RvDividerItemDecoration;
import com.leadcore.demoliveplayer.customviews.SecondaryListAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VideoListActivity extends AppCompatActivity {

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private final static String TAG = "VideoListActivity";
    private ViewPager mViewPager;
    private List<View> mViews = new ArrayList<>();
    private List<String> mPagerTitle = new ArrayList<>();
    private ListView mLiveListView;
    private ListAdapter mLiveListAdapter;

    private RecyclerView mRV;
    private RecyclerAdapter mRAdapter;
    private PullNode mPullNode;
    private List<Node> mNodes = new ArrayList<>();
    private final static String NICK_NAME = "PULLER0";
    private String mUrl;
    private String mToPlayNodeId;
    private boolean mIsWating = false;
    private CustomDialog mWaitDialog;
    private static final int STARTPLAYER_REQUEST_CODE = 1;
    private List<SecondaryListAdapter.DataTree<GroupItem, Node>> mDatas = new ArrayList<>();

    enum SourceType {
        FLV,
        HLS,
        RTMP
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);
        //not used:2018-03-19
        PagerTabStrip tab = findViewById(R.id.pagertab);
        tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        mPagerTitle.add("LiveVideo");
        mPagerTitle.add("VOD Video");

        LayoutInflater inflater = LayoutInflater.from(this);
        View view1 = inflater.inflate(R.layout.fragment_live_list,null);
        View view2 = inflater.inflate(R.layout.fragment_vod_list, null);
//        mViews.add(view1);
//        mViews.add(view2);
//        initLiveView(view1);
//        mViewPager = findViewById(R.id.container);
//        mViewPager.setAdapter(mPagerAdapter);

        //use RecyclerView:2018-03-19
        mRV = findViewById(R.id.rv);
        initRecyclerView();

        initPullNode();
    }

    private void initRecyclerView() {
        mRV.setLayoutManager(new LinearLayoutManager(this));
        mRV.setHasFixedSize(true);
        mRV.addItemDecoration(new RvDividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        mRAdapter = new RecyclerAdapter(this);
        mRAdapter.setData(mDatas);
        mRAdapter.setSubItemClickInterface(new RecyclerAdapter.SubItemClickInterface() {
            @Override
            public void onSubItemClick(int groupIndext, int subIndex) {
                startPlay(groupIndext, subIndex);
            }
        });
        mRV.setAdapter(mRAdapter);
    }
    private void initLiveView(View liveView) {
        mLiveListView = liveView.findViewById(R.id.livelist);
        mLiveListAdapter = new ListAdapter(this);
        mLiveListView.setAdapter(mLiveListAdapter);
    }


//    {
//        for (int i = 0; i < 10; i++) {
//
//            datas.add(new SecondaryListAdapter.DataTree<String, String>(String.valueOf(i), new
//                    ArrayList<String>(){{add("sub 0"); add("sub 1"); add("sub 2");}}));
//
//        }
//    }



    private void initPullNode() {
        mPullNode = CloudMedia.declarePullNode(getApplicationContext(), NICK_NAME, "default");
        mPullNode.setNodesListChangeListener(new PullNode.OnNodesListChange() {
            @Override
            public void onNodesListChange(NodesList nodesList) {
                Log.d(TAG, "OnNodesListChange");
                updateNodeList(nodesList);
            }
        });
        Log.d(TAG, "to connect");
        mPullNode.connect("", "", "g123", "gn", "v123", "vn", new CloudMedia.RPCResultListener() {
            @Override
            public void onSuccess(String s) {
                Log.d(TAG, "CloudMedia connect successed!");
                mPullNode.setMessageListener(new CloudMedia.OnMessageListener() {
                    @Override
                    public void onMessage(String s, String s1, String s2) {
                        Log.d(TAG, "onMessage:s = "+s+", s1 = "+s1+", s2 ="+s2);
                    }
                });
                Toast.makeText(VideoListActivity.this,
                        getString(R.string.cloudmedia_connect_success), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String s) {
                Log.d(TAG, "CloudMedia connect failed!");
                Toast.makeText(VideoListActivity.this,
                        getString(R.string.cloudmedia_connect_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNodeList(NodesList nodesList) {
        if(nodesList == null) {
            Log.d(TAG, "updateNodelist failed!");
            return;
        }

        List<Node> allList = nodesList.getAllOnlineList();
        if (allList == null || allList.size() == 0) {
            List<Node> newList = nodesList.getNewOnlineList();
            if (newList != null && newList.size() > 0) {
                Log.d(TAG, "updateNodeList newList size:"+newList.size());
                addInfoList(newList);
            }
            List<Node> deleList = nodesList.getNewOfflineList();
            if (deleList != null && deleList.size() > 0) {
                Log.d(TAG, "updateNodeList deleList size:"+deleList.size());
                deleteList(deleList);
            }
            List<Node> upList = nodesList.getNewUpdateList();
            if (upList != null && upList.size() > 0) {
                Log.d(TAG, "updateNodeList updateList size:"+upList.size());
                updateList(upList);
            }
        }else {
            Log.d(TAG, "updateNodeList allList size:"+allList.size());
            mNodes.clear();
            mDatas.clear();
            addInfoList(allList);
        }
//        mLiveListAdapter.notifyDataSetChanged();
        mRAdapter.setData(mDatas);
    }

    private void addRecyclerList(List<Node> nodelist) {
        boolean addedGrop = false;
        for (int i = 0; i < nodelist.size(); i++) {
            for (int j = 0; j < mDatas.size(); j++) {
                if (nodelist.get(i).getGroupID().equals(mDatas.get(j).getGroupItem().groupID)) {
                    addedGrop = true;
                    List<Node> subList = mDatas.get(j).getSubItems();
                    subList.add(nodelist.get(i));
                    Log.d(TAG, "add subItem node nick = "
                            +nodelist.get(i).getNick()+"(groupID:"+mDatas.get(j).getGroupItem().groupID
                            +",groupNick:"+mDatas.get(j).getGroupItem().groupNick+")");
                    break;
                }
            }
            if (!addedGrop) {
                GroupItem group = new GroupItem();
                Node node = nodelist.get(i);
                group.groupID = node.getGroupID();
                group.groupNick = node.getGroupNick();
                List<Node> nodeList = new ArrayList<>();
                nodeList.add(node);
                List<String> sub = new ArrayList<>();
                sub.add("OK");
                Log.d(TAG, "add groupNick = "
                        +group.groupNick+", subItemNick:"+node.getNick());
                mDatas.add(new SecondaryListAdapter.DataTree<GroupItem, Node>(group, nodeList));
            }
            addedGrop = false;
        }
    }

    private void addInfoList(List<Node> newList) {
        Log.d(TAG, "addInfoList");
        mNodes.addAll(newList);
        addRecyclerList(newList);
        Log.d(TAG, "mDatas.size = "+mDatas.size());
        Log.d(TAG, "mDatas.sublist.size = "+mDatas.get(0).getSubItems().size());
    }

    private void deleteList(List<Node> deleList) {
        for(int i = 0; i < deleList.size(); i++) {
            Log.d(TAG, "deleteList:CnodeId = "+deleList.get(i).getID());
            Log.d(TAG, "deleteList:CnodeNick = "+deleList.get(i).getNick());
//            for (int j = 0; j < mNodes.size(); j++) {
//                if (mNodes.get(j).getID().equals(deleList.get(i).getID())) {
//                    mNodes.remove(j);
//                    Log.d(TAG, "remove:NodeId = "
//                            +deleList.get(i).getID()+", NickName = "+deleList.get(i).getNick());
//                    break;
//                }
//            }
            for (int j = 0; j < mDatas.size(); j++) {
                if (deleList.get(i).getGroupID().equals(mDatas.get(j).getGroupItem().groupID)) {
                    List<Node> nodeList = mDatas.get(j).getSubItems();
                    for (int k = 0; k < nodeList.size(); k++) {
                        if (deleList.get(i).getID().equals(nodeList.get(k).getID())) {
                            nodeList.remove(k);
                            Log.d(TAG, "remove:GropNick = "
                                    +deleList.get(i).getGroupNick()+", NodeNick = "+deleList.get(i).getNick());
                        }
                    }

                }
            }

        }
    }

    private void updateList(List<Node> upList) {
        List<Node> groupChangedList = new ArrayList<>();
        for(int i = 0; i < upList.size(); i++) {
//            for (int j = 0; j < mNodes.size(); j++) {
//                if (mNodes.get(j).getID().equals(upList.get(i).getID())) {
//                    Log.d(TAG, "updateList:nodeId = "+mNodes.get(j).getID());
//                    Log.d(TAG, "updateList:nodeNick = "+mNodes.get(j).getNick());
//                    mNodes.remove(j);
//                    mNodes.add(j, upList.get(i));
//                    break;
//                }
//            }
            for (int j = 0; j < mDatas.size(); j++) {
                List<Node> nodeList = mDatas.get(j).getSubItems();
                for (int k = 0; k < nodeList.size(); k++) {
                    if (upList.get(i).getID().equals(nodeList.get(k).getID())) {
                        nodeList.remove(k);
                        if (upList.get(i).getGroupID().equals(nodeList.get(k).getGroupID())) {
                            nodeList.add(k, upList.get(i));
                        }else {
                            groupChangedList.add(upList.get(i));
                        }
                    }
                }
            }
        }
        addRecyclerList(groupChangedList);
    }


    private void startPlay(int groupIndex, int subIndex) {
        Log.d(TAG, "startPlay:groupIndex = "+groupIndex+", subIndex"+subIndex);

        Node pushNode = mDatas.get(groupIndex).getSubItems().get(subIndex);//mNodes.get(index);
        mToPlayNodeId = pushNode.getID();

        Log.d(TAG, "sendMessage to : nodeId = "+pushNode.getID());
        mPullNode.sendMessage(pushNode.getGroupID(), pushNode.getID(), "puller0: start push!");
        mPullNode.setStreamExceptionListener(pushNode, new PullNode.OnStreamException() {
            @Override
            public void onStreamException(String s, CloudMedia.CMStreamException e) {
                Log.d(TAG, "onStreamException: "+e.str());
                if (mIsWating) {
                    mIsWating = false;
                    Toast.makeText(VideoListActivity.this,
                            getText(R.string.video_exception_start_push), Toast.LENGTH_LONG).show();
                    dismissWaitDialog();
                }else {
                    Intent intent = new Intent(VideoPlayerActivity.STREAM_EXCEPTION_MSG);
                    VideoListActivity.this.sendBroadcast(intent);
                }
            }
        });
        mPullNode.startPushMedia(pushNode, new CloudMedia.RPCResultListener() {
            @Override
            public void onSuccess(String s) {
                Log.d(TAG, "startPushMedia onSuccess:"+s);
                if (mIsWating) {
                    mIsWating = false;
                    dismissWaitDialog();
                    try {
                        JSONObject jsonObj = new JSONObject(s);
                        if(jsonObj.has("url")) {
                            mUrl = jsonObj.getString("url");
                            Log.d(TAG, "url = "+mUrl);
                            startPlayer();
                            mPullNode.updateStreamStatus(CloudMedia.CMStreamStatus.PULLING, new CloudMedia.RPCResultListener() {
                                @Override
                                public void onSuccess(String s) {
                                    Log.d(TAG, "startPushMedia updateStreamStatus onSuccess:"+s);
                                }

                                @Override
                                public void onFailure(String s) {
                                    Log.d(TAG, "startPushMedia updateStreamStatus onFailure:"+s);
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void onFailure(String s) {
                Log.d(TAG, "startPushMedia onFailure:"+s);
                if (mIsWating) {
                    mIsWating = false;
                    dismissWaitDialog();
                }
                Toast.makeText(VideoListActivity.this,
                        getText(R.string.video_error_start_push), Toast.LENGTH_LONG).show();
            }
        });
        mIsWating = true;
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View waitView = inflater.inflate(R.layout.waiting_dialog, null);
        mWaitDialog = new CustomDialog.Builder(this)
                .create(waitView, R.style.MyWaitDailog, Gravity.CENTER);
        mWaitDialog.setDialogOnKeyDownListner(new CustomDialog.DialogOnKeyDownListner() {
            @Override
            public void onKeyDownListener(int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && mIsWating) {
                    Log.d(TAG, "Dialog back key down");
                    mIsWating = false;
                    dismissWaitDialog();
                    stopPushMedia(mToPlayNodeId);
                }
            }
        });
        mWaitDialog.show();
    }

    private void dismissWaitDialog() {
        if (mWaitDialog != null) {
            mWaitDialog.dismiss();
            mWaitDialog = null;
        }
    }

    private void startPlayer() {
        Intent intent = new Intent(VideoListActivity.this, VideoPlayerActivity.class);
        intent.putExtra("nodeId", mToPlayNodeId);
        intent.putExtra("url", mUrl);
        startActivityForResult(intent,STARTPLAYER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == STARTPLAYER_REQUEST_CODE) {
            Log.d(TAG, "onActivityResult");
            String nodeId = data.getExtras().getString("nodeId");
            stopPushMedia(nodeId);
        }
    }

    private void stopPushMedia(String nodeId) {
        if(mNodes != null)  {
            for (int i = 0; i < mNodes.size(); i++) {
                if(nodeId.equals(mNodes.get(i).getID())) {
                    mPullNode.stopPushMedia(mNodes.get(i), new CloudMedia.RPCResultListener() {
                                @Override
                                public void onSuccess(String s) {
                                    Log.d(TAG, "stopPushMedia onSuccess s = "+s);
                                    mPullNode.updateStreamStatus(CloudMedia.CMStreamStatus.PULLING_CLOSE, new CloudMedia.RPCResultListener() {
                                        @Override
                                        public void onSuccess(String s) {
                                            Log.d(TAG, "stopPushMedia updateStreamStatus onSuccess:"+s);
                                        }

                                        @Override
                                        public void onFailure(String s) {
                                            Log.d(TAG, "stopPushMedia updateStreamStatus onFailure:"+s);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(String s) {
                                    Log.d(TAG, "stopPushMedia onFailure s = "+s);
                                }
                            });
                    break;
                }
            }
        }
    }

    private class ViewHolder {
        int index;
        TextView nodeIdTv;
        TextView nickNameTv;
        Button flvBtn;
        Button hlsBtn;
        Button rtmpBtn;
    }

    class ListAdapter extends BaseAdapter /*implements View.OnClickListener*/{
        private LayoutInflater mInflater;
        public  ListAdapter(Context context) {
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            return mNodes.size();
        }

        @Override
        public Object getItem(int i) {
            return mNodes.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if(view == null) {
                view = mInflater.inflate(R.layout.livelist_item_layout, viewGroup, false);
                holder = new ViewHolder();
                holder.nodeIdTv = view.findViewById(R.id.nodeid);
                holder.nickNameTv = view.findViewById(R.id.nickname);
//                holder.flvBtn = (Button)view.findViewById(R.id.flvplay);
//                holder.hlsBtn = (Button)view.findViewById(R.id.hlsplay);
//                holder.rtmpBtn = (Button)view.findViewById(R.id.rtmpplay);
                view.setTag(holder);
            }else {
                holder = (ViewHolder)view.getTag();
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewHolder clickHolder = (ViewHolder) v.getTag();
                    Log.d(TAG, "list item click: "+clickHolder.index);
//                    startPlay(clickHolder.index, SourceType.RTMP);
                }
            });
            holder.index = i;
            holder.nodeIdTv.setText(mNodes.get(i).getID());
            holder.nickNameTv.setText(mNodes.get(i).getNick());
//            holder.flvBtn.setTag(R.id.flvbtn, i);
//            holder.flvBtn.setOnClickListener(this);
//            holder.hlsBtn.setTag(R.id.hlsbtn, i);
//            holder.hlsBtn.setOnClickListener(this);
//            holder.rtmpBtn.setTag(R.id.rtmpbtn, i);
//            holder.rtmpBtn.setOnClickListener(this);
            return view;
        }

//        @Override
//        public void onClick(View view) {
//            int index = 0;
//            SourceType type = SourceType.RTMP;
//            switch (view.getId()) {
//                case R.id.flvplay:
//                    index = (int) view.getTag(R.id.flvbtn);
//                    type = SourceType.FLV;
//                    break;
//                case R.id.hlsplay:
//                    index = (int) view.getTag(R.id.hlsbtn);
//                    type = SourceType.HLS;
//                    break;
//                case R.id.rtmpplay:
//                    index = (int) view.getTag(R.id.rtmpbtn);
//                    type = SourceType.RTMP;
//                    break;
//            }
//            startPlay(index, type);
//        }
    }

    private PagerAdapter mPagerAdapter = new PagerAdapter() {
        @Override
        public int getCount() {
            return mViews.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mViews.get(position));
            return mViews.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPagerTitle.get(position);
        }
    };

    @Override
    public void onBackPressed() {
        if (mIsWating) {
            mIsWating = false;
            dismissWaitDialog();
            stopPushMedia(mToPlayNodeId);
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if(mPullNode != null) {
            Log.d(TAG, "disconnect");
            mPullNode.disconnect();
        }
    }
}
