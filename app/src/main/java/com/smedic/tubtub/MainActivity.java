package com.smedic.tubtub;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.smedic.tubtub.fragments.PlaylistsFragment;
import com.smedic.tubtub.fragments.RecentlyWatchedFragment;
import com.smedic.tubtub.fragments.SearchFragment;
import com.smedic.tubtub.utils.SnappyDb;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SMEDIC MAIN ACTIVITY";
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private RequestQueue volleyQueue;

    private SearchFragment searchFragment;

    private int[] tabIcons = {
            android.R.drawable.ic_menu_recent_history,
            android.R.drawable.ic_menu_search,
            android.R.drawable.ic_menu_upload_you_tube
    };

    private static final String DATABASE_NAME = "youtube_database";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //SnappyDB - NoSql
        SnappyDb.getInstance().init(this, DATABASE_NAME);

        volleyQueue = Volley.newRequestQueue(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        setupTabIcons();
    }

    /**
     * Override super.onNewIntent() so that calls to getIntent() will return the
     * latest intent that was used to start this Activity rather than the first
     * intent.
     */
    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "ON NEW INTENT!!!");
        super.onNewIntent(intent);
        setIntent(intent);

        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "QUERY: " + query);
            if(searchFragment != null) {
                searchFragment.searchQuery(query);
            }
        }
    }


    private void setupTabIcons() {
        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
    }


    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new RecentlyWatchedFragment(), "ONE");

        searchFragment = new SearchFragment();

        adapter.addFragment(searchFragment, "TWO");
        adapter.addFragment(new PlaylistsFragment(), "THREE");
        viewPager.setAdapter(adapter);
    }

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
            return null; /*mFragmentTitleList.get(position);*/
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            Log.d(TAG, "search view not null");
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        } else {
            Log.e(TAG, "search view null");
        }

        //suggestions
        final CursorAdapter suggestionAdapter = new SimpleCursorAdapter(this,
                R.layout.dropdown_menu,
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
                Log.d(TAG, "TEXT SUBMIT");
                viewPager.setCurrentItem(1);
                return false; //if true, no new intent is started

            }

            @Override
            public boolean onQueryTextChange(String suggestion) {

                if (suggestion.length() > 2) {

                    JsonAsyncTask asyncTask = new JsonAsyncTask(new JsonAsyncTask.AsyncResponse() {
                        @Override
                        public void processFinish(ArrayList<String> result) {
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
                    });
                    asyncTask.execute(suggestion);

                    return true;
                } else {
                    return false;
                }
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_search) {
            MenuItemCompat.expandActionView(item);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
