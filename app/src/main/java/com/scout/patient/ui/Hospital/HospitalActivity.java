package com.scout.patient.ui.Hospital;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.scout.patient.Adapters.ChipsAdapter;
import com.scout.patient.Adapters.HospitalsAdapter;
import com.scout.patient.Models.ModelIntent;
import com.scout.patient.Models.ModelKeyData;
import com.scout.patient.R;
import com.scout.patient.Utilities.HelperClass;
import com.scout.patient.ui.DoctorsActivity.DoctorsActivity;
import com.scout.patient.ui.DoctorsProfile.DoctorsProfileActivity;
import com.scout.patient.ui.HospitalProfile.HospitalProfileActivity;
import com.scout.patient.ui.SearchActivity;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class HospitalActivity extends AppCompatActivity implements Contract.View,HospitalsAdapter.interfaceClickListener{
    @BindView(R.id.recyclerView) RecyclerView recyclerView;
    @BindView(R.id.progressBar) ProgressBar progressBar;
    @BindView(R.id.shimmerLayout) ShimmerFrameLayout shimmerLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.textViewSearch) TextView textViewSearch;

    public static ArrayList<ModelKeyData> list = new ArrayList<ModelKeyData>();
    HospitalsAdapter adapter;
    Unbinder unbinder;
    HospitalsPresenter presenter;
    ModelIntent modelIntent;
    Boolean isScrolling = false, isLoading = false;
    int currentItems, totalItems, scrollOutItems;
    String startingValue = null;
    LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital);
        unbinder = ButterKnife.bind(this);
        presenter = new HospitalsPresenter(HospitalActivity.this);

        modelIntent = (ModelIntent) getIntent().getSerializableExtra("modelIntent");
        if (modelIntent==null)
            modelIntent = new ModelIntent();

        textViewSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HospitalActivity.this, SearchActivity.class).putExtra("key",getString(R.string.only_hospitals)));
            }
        });

        initUi();
        isLoading = true;
        presenter.getHospitalsList(null,1);
    }

    private void initUi() {
        setToolbar();
        list.clear();
        initRecyclerView();
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();
        HelperClass.hideProgressbar(progressBar);
        recyclerView.setVisibility(View.GONE);
    }

    private void setToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(HospitalActivity.this,2));
        recyclerView.hasFixedSize();
        adapter = new HospitalsAdapter(list,HospitalActivity.this);
        adapter.setUpOnClickListener(HospitalActivity.this);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true;
                    check_loadMoreData();
                }
            }
        });
    }

    public void check_loadMoreData() {
        currentItems = manager.getChildCount();
        totalItems = manager.getItemCount();
        scrollOutItems = manager.findFirstVisibleItemPosition();

        if (isScrolling && ((currentItems + scrollOutItems == totalItems) || scrollOutItems == -1) )
            loadMoreData();
    }

    private void loadMoreData() {
        if ( startingValue != null && !isLoading) {
            isLoading = true;
            isScrolling = false;
            HelperClass.showProgressbar(progressBar);
            presenter.getHospitalsList(startingValue,1);
        }
    }

    @Override
    public void holderClick(int position) {
        Intent intent = new Intent(this, HospitalProfileActivity.class);
//        modelIntent.setIntentFromHospital(true);
//        modelIntent.setListOfDoctors(list.get(position).getHospitalDoctors());
        intent.putExtra("hospitalId",list.get(position).getId().getId());
        intent.putExtra("hospitalName",list.get(position).getName());
        startActivity(intent);
    }

    @Override
    public void notifyAdapter() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void setErrorUi(String message) {
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
        HelperClass.hideProgressbar(progressBar);
        HelperClass.toast(this,message);
        isLoading = false;
    }

    @Override
    public void updateSuccessUi(ArrayList<ModelKeyData> data) {
        if (progressBar!=null) {
            HelperClass.hideProgressbar(progressBar);
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        if (data!=null && !data.isEmpty() && list!=null) {
            list.addAll(data);
            startingValue = data.get(data.size()-1).getId().getId();
        }else
            startingValue = null;

        isLoading = false;
        notifyAdapter();
    }
}
