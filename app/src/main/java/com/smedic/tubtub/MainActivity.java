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
package com.smedic.tubtub;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.fragments.FavoritesFragment;
import com.smedic.tubtub.fragments.PlaylistsFragment;
import com.smedic.tubtub.fragments.RecentlyWatchedFragment;
import com.smedic.tubtub.fragments.SearchFragment;
import com.smedic.tubtub.utils.NetworkConf;
import com.smedic.tubtub.youtube.JsonAsyncTask;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "SMEDIC MAIN ACTIVITY";
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private final int PERMISSIONS = 1;

    private int initialColor = 0xffff0040;
    private int initialColors[] = new int[2];

    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;

    private int[] tabIcons = {
            R.drawable.ic_action_heart,
            R.drawable.ic_recently_wached,
            R.drawable.ic_search,
            R.drawable.ic_action_playlist
    };

    private NetworkConf networkConf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        YouTubeSqlDb.getInstance().init(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        networkConf = new NetworkConf(this);

        setupTabIcons();

        loadColor();

        requestPermissions();
        /*if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }*/
    }

    @AfterPermissionGranted(PERMISSIONS)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_PHONE_STATE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            Log.d(TAG, "requestPermissions: all permissions are there");
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.all_permissions_request),
                    PERMISSIONS, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Override super.onNewIntent() so that calls to getIntent() will return the
     * latest intent that was used to start this Activity rather than the first
     * intent.
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        handleIntent(intent);
    }

    /**
     * Handle search intent and queries YouTube for videos
     *
     * @param intent
     */
    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            viewPager.setCurrentItem(2, true); //switch to search fragment

            if (searchFragment != null) {
                searchFragment.searchQuery(query);
            }
        }
    }

    /**
     * Setups icons for 3 tabs
     */
    private void setupTabIcons() {
        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
        tabLayout.getTabAt(3).setIcon(tabIcons[3]);
    }

    /**
     * Setups viewPager for switching between pages according to the selected tab
     *
     * @param viewPager
     */
    private void setupViewPager(ViewPager viewPager) {

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        searchFragment = new SearchFragment();
        recentlyPlayedFragment = new RecentlyWatchedFragment();
        adapter.addFragment(new FavoritesFragment(), null);
        adapter.addFragment(recentlyPlayedFragment, null);
        adapter.addFragment(searchFragment, null);
        adapter.addFragment(new PlaylistsFragment(), null);
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:");
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied: ");
    }

    /**
     * Class which provides adapter for fragment pager
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }

    /**
     * Options menu in action bar
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        //suggestions
        final CursorAdapter suggestionAdapter = new SimpleCursorAdapter(this,
                R.layout.suggestions,
                null,
                new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},
                new int[]{android.R.id.text1},
                0);
        final List<String> suggestions = new ArrayList<>();

        searchView.setSuggestionsAdapter(suggestionAdapter);

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                searchView.setQuery(suggestions.get(position), false);
                searchView.clearFocus();

                Intent suggestionIntent = new Intent(Intent.ACTION_SEARCH);
                suggestionIntent.putExtra(SearchManager.QUERY, suggestions.get(position));
                handleIntent(suggestionIntent);

                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false; //if true, no new intent is started
            }

            @Override
            public boolean onQueryTextChange(String suggestion) {
                // check network connection. If not available, do not query.
                // this also disables onSuggestionClick triggering
                if (suggestion.length() > 2) { //make suggestions after 3rd letter

                    if (networkConf.isNetworkAvailable()) {

                        new JsonAsyncTask(new JsonAsyncTask.OnSuggestionsLoadedListener() {
                            @Override
                            public void OnSuggestionsLoaded(ArrayList<String> result) {
                                suggestions.clear();
                                suggestions.addAll(result);
                                String[] columns = {
                                        BaseColumns._ID,
                                        SearchManager.SUGGEST_COLUMN_TEXT_1
                                };
                                MatrixCursor cursor = new MatrixCursor(columns);

                                for (int i = 0; i < result.size(); i++) {
                                    String[] tmp = {Integer.toString(i), result.get(i)};
                                    cursor.addRow(tmp);
                                }
                                suggestionAdapter.swapCursor(cursor);

                            }
                        }).execute(suggestion);
                        return true;
                    }
                }
                return false;
            }
        });

        return true;
    }

    /**
     * Handles selected item from action bar
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle(getString(R.string.myName));
            alertDialog.setIcon(R.mipmap.ic_launcher);

            alertDialog.setMessage(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME + "\n\n" +
                    getString(R.string.email) + "\n\n" +
                    getString(R.string.date) + "\n");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();

            return true;
        } else if (id == R.id.action_clear_list) {
            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).deleteAll();
            recentlyPlayedFragment.clearRecentlyPlayedList();
            return true;
        } else if (id == R.id.action_search) {
            MenuItemCompat.expandActionView(item);
            return true;
        } else if (id == R.id.action_color_picker) {
            /* Show color picker dialog */
            ColorPickerDialogBuilder
                    .with(this)
                    .setTitle("Choose background and text color")
                    .initialColor(initialColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .setPickerCount(2)
                    .initialColors(initialColors)
                    .density(12)
                    .setOnColorSelectedListener(new OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(int selectedColor) {
                        }
                    })
                    .setPositiveButton("ok", new ColorPickerClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                            //changeBackgroundColor(selectedColor);
                            if (allColors != null) {
                                setColors(allColors[0], allColors[1]);
                            }
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .showColorEdit(true)
                    .build()
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads app theme color saved in preferences
     */
    private void loadColor() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        int backgroundColor = sp.getInt("BACKGROUND_COLOR", -1);
        int textColor = sp.getInt("TEXT_COLOR", -1);

        if (backgroundColor != -1 && textColor != -1) {
            setColors(backgroundColor, textColor);
        } else {
            initialColors = new int[]{
                    ContextCompat.getColor(this, R.color.colorPrimary),
                    ContextCompat.getColor(this, R.color.textColorPrimary)};
        }
    }

    /**
     * Save app theme color in preferences
     */
    private void setColors(int backgroundColor, int textColor) {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(backgroundColor);
        toolbar.setTitleTextColor(textColor);
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setBackgroundColor(backgroundColor);
        tabs.setTabTextColors(textColor, textColor);
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putInt("BACKGROUND_COLOR", backgroundColor).commit();
        sp.edit().putInt("TEXT_COLOR", textColor).commit();

        initialColors[0] = backgroundColor;
        initialColors[1] = textColor;
    }
}
