package com.example.testapp;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import androidx.fragment.app.FragmentActivity;

import com.example.testapp.api.ApiInterface;
import com.example.testapp.model.map.OverViewPolyline;
import com.example.testapp.model.map.Result;
import com.example.testapp.model.map.Routes;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.ButtCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserMapDirectionsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final String keyApi = "AIzaSyAArm8OLzCuc8PBeBnEh9NKbQK_ss-ImzI";
    private GoogleMap map;
    private ApiInterface apiInterface;
    private List<LatLng> polylineList;
    private PolylineOptions polylineOptions;
    private boolean apiCalled = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_map_directions);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync( this);


        Retrofit retrofit = new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl("https://maps.googleapis.com/").build();
        apiInterface = retrofit.create(ApiInterface.class);

    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setTrafficEnabled(true);
        if (!apiCalled) {
            getDirection("97 Man thiện, Thành phố thủ đức", "360/19/7A Lã xuân oai, long trường");
            apiCalled = true;
        }
//        getDirection("97 Man thiện, Thành phố thủ đức", "360/19/7A Lã xuân oai, long trường");
    }

    private void getDirection(String destination,String origin){
        apiInterface.getDirection(destination,origin,
                keyApi).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<Result>() {
            @Override
            public void onSubscribe(Disposable d) {
            }
            @Override
            public void onSuccess(Result result) {
                ArrayList<LatLng> latLngs = new ArrayList<>();
                
                // get encode (point) from direction API
                for(Routes rt : result.getRoutes()){
                    String point = rt.getOverview_polyline().getPoints();
                    latLngs.addAll(decodePoly(point));
                }

                // Format polyline
                polylineOptions = new PolylineOptions();
                polylineOptions.color(getColor(R.color.mainColor));
                polylineOptions.width(8);
                polylineOptions.startCap(new ButtCap());
                polylineOptions.jointType(JointType.ROUND);

                // Use latLng list draw polyline
                polylineOptions.clickable(true).addAll(latLngs);

                // Add polyLine in google map
                map.addPolyline(polylineOptions);
                
                // Set animationCamera focus polyLine
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                LatLng start = new LatLng(result.getRoutes().get(0).getLegs().get(0).getEnd_location().getLat(), result.getRoutes().get(0).getLegs().get(0).getEnd_location().getLng());
                LatLng end = new LatLng(result.getRoutes().get(0).getLegs().get(0).getStart_location().getLat(), result.getRoutes().get(0).getLegs().get(0).getStart_location().getLng());
                String start_address = result.getRoutes().get(0).getLegs().get(0).getStart_address();
                String end_address = result.getRoutes().get(0).getLegs().get(0).getEnd_address();
                addMarkers(start, end, start_address, end_address);
                builder.include(start);
                builder.include(end);
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),100));
            }
            @Override
            public void onError(Throwable e) {
                Log.e("Error", "Error occurred: " + e.getMessage());
            }
        });

    }
    // Change encode to list LatLng
    private List<LatLng> decodePoly (String encoded){
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len){
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1 ):(result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1 ):(result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));

            poly.add(p);

        }
        return poly;
    }

    private void addMarkers(LatLng origin, LatLng destination, String start_address, String end_address) {
        MarkerOptions originMarkerOptions = new MarkerOptions()
                .position(origin)
                .title(start_address)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marker_cooffeshop));
        map.addMarker(originMarkerOptions);

        MarkerOptions destinationMarkerOptions = new MarkerOptions()
                .position(destination)
                .title(end_address)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marker_location));
        map.addMarker(destinationMarkerOptions);
    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}