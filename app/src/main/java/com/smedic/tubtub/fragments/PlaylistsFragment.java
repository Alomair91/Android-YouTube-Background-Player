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

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.services.youtube.YouTubeScopes;
import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.util.Swappable;
import com.smedic.tubtub.BackgroundAudioService;
import com.smedic.tubtub.R;
import com.smedic.tubtub.YouTubePlaylist;
import com.smedic.tubtub.YouTubeSearch;
import com.smedic.tubtub.YouTubeVideo;
import com.smedic.tubtub.interfaces.YouTubePlaylistsReceiver;
import com.smedic.tubtub.interfaces.YouTubeVideosReceiver;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.SnappyDb;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Class that handles list of the playlists acquired from YouTube
 * Created by smedic on 7.3.16..
 */
public class PlaylistsFragment extends Fragment implements YouTubeVideosReceiver, YouTubePlaylistsReceiver {

    private static final String TAG = "SMEDIC PlaylistsFrag";

    private ArrayList<YouTubePlaylist> playlists;
    private DynamicListView playlistsListView;
    private Handler handler;
    private PlaylistAdapter playlistsAdapter;

    public static final String ACCOUNT_KEY = "accountName";
    private String mChosenAccountName;

    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;

    private YouTubeSearch youTubeSearch;
    private ImageButton searchPlaylistsButton;

    private ProgressBar loadingProgressBar;

    public PlaylistsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        youTubeSearch = new YouTubeSearch(getActivity(), this);
        youTubeSearch.setYouTubePlaylistsReceiver(this);
        youTubeSearch.setYouTubeVideosReceiver(this);

        if (savedInstanceState != null) {
            mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY);
            youTubeSearch.setAuthSelectedAccountName(mChosenAccountName);
        } else {
            loadAccount();
        }
    }

    /**
     * Loads account saved in preferences
     */
    private void loadAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        mChosenAccountName = sp.getString(ACCOUNT_KEY, null);
        youTubeSearch.setAuthSelectedAccountName(mChosenAccountName);
    }

    /**
     * Save account in preferences for future usages
     */
    private void saveAccount() {
        Log.d(TAG, "Saving account name... " + mChosenAccountName);
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        sp.edit().putString(ACCOUNT_KEY, mChosenAccountName).commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playlists, container, false);

        /* Setup the ListView */
        playlistsListView = (DynamicListView) v.findViewById(R.id.playlists);

        searchPlaylistsButton = (ImageButton) v.findViewById(R.id.loadButton);

        //loadingProgressBar = (ProgressBar) v.findViewById(R.id.loadingBar);
        //loadingProgressBar.setVisibility(View.INVISIBLE);

        playlists = new ArrayList<>();
        setupListViewAndAdapter();

        handler = new Handler();

        searchPlaylistsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mChosenAccountName == null) {
                    chooseAccount();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String token = GoogleAuthUtil.getToken(getActivity(), mChosenAccountName,
                                        "oauth2:" + YouTubeScopes.YOUTUBE);
                                Log.d(TAG, "TOKEN " + token);
                            } catch (IOException io) {

                            } catch (UserRecoverableAuthException erae) {

                            } catch (GoogleAuthException gae) {

                            }
                        }
                    }).start();
                    //loadingProgressBar.setVisibility(View.VISIBLE);
                    youTubeSearch.searchPlaylists();
                }
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getUserVisibleHint()) {
            //do nothing for now
        }

        playlists.clear();
        playlists.addAll(SnappyDb.getInstance().getAllPlaylistItems());
        playlistsAdapter.notifyDataSetChanged();
    }


    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);

        if (visible && isResumed()) {
            //Log.d(TAG, "PlaylistsFragment visible and resumed");
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

    /**
     * Handles Google OAuth 2.0 authorization or account chosen result
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_AUTHORIZATION:
                if (resultCode != Activity.RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null
                        && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mChosenAccountName = accountName;
                        youTubeSearch.setAuthSelectedAccountName(accountName);
                        saveAccount();
                    }

                    youTubeSearch.searchPlaylists();
                }
                break;
        }
    }

    /**
     * Choose Google account if OAuth 2.0 choosing is necessary
     * acquiring YouTube private playlists requires OAuth 2.0 authorization
     */
    private void chooseAccount() {
        startActivityForResult(youTubeSearch.getCredential().newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
    }

    /**
     * Setups list view and adapter for storing YouTube playlists
     */
    public void setupListViewAndAdapter() {
        playlistsAdapter = new PlaylistAdapter(getActivity());
        SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(playlistsAdapter);
        animationAdapter.setAbsListView(playlistsListView);
        playlistsListView.setAdapter(animationAdapter);

        /* Enable drag and drop functionality */
        playlistsListView.enableDragAndDrop();
        playlistsListView.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                                   final int position, final long id) {
                        playlistsListView.startDragging(position);
                        return true;
                    }
                }
        );

        playlistsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos,
                                    long id) {
                Toast.makeText(getContext(), "Playing playlist: " + playlists.get(pos).getTitle(), Toast.LENGTH_SHORT).show();
                youTubeSearch.acquirePlaylistVideos(playlists.get(pos).getId()); //results are in onVideosReceived callback method
            }
        });

    }


    /**
     * Custom array adapter class which enables drag and drop list items swapping
     */
    public class PlaylistAdapter extends ArrayAdapter<YouTubePlaylist> implements Swappable {

        public PlaylistAdapter(Activity context) {
            super(context, R.layout.video_item, playlists);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.playlist_item, parent, false);
            }
            ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
            TextView title = (TextView) convertView.findViewById(R.id.playlist_title);
            TextView videosNumber = (TextView) convertView.findViewById(R.id.videos_number);
            TextView privacy = (TextView) convertView.findViewById(R.id.privacy);

            YouTubePlaylist searchResult = playlists.get(position);

            Picasso.with(getContext()).load(searchResult.getThumbnailURL()).into(thumbnail);
            title.setText(searchResult.getTitle());
            videosNumber.setText("Number of videos: " + String.valueOf(searchResult.getNumberOfVideos()));
            privacy.setText("Status: " + searchResult.getStatus());

            return convertView;
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).hashCode();
        }


        @Override
        public boolean hasStableIds() {
            return true;
        }


        @Override
        public void swapItems(int i, int i1) {
            YouTubePlaylist firstItem = getItem(i);

            playlists.set(i, getItem(i1));
            playlists.set(i1, firstItem);

            notifyDataSetChanged();
        }
    }

    /**
     * Called when playlist video items are received
     *
     * @param youTubeVideos - list to be played in background service
     */
    @Override
    public void onVideosReceived(ArrayList<YouTubeVideo> youTubeVideos) {
        Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        serviceIntent.putExtra(Config.YOUTUBE_MEDIA_TYPE, Config.YOUTUBE_PLAYLIST);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, youTubeVideos);
        getActivity().startService(serviceIntent);
    }

    /**
     * Called when playlists are received
     *
     * @param youTubePlaylists - list of playlists to be shown in list view
     */
    @Override
    public void onPlaylistsReceived(ArrayList<YouTubePlaylist> youTubePlaylists) {
        playlists.clear();
        playlists.addAll(youTubePlaylists);
        handler.post(new Runnable() {
            public void run() {
                if (playlistsAdapter != null) {
                    playlistsAdapter.notifyDataSetChanged();
                }
            }
        });
    }
}