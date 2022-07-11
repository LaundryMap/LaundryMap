package com.kakaologin_sample;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.kakao.sdk.user.UserApiClient;
import com.kakaologin_sample.R;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;


public class MapFragmentActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private JSONArray washterias;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;


    private ArrayList<Marker> markers;
    private static final String HOST = "143.248.199.127";
    private static final String PORT = "80";
    private static String profile_image_url;

    //추가
    private static String nickname;
    private static String kakao_id;
    private TextView tv_name;
    private TextView TEL;
    private ImageView image;
    private Boolean flag;
    private TextView btn_logout;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map_fragment_activity);

        Intent intent = getIntent();

        profile_image_url = intent.getStringExtra("profile_image");

        //추가
        nickname=intent.getStringExtra("nickname");
        kakao_id= String.valueOf(intent.getLongExtra("kakao_id",0L));

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.naverMap);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.naverMap, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
       

//header change
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView)findViewById(R.id.navigation_view);
        View headerview = navigationView.getHeaderView(0);

        ImageView imageView = (ImageView)headerview.findViewById(R.id.iv_image);
        Glide.with(MapFragmentActivity.this).load(profile_image_url).into(imageView);
        TextView tv_name = headerview.findViewById(R.id.tv_name);
        tv_name.setText(nickname);

        btn_logout = headerview.findViewById(R.id.btn_logout);
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserApiClient.getInstance().logout(error -> {
                    if (error != null) {
                        Log.e("tag", "로그아웃 실패, SDK에서 토큰 삭제됨", error);
                    } else {
                        Log.e("tag", "로그아웃 성공, SDK에서 토큰 삭제됨");
                        Intent goto_login_intent = new Intent(v.getContext(), LoginActivity.class);
                        startActivity(goto_login_intent);
                    }
                    return null;
                });
            }
        });

        Button reserve_cancel_btn = (Button)headerview.findViewById(R.id.reserve_cancel_btn);
        TextView reserve_washteria_name= (TextView)headerview.findViewById(R.id.reserve_washteria_name);
        TextView reserve_washer_type = (TextView)headerview.findViewById(R.id.reserve_washer_type);
        TextView reserve_start_time = (TextView)headerview.findViewById(R.id.reserve_start_time);
        TextView reserve_open_reservation = (TextView)headerview.findViewById(R.id.navigation_bar_open_reservation);

        LinearLayout layout_reservation = (LinearLayout)headerview.findViewById(R.id.layout_reservation);
        LinearLayout layout_goto_reservation = (LinearLayout)headerview.findViewById(R.id.layout_goto_reservation);
        layout_goto_reservation.setVisibility(View.GONE);
        layout_reservation.setVisibility(View.GONE);


        TextView no_reservation_date = (TextView)headerview.findViewById(R.id.no_reservation_date);
        Button go_to_reserveCreateion = (Button)headerview.findViewById(R.id.go_to_reserveCreateion);


        RequestQueue requestQueue = Volley.newRequestQueue(MapFragmentActivity.this);
        String uri2 = String.format("http://"+HOST+"/load_reservation?id="+kakao_id);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, uri2, new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                try {
                    JSONObject jsonObject = new JSONObject(response.toString());
                    Log.d("response",jsonObject.toString());
                    String name = String.valueOf(jsonObject.getJSONArray("result").getJSONObject(0).getString("name"));
                    String machine_type = String.valueOf(jsonObject.getJSONArray("result").getJSONObject(0).getString("machine_type"));
                    String start_time = String.valueOf(jsonObject.getJSONArray("result").getJSONObject(0).getString("reserve_start_time"));
                    start_time=start_time.substring(10,16);
                    flag=true;
                    switch(machine_type) {
                        case "big_dryer":
                            machine_type = "대형 건조기";
                            break;
                        case "dryer":
                            machine_type ="중형 건조기";
                            break;
                        case "big_washer" :
                            machine_type ="대형 세탁기";
                            break;
                        case "washer":
                            machine_type ="중형 세탁기";
                            break;
                    }
                    reserve_washteria_name.setText(name);
                    reserve_washer_type.setText(machine_type);
                    reserve_start_time.setText(start_time);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("asdf", "에러: " + error.toString());
                //추가
                flag=false;
            }
        });
        requestQueue.add(stringRequest);

        reserve_open_reservation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout layout_reservation = findViewById(R.id.layout_reservation);
                LinearLayout layout_goto_reservation = findViewById(R.id.layout_goto_reservation);
                if (flag) {
                    if (layout_reservation.getVisibility()==View.VISIBLE){
                        layout_reservation.setVisibility(View.GONE);
                    }else{
                        layout_reservation.setVisibility(View.VISIBLE);
                    }
                    layout_goto_reservation.setVisibility(View.GONE);
                }
                else{
                    if (layout_goto_reservation.getVisibility()==View.VISIBLE){
                        layout_goto_reservation.setVisibility(View.GONE);
                    }else{
                        layout_goto_reservation.setVisibility(View.VISIBLE);
                    }
                    layout_reservation.setVisibility(View.GONE);
                }

            }
        });


        reserve_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // String uri2 = String.format("http://"+HOST+"/reservation/cancel/?id="+kakao_id);
                String uri2 = String.format("http://"+HOST+"/reservation/cancel?id=123");
                StringRequest stringRequest = new StringRequest(Request.Method.GET, uri2, new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response.toString());
                            layout_reservation.setVisibility(View.GONE);
                            layout_goto_reservation.setVisibility(View.VISIBLE);

                            no_reservation_date.setText(getTime());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("asdf", "에러: " + error.toString());
                    }
                });
                requestQueue.add(stringRequest);
            }});


        go_to_reserveCreateion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 drawerLayout.closeDrawer(GravityCompat.END);
//                onBackPressed();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.custom_tool_bar, menu);
        return true;
    }


    //추가
    private String getTime() {
        long mNow;
        Date mDate;
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd");
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }

    private void showView(final View view){
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_up);
        //use this to make it longer:  animation.setDuration(1000);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }
        });

        view.startAnimation(animation);
    }

    private void hideView(final View view){
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_down);
        //use this to make it longer:  animation.setDuration(1000);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }
        });
        view.startAnimation(animation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btnCommunityNotification: { // 왼쪽 상단 버튼 눌렀을 때
                drawerLayout.openDrawer(GravityCompat.END);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        naverMap.getUiSettings().setLocationButtonEnabled(true);

        try {
            load_markers();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void load_markers() throws JSONException {

        String url = "http://" + HOST + ":" + PORT + "/washteria_location";

        RequestQueue requestQueue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener() {
            @Override
            public void onResponse(Object response) {
         //       Log.d("asdf", response.toString());

                try {
                    JSONObject jsonObject = new JSONObject(response.toString());
                    markers = new ArrayList<Marker>();
                    washterias = jsonObject.getJSONArray("result");
                    //Log.d("asdf", washterias.toString());

                    for (int i = 0; i < washterias.length(); i++) {
                        JSONObject washteria = washterias.getJSONObject(i);

                        int washteria_id = washteria.getInt("washteria_id");
                        String washteria_name = washteria.getString("name");
                        int washer_big_num = washteria.getInt("washer_big_num");
                        int washer_medium_num = washteria.getInt("washer_medium_num");
                        int dryer_big_num  = washteria.getInt("dryer_big_num");
                        int dryer_medium_num  = washteria.getInt("dryer_medium_num");

                        Marker marker = new Marker();
                        marker.setPosition(new LatLng(washteria.getDouble("locationY"), washteria.getDouble("locationX")));
                        marker.setCaptionText(washteria_name);
                        marker.setCaptionRequestedWidth(200);
                        marker.setTag(washteria_id);
                        marker.setWidth(120);
                        marker.setHeight(120);
                        marker.setIcon(OverlayImage.fromResource(R.drawable.washer2));

                       // Log.d("asdf", marker.getTag() + " : " + marker.getPosition().toString());
                        marker.setMap(naverMap);

                        marker.setOnClickListener(new Overlay.OnClickListener() {
                            @Override
                            public boolean onClick(@NonNull Overlay overlay) {
                                Dialog mDialog = new Dialog(MapFragmentActivity.this);
                                mDialog.setContentView(R.layout.map_popup_dialog);
                                mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                                String url = "http://"+HOST+":"+PORT+"/washteria_machines?id="+washteria_id;

                                RequestQueue requestQueue = Volley.newRequestQueue(MapFragmentActivity.this);

                                StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener(){
                                    @Override
                                    public void onResponse(Object response) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(response.toString());

                                            JSONArray machines = jsonObject.getJSONArray("result");

                                            int washer_num = 0;
                                            int dryer_num = 0;
                                            int etc_num = 0;

                                            //추가
                                            String Tel = washteria.getString("TEL") ;
                                            String washteriaImageurl = washteria.getString("thumUrl");
                                            Log.d("Tel", Tel);
                                            Log.d("Url",washteriaImageurl);
                                            for(int i=0; i<machines.length(); i++){
                                                JSONObject machine = machines.getJSONObject(i);
                                                int status = machine.getInt("status");
                                                String machine_type = machine.getString("machine_type");

                                                if(status == 0){
                                                    switch(machine_type){
                                                        case "big_washer" : washer_num += 1; break;
                                                        case "washer" : washer_num += 1; break;
                                                        case "big_dryer" : dryer_num += 1; break;
                                                        case "dryer" : dryer_num += 1; break;
                                                        default : etc_num += 1; break;
                                                    }
                                                }
                                                //추가
                                                TextView TEL = mDialog.findViewById(R.id.TEL);
                                                TEL.setText(Tel);
                                                ImageView Dialogimage = mDialog.findViewById(R.id.image);
                                                Glide.with(MapFragmentActivity.this).load(washteriaImageurl).into(Dialogimage);

                                                TextView washer_num_tv = mDialog.findViewById(R.id.washer_num);
                                                washer_num_tv.setText("세탁기 : " + washer_num + "대");
                                                TextView dryer_num_tv = mDialog.findViewById(R.id.dryer_num);
                                                dryer_num_tv.setText("건조기 : " + dryer_num + "대");
                                                TextView etc_num_tv = mDialog.findViewById(R.id.etc_num);
                                                etc_num_tv.setText("기타 : " + etc_num + "대");
                                            }
                                        }
                                        catch (JSONException e) {
                                        e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d("asdf","에러: " + error.toString());
                                    }
                                });

                                requestQueue.add(stringRequest);

                                TextView washteria_name_tv = mDialog.findViewById(R.id.washteria_name);
                                washteria_name_tv.setText(marker.getCaptionText());

                                TextView washer_num = mDialog.findViewById(R.id.washer_num);
//                                washer_num.setText()
                                TextView dryer_num = mDialog.findViewById(R.id.dryer_num);
//                                dryer_num.setText()
                                TextView etc_num = mDialog.findViewById(R.id.etc_num);
//                                etc_num.setText()

                                Button open_reservation = mDialog.findViewById(R.id.open_reservation);
                                open_reservation.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent intent = new Intent(MapFragmentActivity.this, ReserveActivity.class);
                                        intent.putExtra("washteria_id", washteria_id);
                                        intent.putExtra("washteria_name", washteria_name);
                                        intent.putExtra("washer_big_num", washer_big_num);
                                        intent.putExtra("washer_num", washer_medium_num);
                                        intent.putExtra("dryer_big_num", dryer_big_num);
                                        intent.putExtra("dryer_num", dryer_medium_num);
                                        startActivity(intent);
                                    }
                                });

                                mDialog.getWindow().setDimAmount(0);
                                mDialog.getWindow().setGravity(Gravity.BOTTOM);
                                mDialog.show();

                                return false;
                            }
                        });
                        markers.add(marker);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //에러
                Log.d("asdf", "에러: " + error.toString());
            }
        });
        requestQueue.add(stringRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }


}