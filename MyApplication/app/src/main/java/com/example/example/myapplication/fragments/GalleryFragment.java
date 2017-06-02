package com.example.example.myapplication.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.example.myapplication.R;
import com.example.example.myapplication.adapters.RecyclerViewGridAdapter;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.ImageTile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A fragment that allows a user to select images from the gallery. Leads to the batch image tagging
 * activity
 */
public class GalleryFragment extends Fragment implements View.OnClickListener {

    @BindView(R.id.gallery_grid) RecyclerView _recyclerGrid;
    @BindView(R.id.gallery_select) Button _selectButton;

    private Activity mActivity;
    private RecyclerViewGridAdapter adapter;

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.custom_gallery, container, false);
        ButterKnife.bind(this, view);
        if (checkExternalStoragePermission()) {
            setupView();
        }
        return view;
    }

    private void setupView() {
        final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};
        final String orderBy = MediaStore.Images.Media._ID;
        Cursor imagecursor = mActivity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
        int count = imagecursor.getCount();
        List<ImageTile> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            imagecursor.moveToPosition(i);
            int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);
            tiles.add(new ImageTile(0, null, new File(imagecursor.getString(dataColumnIndex))));
        }
        imagecursor.close();
        this._recyclerGrid.setLayoutManager(new GridLayoutManager(mActivity, 3));
        this.adapter = new RecyclerViewGridAdapter(mActivity, tiles, false); // not encrypted so set boolean to false
        this.adapter.setClickListener(new ImageClickListener(mActivity));
        _selectButton.setOnClickListener(this);
        this._recyclerGrid.setAdapter(this.adapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mActivity = (Activity) context;
    }

    private boolean checkExternalStoragePermission() {
        // first check if we have permission to read the URI
        int permissionCheck = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    public void requestExternalStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            return;
        }
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupView();
            }
        }
    }

    /*@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_gallery);
        ButterKnife.bind(this);

        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
        final String orderBy = MediaStore.Images.Media._ID;
        Cursor imagecursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
        int image_column_index = imagecursor.getColumnIndex(MediaStore.Images.Media._ID);
        int count = imagecursor.getCount();
        List<ImageTile> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            imagecursor.moveToPosition(i);
            int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);
            tiles.add(new ImageTile(0, null, new File(imagecursor.getString(dataColumnIndex))));
        }
        imagecursor.close();
        this._recyclerGrid.setLayoutManager(new GridLayoutManager(this, 3));
        this.adapter = new RecyclerViewGridAdapter(this, tiles);
        this.adapter.setClickListener(new CustomPhotoGalleryActivity.ImageClickListener(this));
        _selectButton.setOnClickListener(this);
        this._recyclerGrid.setAdapter(this.adapter);
    }*/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gallery_select:
                selectImage();
                break;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            requestExternalStoragePermission();
            refreshFragment();
        }
    }

    public void refreshFragment() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.setAllowOptimization(false);
        transaction.detach(this).attach(this).commitAllowingStateLoss();
    }

    private void selectImage() {
        _selectButton.setEnabled(false);
        List<String> selectImageFiles = new ArrayList<>();
        for (ImageTile tile : this.adapter.getTiles()) {
            if (tile.selected) {
                Log.i("SELECTED", tile.file.getAbsolutePath());
                selectImageFiles.add(tile.file.getAbsolutePath());
            }
        }
        if (selectImageFiles.size() == 0) {
            Toast.makeText(mActivity.getApplicationContext(), "Please select at least one image", Toast.LENGTH_LONG).show();
            _selectButton.setEnabled(true);
        } else {
            Intent i = new Intent(mActivity, BatchTaggingActivity.class);
            Bundle b = new Bundle();
            b.putStringArray(Const.IMAGE_FILE_BUNDLE_LABEL, selectImageFiles.toArray(new String[1]));
            i.putExtras(b);
            startActivity(i);
            _selectButton.setEnabled(true);
        }
    }

    private class ImageClickListener implements RecyclerViewGridAdapter.ItemClickListener {

        private Activity activity;

        public ImageClickListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onItemClick(View view, int position) {

            ImageView imageView = (ImageView) view;

            ImageTile tile = adapter.getItem(position);
            if (tile.selected) {
                ShapeDrawable shapedrawable = new ShapeDrawable();
                shapedrawable.setShape(new RectShape());
                shapedrawable.getPaint().setColor(Color.TRANSPARENT);
                shapedrawable.getPaint().setStrokeWidth(10f);
                shapedrawable.getPaint().setStyle(Paint.Style.STROKE);
                imageView.setBackground(shapedrawable);
                tile.selected = false;
                Log.i("SELECTED", position + " deselected");
            } else {
                ShapeDrawable shapedrawable = new ShapeDrawable();
                shapedrawable.setShape(new RectShape());
                shapedrawable.getPaint().setColor(0Xffff2929);
                shapedrawable.getPaint().setStrokeWidth(10f);
                shapedrawable.getPaint().setStyle(Paint.Style.STROKE);
                imageView.setBackground(shapedrawable);
                tile.selected = true;
                Log.i("SELECTED", position + " selected");
            }
        }
    }

}
