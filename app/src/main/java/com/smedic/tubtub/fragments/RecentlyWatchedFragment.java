/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smedic.tubtub.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.SimpleSwipeUndoAdapter;
import com.smedic.tubtub.BackgroundAudioService;
import com.smedic.tubtub.R;
import com.smedic.tubtub.VideosAdapter;
import com.smedic.tubtub.YouTubeVideo;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.NetworkConf;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Class that handles list of the recently watched YouTube
 * Created by smedic on 7.3.16..
 */
public class RecentlyWatchedFragment extends Fragment {

    private static final String TAG = "SMEDIC RecentlyWatched";
    private ArrayList<YouTubeVideo> recentlyPlayedVideos;

    private DynamicListView recentlyPlayedListView;
    private VideosAdapter videoListAdapter;

    private NetworkConf conf;

    public RecentlyWatchedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        recentlyPlayedVideos = new ArrayList<>();
        conf = new NetworkConf(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_recently_watched, container, false);

        recentlyPlayedListView = (DynamicListView) v.findViewById(R.id.recently_played);
        setupListViewAndAdapter();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getUserVisibleHint()) {
            //do nothing for now
        }

        Log.d(TAG, "onResume - RecentlyWatchedFragment");
        recentlyPlayedVideos.clear();
        recentlyPlayedVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).readAll());
        videoListAdapter.notifyDataSetChanged();
    }


    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);

        if (visible && isResumed()) {
            //Log.d(TAG, "RecentlyWatchedFragment visible and resumed");
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    /**
     * Setups list view and adapter for storing recently watched YouTube videos
     */
    private void setupListViewAndAdapter() {

        /* Setup the adapter */
        videoListAdapter = new VideosAdapter(getActivity(), recentlyPlayedVideos, false);
        SimpleSwipeUndoAdapter simpleSwipeUndoAdapter = new SimpleSwipeUndoAdapter(videoListAdapter, getContext(), new MyOnDismissCallback());
        SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(simpleSwipeUndoAdapter);
        animationAdapter.setAbsListView(recentlyPlayedListView);
        recentlyPlayedListView.setAdapter(animationAdapter);

        /* Enable drag and drop functionality */
        recentlyPlayedListView.enableDragAndDrop();
        //recentlyPlayedListView.setDraggableManager(new TouchViewDraggableManager(R.id.row_item));
        recentlyPlayedListView.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                                   final int position, final long id) {
                        recentlyPlayedListView.startDragging(position);
                        return true;
                    }
                }
        );

        /* Enable swipe to dismiss with Undo */
        recentlyPlayedListView.enableSimpleSwipeUndo();

        addListeners();
    }

    /**
     * Adds listener for list item choosing
     */
    void addListeners() {
        recentlyPlayedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, final int pos,
                                    long id) {
                if (conf.isNetworkAvailable()) {

                    Toast.makeText(getContext(), "Playing: " + recentlyPlayedVideos.get(pos).getTitle(), Toast.LENGTH_SHORT).show();

                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).create(recentlyPlayedVideos.get(pos));

                    Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
                    serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
                    serviceIntent.putExtra(Config.YOUTUBE_MEDIA_TYPE, Config.YOUTUBE_VIDEO);
                    serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, recentlyPlayedVideos.get(pos));
                    getActivity().startService(serviceIntent);
                } else {
                    conf.createNetErrorDialog();
                }
            }
        });
    }

    /**
     * Callback which handles onDismiss event of a list item
     */
    private class MyOnDismissCallback implements OnDismissCallback {

        @Nullable
        private Toast mToast;

        @Override
        public void onDismiss(@NonNull final ViewGroup listView, @NonNull final int[] reverseSortedPositions) {
            for (int position : reverseSortedPositions) {
                YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).
                        delete(recentlyPlayedVideos.get(position).getId());
                recentlyPlayedVideos.remove(position);
                videoListAdapter.notifyDataSetChanged();
            }

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(
                    getActivity(),
                    getString(R.string.removed_positions, Arrays.toString(reverseSortedPositions)),
                    Toast.LENGTH_LONG
            );
            mToast.show();
        }
    }

    /**
     * Clears recently played list items
     */
    public void clearRecentlyPlayedList() {
        recentlyPlayedVideos.clear();
        videoListAdapter.notifyDataSetChanged();
    }

}
