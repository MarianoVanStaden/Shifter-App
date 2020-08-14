package com.scout.patient.ui.DoctorsActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.scout.patient.Adapters.DoctorsAdapter;
import com.scout.patient.Models.ModelIntent;
import com.scout.patient.Models.ModelKeyData;
import com.scout.patient.R;
import com.scout.patient.Utilities.HelperClass;
import com.scout.patient.ui.DoctorsProfile.DoctorsProfileActivity;
import com.scout.patient.ui.SearchActivity;

import org.w3c.dom.Text;

import java.util.ArrayList;

import butterknife.BindInt;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DoctorsActivity extends AppCompatActivity implements Contract.View,DoctorsAdapter.interfaceClickListener{//,SwipeRefreshLayout.OnRefreshListener{
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.shimmerLayout)
    ShimmerFrameLayout shimmerLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.collapsingToolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.textViewSearch)
    TextView textViewSearch;

    public static ArrayList<ModelKeyData> list = new ArrayList<ModelKeyData>();
    DoctorsAdapter adapter;
    Unbinder unbinder;
    DoctorsActivityPresenter presenter;
    ModelIntent modelIntent;
    Boolean isScrolling = false, isLoading = false;
    int currentItems, totalItems, scrollOutItems;
    String startingValue = null;        // Id of the doctor for which we make api call to get doctors having their id greater than  this.
    int startingIndex = -1;             // Index from which we load doctors of list of doctor's provided by hospital.
    LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctors);
        unbinder = ButterKnife.bind(this);
        presenter = new DoctorsActivityPresenter(DoctorsActivity.this);

        modelIntent = (ModelIntent) getIntent().getSerializableExtra("modelIntent");

        initUi();

        textViewSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DoctorsActivity.this, SearchActivity.class).putExtra("key",getString(R.string.only_doctors)));
            }
        });

        isLoading = true;
        if (modelIntent!=null && modelIntent.isIntentFromHospital()) {
            presenter.loadDoctorsList(modelIntent.getListOfDoctors(),0);
        }
        else {
            presenter.loadDoctorsList("", 2);
        }
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
        collapsingToolbarLayout.setTitle(getString(R.string.our_doctors));
        collapsingToolbarLayout.setTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(DoctorsActivity.this,2));
        recyclerView.hasFixedSize();
        adapter = new DoctorsAdapter(list,DoctorsActivity.this);
        adapter.setUpOnClickListener(DoctorsActivity.this);
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
        if (!isLoading ) {
            if(modelIntent!=null && modelIntent.isIntentFromHospital() && startingIndex!=-1){
                isLoading = true;
                isScrolling = false;
                HelperClass.showProgressbar(progressBar);
                presenter.loadDoctorsList(modelIntent.getListOfDoctors(),startingIndex);
            }else if (startingValue != null ){
                isLoading = true;
                isScrolling = false;
                HelperClass.showProgressbar(progressBar);
                presenter.loadDoctorsList(startingValue, 2);
            }
        }
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
            if (modelIntent!=null && modelIntent.isIntentFromHospital()) {
                startingIndex = list.size();
            }else {
                startingValue = data.get(data.size() - 1).getId().getId();
            }
        }else {
            startingValue = null;
            startingIndex = -1;
        }

        isLoading = false;
        notifyAdapter();
    }

    @Override
    public void holderClick(int position) {
        Intent intent = new Intent(this, DoctorsProfileActivity.class);
        intent.putExtra("doctorId",list.get(position).getId().getId());
        intent.putExtra("doctorName",list.get(position).getName());
//        if (modelIntent==null)
//            modelIntent = new ModelIntent();
        //modelIntent.setDoctorProfileInfo(list.get(position));
        startActivity(intent);
//        if (modelIntent.getBookAppointmentData()!=null)
//            finish();
    }
}
