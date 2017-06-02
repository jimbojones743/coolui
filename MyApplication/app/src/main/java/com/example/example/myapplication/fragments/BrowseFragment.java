package com.example.example.myapplication.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.example.myapplication.R;
import com.example.example.myapplication.adapters.RecyclerViewGridAdapter;
import com.example.example.myapplication.db.ImageDatabase;
import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.ImageTile;
import com.example.example.myapplication.utils.Utils;
import com.google.common.collect.ArrayListMultimap;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

/**
 * Fragment for the browse activity
 */
public class BrowseFragment extends Fragment implements View.OnClickListener {

    @BindView(R.id.browse_image_grid) RecyclerView _recyclerGrid;
    @BindView(R.id.browse_load_more) Button _loadButton;
    @BindView(R.id.browse_clear_all) Button _clearButton;
    @BindView(R.id.browse_query_input) EditText _queryText;
    @BindView(R.id.browse_query_button) Button _queryButton;

    private RecyclerViewGridAdapter adapter;
    private Activity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_browse, container, false);
        ButterKnife.bind(this, view);
        _loadButton.setOnClickListener(this);
        _clearButton.setOnClickListener(this);
        _queryButton.setOnClickListener(this);
        populateGrid();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mActivity = (Activity) context;
    }


    @Override
    public void onResume() {
        super.onResume();
        populateGrid();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.browse_load_more:
                queryTimestamp();
                break;
            case R.id.browse_clear_all:
                deleteAll();
                break;
            case R.id.browse_query_button:
                query();
                break;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i("BROWSE USER VISIBLE", isVisibleToUser + "");
        if (isVisibleToUser) {
            //refreshFragment();
            //populateGrid();
            refreshAdapter();
        }
    }


    private void query() {
        String queryFull = _queryText.getText().toString().toLowerCase().trim();
        new Query().execute(queryFull);
    }

    private void refreshAdapter() {
        this.adapter.clear();
        File dir = Utils.getCurrentUserThumbnailDir(mActivity);
        List<ImageTile> tiles = new ArrayList<>();
        ImageDatabase idb = new ImageDatabase(mActivity);
        for (File image : dir.listFiles()) {
            if (image.getAbsolutePath().endsWith(".jpg")) {
                String imageName = image.getName().replace(".jpg", "");
                System.out.println(idb.getTimestamp(imageName));
                tiles.add(new ImageTile(idb.getTimestamp(imageName), imageName, image));
            }
        }
        idb.close();
        Collections.sort(tiles, new Comparator<ImageTile>() {
            @Override
            public int compare(ImageTile o1, ImageTile o2) {
                return new Long(o1.timestamp - o2.timestamp).intValue();
            }
        });
        this.adapter.addAll(tiles);
    }

    private void populateGrid() {
        File dir = Utils.getCurrentUserThumbnailDir(mActivity);
        List<ImageTile> tiles = new ArrayList<>();
        ImageDatabase idb = new ImageDatabase(mActivity);
        for (File image : dir.listFiles()) {
            if (image.getAbsolutePath().endsWith(".jpg")) {
                String imageName = image.getName().replace(".jpg", "");
                System.out.println(idb.getTimestamp(imageName));
                tiles.add(new ImageTile(idb.getTimestamp(imageName), imageName, image));
            }
        }
        idb.close();
        this._recyclerGrid.setLayoutManager(new GridLayoutManager(mActivity, 3));

        // sort the data before putting it in the adapter
        Collections.sort(tiles, new Comparator<ImageTile>() {
            @Override
            public int compare(ImageTile o1, ImageTile o2) {
                return new Long(o1.timestamp - o2.timestamp).intValue();
            }
        });

        this.adapter = new RecyclerViewGridAdapter(mActivity, tiles, true);
        this.adapter.setClickListener(new ImageClickListener(mActivity));
        _recyclerGrid.setAdapter(adapter);
    }

    /**
     * Fetches from the server images in reverse order in which they were uploaded.
     */
    public void queryTimestamp() {
        final ProgressDialog progressDialog = new ProgressDialog(mActivity);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Fetching more images...");
        progressDialog.show();
        DynRH2LevClientWrapper.queryTimestamp(mActivity, 2, new FileAsyncHttpResponseHandler(mActivity) {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                // possibly no more photos to send
                Log.i("QUERY TIMELINE", "FAILURE");
                progressDialog.dismiss();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                // add the photos to the list adapter
                // set the last image we retrieved here. Need someway to know which image has the earliest timestamp
                Log.i("QUERY TIMELINE", "SUCCESS");
                try {
                    if (!file.getName().equals("null")) {
                        Utils.unpackThumbnailsZip(mActivity, file);
                    }
                    File dir = Utils.getCurrentUserThumbnailDir(mActivity);
                    List<ImageTile> tiles = new ArrayList<>();
                    ImageDatabase idb = new ImageDatabase(mActivity);
                    for (File image : dir.listFiles()) {
                        if (image.getAbsolutePath().endsWith(".jpg")) {
                            String imageName = image.getName().replace(".jpg", "");
                            tiles.add(new ImageTile(idb.getTimestamp(imageName), imageName, image));
                        }
                    }
                    idb.close();
                    //TODO : find a cleaner way to do this without having to clear the adapter
                    adapter.clear();

                    // sort the data before putting it in the adapter
                    Collections.sort(tiles, new Comparator<ImageTile>() {
                        @Override
                        public int compare(ImageTile o1, ImageTile o2) {
                            return new Long(o1.timestamp - o2.timestamp).intValue();
                        }
                    });
                    adapter.addAll(tiles);
                    progressDialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Clears all the saved images we have queried, and clears the cookie from our cookie store
     */
    public void deleteAll() {
        DynRH2LevClientWrapper.clearImageCookie(mActivity, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("CLEAR COOKIE", "SUCCESS");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.i("CLEAR COOKIE", "FAIL");
            }
        });
        File dir = Utils.getCurrentUserThumbnailDir(mActivity);
        // if the file is empty, do nothing
        if (dir.list().length != 0) {
            // delete all the images from our file
            for (File image : dir.listFiles()) {
                if (image.getAbsolutePath().endsWith(".jpg")) {
                    image.delete();
                }
            }
        }

        File imageDir = Utils.getCurrentUserImageDir(mActivity);
        if (imageDir.list().length != 0) {
            for (File image : imageDir.listFiles()) {
                if (image.getAbsolutePath().endsWith(".jpg")) {
                    image.delete();
                }
            }
        }

        File mediumDir = Utils.getCurrentUserMediumDir(mActivity);
        if (mediumDir.list().length != 0) {
            for (File image : mediumDir.listFiles()) {
                if (image.getAbsolutePath().endsWith(".jpg")) {
                    image.delete();
                }
            }
        }

        // clear the database
        ImageDatabase idb = new ImageDatabase(mActivity);
        idb.clearAll();
        idb.close();

        // clear the local multimap index
        try {
            Utils.saveCurrentUserTagIndex(mActivity, ArrayListMultimap.<String, String>create());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // clear the adapter
        this.adapter.clear();
    }

    private class Query extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            // this is the lowercased and trimmed query
            final String queryFull = params[0];
            // if our query contains multiple keywords, run query multiple times before bringing user to
            // the view query activity
            final String[] keywords = queryFull.split(" ");
            final CountDownLatch countDownLatch = new CountDownLatch(keywords.length);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    for (String keyword : keywords) {
                        try {
                            DynRH2LevClientWrapper.query(mActivity, keyword, new FileAsyncHttpResponseHandler(mActivity) {
                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                                    Log.i("QUERY", "failed");
                                    countDownLatch.countDown();
                                }

                                @Override
                                public void onSuccess(int statusCode, Header[] headers, File file) {
                                    Log.i("QUERY", "" + "success");
                                    Log.i("QUERY", file.getName());
                                    if (statusCode == 227) {
                                        // this is a local query
                                        Log.i("QUERY", "LOCAL SUCCESS");
                                    } else {
                                        Log.i("QUERY", "CLOUD SUCCESS");
                                        try {
                                            Utils.unpackThumbnailsZip(mActivity, file);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    Log.i("COUNTDOWN", countDownLatch.getCount() + "");
                                    countDownLatch.countDown();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            try {
                countDownLatch.await(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return queryFull;
        }

        @Override
        protected void onPostExecute(String queryFull) {
            Bundle b = new Bundle();
            b.putString(Const.QUERY_RESULT_LABEL, queryFull);
            Intent queryView = new Intent(mActivity, ViewQueryActivity.class);
            queryView.putExtras(b);
            startActivity(queryView);
        }
    }

    private class ImageClickListener implements RecyclerViewGridAdapter.ItemClickListener {

        private Activity activity;

        public ImageClickListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onItemClick(View view, int position) {
            // we get the tile that was clicked
            final ImageTile tile = adapter.getItem(position);
            // if we already have the full image, don't bother requesting it from the server again
            Intent intent = new Intent(mActivity, FullImageActivity.class);
            intent.putExtra(Const.PICTURE_NAME_LABEL, tile.name);
            startActivity(intent);
        }
    }

}
