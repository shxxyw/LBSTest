package com.example.shxxy.lbstest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public LocationClient mLocationClient;

    private TextView positionText;

    private MapView mapView;

    private BaiduMap baiduMap;

    private boolean isFirstLocate = true;
    Button location;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();//获取BaiduMap实例
        baiduMap.setMyLocationEnabled(true);//开启显示自身位置
        positionText = (TextView) findViewById(R.id.position_text_view);
        location = (Button) findViewById(R.id.location);

        /*--------进行多个运行时权限的申请--------*/
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.
                permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.
                permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.
                permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){
        String[] permissions = permissionList.toArray(new String[permissionList.size()]);//将List类型转化成String数组
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else {
            requestLocation();
          isFirstLocate =true;
        }
    }
    private void navigateTo(BDLocation location){
        //这里会存在再次打开时候没有更新到定位的情况
        if (isFirstLocate){
            Toast.makeText(this, "nav to " + location.getAddrStr(), Toast.LENGTH_SHORT).show();
            LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);//定位有时会出错，需要多次定位
            baiduMap.animateMapStatus(update);
          //  update = MapStatusUpdateFactory.zoomTo(16f);
           // baiduMap.animateMapStatus(update);
            //  update = MapStatusUpdateFactory.newLatLngZoom(ll,12.5f); 即放缩又定位
            isFirstLocate = false;
        }
          /*使用MyLocationData.Builder封装当前位置，构造实例传入数据，完成后利用build生成MyLocationData实例，
          最后在baiduMap中设置和传入；注意 要用setMyLocationEnabled()方法开启，也要记得关闭
          * */
        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        MyLocationData locationData = locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }
    private void requestLocation (){
        initLocation();
        mLocationClient.start();
}

//实时更新位置
    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(5000);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0){
                    for (int result : grantResults){
                        if (result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"必须同意所有的权限才能使用本程序",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else {
                    Toast.makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }
//地图跳转到定位场所

    public class MyLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation( final BDLocation bdLocation) {
           //将BDLocation对象传给定位navition方法，实现在地图上定位
            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation
                    || bdLocation.getLocType() == BDLocation.TypeNetWorkLocation){
                navigateTo(bdLocation);
            }
            location.setOnClickListener(new View.OnClickListener() {//设置回到定位按钮
                @Override
                public void onClick(View view) {
                    LatLng ll = new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
                    MapStatusUpdate update;
                    update = MapStatusUpdateFactory.zoomTo(16f);
                    baiduMap.animateMapStatus(update);//只会执行后一个
                     update = MapStatusUpdateFactory.newLatLngZoom(ll,16f);
                    baiduMap.animateMapStatus(update);

                }
            });
          /*  runOnUiThread (new Runnable(){//将线程切换到主线程进行UI操作
                @Override
                public void run() {//使用百度地图确定位置和定位不能同时用，会出错
                    StringBuilder currentPosition = new StringBuilder();
                    currentPosition.append("纬度:").append(bdLocation.getLatitude()).append("\n");
                    currentPosition.append("经线：").append(bdLocation.getLongitude()).append("\n");
                    currentPosition.append("国家：").append(bdLocation.getCountry()).append("\n");
                    currentPosition.append("省：").append(bdLocation.getProvince()).append("\n");
                    currentPosition.append("市：").append(bdLocation.getCity()).append("\n");
                    currentPosition.append("区：").append(bdLocation.getDistrict()).append("\n");
                    currentPosition.append("街道：").append(bdLocation.getStreet()).append("\n");
                    currentPosition.append("定位方式：");
                    if (bdLocation.getLocType()  == bdLocation.TypeGpsLocation){
                        currentPosition.append("GPS");
                    } else if (bdLocation.getLocType() == bdLocation.TypeNetWorkLocation){
                        currentPosition.append("网络");
                    }
                    positionText.setText(currentPosition);
                }
            });*/
        }
        /*@Override
        public void onConnectHotSpotMessage(String s,int i ){
        }
        */
    }
}
