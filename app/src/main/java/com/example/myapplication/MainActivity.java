package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private int currentPage = 0;
    private int totalGifsLoaded = 0;
    private boolean isLoading = false;
    private final int PAGE_SIZE = 20;
    private RequestQueue requestQueue;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private EditText editText;
    private GifAdapter gifAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusBarColor();
        progressBar = findViewById(R.id.progressLoadingBar);
        recyclerView = findViewById(R.id.gifList);
        editText = findViewById(R.id.textInputField);

        progressBar.setVisibility(View.INVISIBLE);

        // Initialize the request queue
        requestQueue = Volley.newRequestQueue(this);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        gifAdapter = new GifAdapter(new ArrayList<>());
        recyclerView.setAdapter(gifAdapter);

        // Attach scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Load more gifs if scrolled to the bottom and not currently loading
                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    loadMoreGifs();
                    Log.e("LOADING", "Is Loading True");

                }
            }
        });

        getInput();

    }
    private void loadMoreGifs() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        String searchText = editText.getText().toString();
        request(searchText, false);
    }
    public void getInput() {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString();
                request(searchText, true);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String searchText = s.toString();
                request(searchText, true);

            }
        });
    }
    private void statusBarColor(){
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.M){
            getWindow().setStatusBarColor((getResources().getColor(R.color.dark_blue,this.getTheme())));
        } else if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor((getResources().getColor(R.color.dark_blue)));
        }
    }
    public void request(String searchText, Boolean isRefreshing) {
        String apiKey = "*******************************";
        String url = "https://api.giphy.com/v1/gifs/search?api_key=" + apiKey +
                "&q=" + searchText +
                "&limit=" + PAGE_SIZE +
                "&offset=" + totalGifsLoaded;
        Log.e("SEARCH TEXT::", searchText);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {

                            Log.e("SEARCH TEXT::", searchText);
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray jsonArray = jsonObject.getJSONArray("data");
                            totalGifsLoaded += jsonArray.length();
                            ArrayList<String> gifUrls = new ArrayList<>();

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject gifObject = jsonArray.getJSONObject(i);
                                JSONObject imageObject = gifObject.getJSONObject("images");
                                JSONObject originalObject = imageObject.getJSONObject("original");

                                // Extract the URL from the "original" object
                                String imageUrl = originalObject.getString("url");
                                Log.d("URL", "URL: " + imageUrl);
                                gifUrls.add(imageUrl);
                            }
                            if(!isRefreshing){
                                gifAdapter.addGifUrls(gifUrls);
                                totalGifsLoaded += gifUrls.size();

                                isLoading = false;
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                            else{
                                gifAdapter.updateGifUrls(gifUrls);
                                isLoading = false;
                                progressBar.setVisibility(View.INVISIBLE);
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        requestQueue.add(stringRequest);
    }
}
