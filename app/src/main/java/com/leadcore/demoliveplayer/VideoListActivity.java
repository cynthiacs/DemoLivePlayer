package com.leadcore.demoliveplayer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.cloudmedia.CloudMedia;
import com.example.cloudmedia.RemoteMediaNode;

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
    private ProgressBar mPbar;
    private SurfaceView mSurfaceView;
    private ListView mLiveListView;
    private ListAdapter mLiveListAdapter;
    private CloudMedia mCloudMedia;
    private List<CloudMedia.Node> mNodes = new ArrayList<>();
    private final static String NICK_NAME = "PULLER0";
    private String mUrl;
    private String mToPlayNodeId;
    private boolean mIsWating = false;
    private static final int STARTPLAYER_REQUEST_CODE = 1;

    enum SourceType {
        FLV,
        HLS,
        RTMP
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);
        PagerTabStrip tab = findViewById(R.id.pagertab);
        tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        mPagerTitle.add("LiveVideo");
        mPagerTitle.add("VOD Video");

        LayoutInflater inflater = LayoutInflater.from(this);
        View view1 = inflater.inflate(R.layout.fragment_live_list,null);
        View view2 = inflater.inflate(R.layout.fragment_vod_list, null);
        mViews.add(view1);
        mViews.add(view2);
        initLiveView(view1);
        initCloudMedia();
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mPagerAdapter);
    }

    private void initLiveView(View liveView) {
        /*mSurfaceView = (SurfaceView)liveView.findViewById(R.id.surfaceview);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                surfaceHolder.setKeepScreenOn(true);
                Log.d(TAG, "surfaceCreated "+mPlayer);
                if(mPlayer != null) {
                    mPlayer.setVideoSurface(mSurfaceView.getHolder().getSurface());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d(TAG, "surfaceChanged");
                if (mPlayer != null) {
                    mPlayer.setSurfaceChanged();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceDestroyed");
            }
        });*/
        mLiveListView = liveView.findViewById(R.id.livelist);
        mLiveListAdapter = new ListAdapter(this);
        mLiveListView.setAdapter(mLiveListAdapter);
        mPbar = liveView.findViewById(R.id.startplaypb);
    }

    private void initCloudMedia() {
        mCloudMedia = new CloudMedia(getApplicationContext());
        mCloudMedia.setNodesListChangeListener(new CloudMedia.OnNodesListChange() {
            @Override
            public void onNodesListChange(CloudMedia.NodesList nodesList) {
                Log.d(TAG, "OnNodesListChange");
                updateNodeList(nodesList);
            }
        });
        Log.d(TAG, "to connect");
        mCloudMedia.connect(NICK_NAME, CloudMedia.CMRole.ROLE_PULLER, new CloudMedia.RPCResultListener() {
            @Override
            public void onSuccess(String s) {
                Log.d(TAG, "CloudMedia connect successed!");
                setMessageListener();
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

    private void updateNodeList(CloudMedia.NodesList nodesList) {
        if(nodesList == null) {
            Log.d(TAG, "updateNodelist failed!");
            return;
        }

        List<CloudMedia.Node> allList = nodesList.getAllOnlineList();
        if (allList == null || allList.size() == 0) {
            List<CloudMedia.Node> newList = nodesList.getNewOnlineList();
            if (newList != null && newList.size() > 0) {
                Log.d(TAG, "updateNodeList newList size:"+newList.size());
                addInfoList(newList);
            }
            List<CloudMedia.Node> deleList = nodesList.getNewOfflineList();
            if (deleList != null && deleList.size() > 0) {
                Log.d(TAG, "updateNodeList deleList size:"+deleList.size());
                deleteList(deleList);
            }
            List<CloudMedia.Node> upList = nodesList.getNewUpdateList();
            if (upList != null && upList.size() > 0) {
                Log.d(TAG, "updateNodeList updateList size:"+upList.size());
                updateList(upList);
            }
        }else {
            Log.d(TAG, "updateNodeList allList size:"+allList.size());
            mNodes.clear();
            addInfoList(allList);
        }
        mLiveListAdapter.notifyDataSetChanged();
    }

    private void addInfoList(List<CloudMedia.Node> newList) {
        mNodes.addAll(newList);
    }

    private void deleteList(List<CloudMedia.Node> deleList) {
        for(int i = 0; i < deleList.size(); i++) {
            Log.d(TAG, "deleteList:CnodeId = "+deleList.get(i).getID());
            Log.d(TAG, "deleteList:CnodeNick = "+deleList.get(i).getNick());
            for (int j = 0; j < mNodes.size(); j++) {
                if (mNodes.get(j).getID().equals(deleList.get(i).getID())) {
                    mNodes.remove(j);
                    Log.d(TAG, "remove:NodeId = "
                            +deleList.get(i).getID()+", NickName = "+deleList.get(i).getNick());
                    break;
                }
            }

        }
    }

    private void updateList(List<CloudMedia.Node> upList) {
        for(int i = 0; i < upList.size(); i++) {
            for (int j = 0; j < mNodes.size(); j++) {
                if (mNodes.get(j).getID().equals(upList.get(i).getID())) {
                    Log.d(TAG, "updateList:nodeId = "+mNodes.get(j).getID());
                    Log.d(TAG, "updateList:nodeNick = "+mNodes.get(j).getNick());
                    mNodes.remove(j);
                    mNodes.add(j, upList.get(i));
                    break;
                }
            }
        }
    }


    private void startPlay(int index, SourceType type) {
        Log.d(TAG, "startPlay:index = "+index+", type = "+type);

        CloudMedia.Node cNode = mNodes.get(index);
        final RemoteMediaNode node = mCloudMedia.declareRemoteMediaNode(cNode);
        mToPlayNodeId = cNode.getID();

        Log.d(TAG, "sendMessage: nodeId = "+cNode.getID());
        mCloudMedia.sendMessage(cNode.getGroupID(), cNode.getID(), "puller0: start push!");

        node.startPushMedia(new CloudMedia.RPCResultListener() {
            @Override
            public void onSuccess(String s) {
                Log.d(TAG, "startPushMedia onSuccess:"+s);
                node.setStreamExceptionListener(new RemoteMediaNode.OnStreamException() {
                    @Override
                    public void onStreamException(String s, RemoteMediaNode.StreamException e) {
                        Log.d(TAG, "onStreamException: "+e.str());
                    }
                });
                if (mIsWating) {
                    mIsWating = false;
                    mPbar.setVisibility(View.GONE);
                    try {
                        JSONObject jsonObj = new JSONObject(s);
                        if(jsonObj.has("url")) {
                            mUrl = jsonObj.getString("url");
                            Log.d(TAG, "url = "+mUrl);
                            startPlayer();
                            mCloudMedia.updateStreamStatus(CloudMedia.CMStreamStatus.PULLING, new CloudMedia.RPCResultListener() {
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
                    mPbar.setVisibility(View.GONE);
                }
                Toast.makeText(VideoListActivity.this,
                        getText(R.string.video_error_start_push), Toast.LENGTH_LONG).show();
            }
        });
        mIsWating = true;
        mPbar.setVisibility(View.VISIBLE);

    }

    private void startPlayer() {
        Intent intent = new Intent(VideoListActivity.this, VideoPlayerActivity.class);
        intent.putExtra("nodeId", mToPlayNodeId);
        intent.putExtra("url", mUrl);
        startActivityForResult(intent,STARTPLAYER_REQUEST_CODE);
    }

    private void setMessageListener() {
        if (mCloudMedia != null) {
            mCloudMedia.setMessageListener(new CloudMedia.OnMessageListener() {
                @Override
                public void onMessage(String s, String s1, String s2) {
                    Log.d(TAG, "onMessage:s = "+s+", s1 = "+s1+", s2 ="+s2);
                }
            });
        }
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
                    mCloudMedia.declareRemoteMediaNode(mNodes.get(i)).
                            stopPushMedia(new CloudMedia.RPCResultListener() {
                                @Override
                                public void onSuccess(String s) {
                                    Log.d(TAG, "stopPushMedia onSuccess s = "+s);
                                    mCloudMedia.updateStreamStatus(CloudMedia.CMStreamStatus.PULLING_CLOSE, new CloudMedia.RPCResultListener() {
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

    class MediaInfo {
        RemoteMediaNode mRemoteNode;
        String mNodeId;
        String mNodeNickName;
        String mNodeStatus;
        String mGroupId;
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
                    startPlay(clickHolder.index, SourceType.RTMP);
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
        if (mIsWating && mPbar.getVisibility() == View.VISIBLE) {
            mIsWating = false;
            mPbar.setVisibility(View.GONE);
            stopPushMedia(mToPlayNodeId);
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if(mCloudMedia != null) {
            Log.d(TAG, "disconnect");
            mCloudMedia.disconnect();
        }
    }
}
