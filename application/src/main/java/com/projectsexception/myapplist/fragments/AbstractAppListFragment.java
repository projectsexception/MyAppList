package com.projectsexception.myapplist.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.projectsexception.myapplist.MyAppListPreferenceActivity;
import com.projectsexception.myapplist.R;
import com.projectsexception.myapplist.model.AppInfo;
import com.projectsexception.myapplist.util.AppUtil;
import com.projectsexception.myapplist.util.ApplicationsReceiver;
import com.projectsexception.myapplist.view.AppListAdapter;

import java.util.ArrayList;

import butterknife.InjectView;
import butterknife.Views;

public abstract class AbstractAppListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<ArrayList<AppInfo>>,
        AdapterView.OnItemClickListener,
        AppListAdapter.ActionListener {

    private static final String KEY_LISTENER = "AbstractAppListFragment";

    protected static final String ARG_RELOAD = "reload";
    
    protected MenuItem mRefreshItem;
    protected AppListAdapter mAdapter;
    private boolean mListShown;
    private boolean mAnimations;
    @InjectView(android.R.id.list)
    ListView mListView;
    @InjectView(android.R.id.empty) View mEmptyView;
    @InjectView(android.R.id.progress) View mProgress;

    abstract int getMenuAdapter();
    abstract void showAppInfo(String name, String packageName);
    abstract Loader<ArrayList<AppInfo>> createLoader(int id, Bundle args);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        Views.inject(this, view);
        return view;
    }
    
    @Override 
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mAnimations = prefs.getBoolean(MyAppListPreferenceActivity.KEY_ANIMATIONS, true);
        mAdapter = new AppListAdapter(getActivity(), savedInstanceState, getMenuAdapter(), mAnimations);
        mAdapter.setOnItemClickListener(this);
        mAdapter.setAdapterView(mListView);
        mAdapter.setListener(this);

        mListView.setFastScrollEnabled(true);
        mListView.setEmptyView(mEmptyView);

        // Start out with a progress indicator.
        setListShown(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            mAdapter.save(outState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ApplicationsReceiver.getInstance(getActivity()).isContextChanged(KEY_LISTENER)) {
            Bundle args = new Bundle();
            args.putBoolean(ARG_RELOAD, true);
            getLoaderManager().restartLoader(0, args, this);
        }
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean animations = prefs.getBoolean(MyAppListPreferenceActivity.KEY_ANIMATIONS, true);
        if (mAnimations != animations) {
            mAnimations = animations;
            mAdapter.setAnimations(animations);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppInfo appInfo = mAdapter.getData().get(position);
        if (!TextUtils.isEmpty(appInfo.getPackageName())) {
            if (appInfo.isInstalled()) {
                showAppInfo(appInfo.getName(), appInfo.getPackageName());
            } else {
                AppUtil.showPlayGoogleApp(getActivity(), appInfo.getPackageName(), false);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        ApplicationsReceiver.unregisterListener(getActivity());
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_select_all) {
            for (int i = 0; i < mAdapter.getCount(); ++i) {
                mAdapter.setItemChecked(i, true);
            }
            return true;
        } else if (item.getItemId() == R.id.menu_refresh) {
            Bundle args = new Bundle();
            args.putBoolean(ARG_RELOAD, true);
            getLoaderManager().restartLoader(0, args, this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override 
    public Loader<ArrayList<AppInfo>> onCreateLoader(int id, Bundle args) {
        loading(true);
        return createLoader(id, args);
    }

    @Override 
    public void onLoadFinished(Loader<ArrayList<AppInfo>> loader, ArrayList<AppInfo> data) {
        loading(false);
        
        ApplicationsReceiver.getInstance(getActivity()).registerListener(KEY_LISTENER);
        
        // Set the new data in the adapter.
        mAdapter.setData(data);

        // The list should now be shown.
        setListShown(true);
    }

    @Override 
    public void onLoaderReset(Loader<ArrayList<AppInfo>> loader) {
        loading(false);
        // Clear the data in the adapter.
        mAdapter.setData(null);
    }

    public void setListShown(boolean shown) {
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            mProgress.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        } else {
            mProgress.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.INVISIBLE);
        }
    }
    
    protected void loading(boolean loading) {
        if (mEmptyView != null) {
            if (loading) {
                mEmptyView.setVisibility(View.INVISIBLE);
            } else {
                mEmptyView.setVisibility(View.VISIBLE);
            }
        }

        if (mRefreshItem != null) {
            if(loading) {
                mRefreshItem.setEnabled(false);
                mRefreshItem.setActionView(R.layout.refresh_loading);
            } else {
                mRefreshItem.setEnabled(true);
                mRefreshItem.setActionView(null);
            }           
        }
    }
}