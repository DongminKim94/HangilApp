package com.hausung.hangil.Beacon;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hausung.hangil.R;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrInterface;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SangsangParkShowActivity extends AppCompatActivity implements BeaconConsumer {
    TextView textShow;
    private SlidrInterface slidr;
    private ImageView imageView;

    private static final String TAG = "Beacontest";
    private BeaconManager beaconManager;
    int count;
    //이 기기를 비콘과 연결했는지 안했는지 확인하기 위해 필요
    public List<Beacon> beaconList = new ArrayList<>();
    TextView textView;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sangsangpark_show);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        //스와이프 코드
        slidr = Slidr.attach(this);
        textShow = findViewById(R.id.textShow);
        imageView = (ImageView)findViewById(R.id.image);

        //비콘 매니저 생성, BeaconManager의 인스턴스 획득
        beaconManager = BeaconManager.getInstanceForApplication(this);
        textView = (TextView) findViewById(R.id.textShow);//비콘검색후 검색내용 뿌려주기위한 textview

        //비콘 매니저에서 layout 설정 'm:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25'
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        //beaconManager 설정 bind -> 서비스에 바인드를 호출
        beaconManager.bind(this);

        //beacon 을 활용하려면 블루투스 권한획득(Andoird M버전 이상)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access" );
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok,null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override
    public void onBeaconServiceConnect() {    //Beacon 서비스에 연결되면 호출된다
        //Notifier를 설정한다
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            // 비콘이 감지되면 해당 함수가 호출된다. Collection<Beacon> beacons에는 감지된 비콘의 리스트가,
            // region에는 비콘들에 대응하는 Region 객체가 들어온다.
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    beaconList.clear();
                    for (Beacon beacon : beacons) {
                        beaconList.add(beacon);
                    }
                }
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }


    // 버튼이 클릭되면 textView 에 비콘들의 정보를 뿌린다.
    public void OnButtonClicked(View view){
        // 아래에 있는 handleMessage를 부르는 함수. 맨 처음에는 0초간격이지만 한번 호출되고 나면
        // 1초마다 불러온다.
        handler.sendEmptyMessage(0);
    }
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            textView.setText("");

            //db에서 비콘 정보 저장하고 가져올 때 필요
            FirebaseFirestore db= FirebaseFirestore.getInstance();

            // 비콘의 아이디와 거리를 측정하여 textView에 넣는다.
            for(final Beacon beacon : beaconList){
                String uuid=beacon.getId1().toString(); //beacon uuid
                int major = beacon.getId2().toInt(); //beacon major
                int minor = beacon.getId3().toInt();// beacon minor



                else if(minor==45325){
                    final DocumentReference beaconDoc=db.collection("Beacon").document("45325");
                    beaconDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                    //현재 count 받아오기
                                    count=(int)document.get("count");
                                    if(count<=5){
                                        //getDistance를 했을 때 해당 비콘 범위 내에 있다면
                                        //문서의 count 값 올려주기
                                        beaconDoc.update("count",count+1);
                                        //beacon 의 식별을 위하여 major 값으로 확인
                                        //textView.append("ID 1 : " + beacon.getId2() + " / " + "Distance : " + Double.parseDouble(String.format("%.3f", beacon.getDistance())) + "m\n");
                                        textView.append("여기는 상상파크 입니다\n");
                                        textView.append("Distance : " + Double.parseDouble(String.format("%.3f", beacon.getDistance())));
                                        //textView.append("Beacon Bluetooth Id : "+address+"\n");
                                        //textView.append("Beacon UUID : "+uuid+"\n");
                                        if( beacon.getDistance()<= 1){
                                            imageView.setImageResource(R.drawable.one);
                                            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                                        }
                                        else if( 2<= beacon.getDistance() &&beacon.getDistance()<= 5){
                                            imageView.setImageResource(R.drawable.two);
                                            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                                        }
                                        else{

    Button ConfirmSubmit = (Button) findViewById(R.id.Submit);
        ConfirmSubmit.setOnClickListener(
                new Button.OnClickListener() {
        public void onClick(View v) {
            //예약 정보 업데이트
            name = mStrName.getText().toString();
            id = mStrStudentid.getText().toString();
            Spinner spinner=(Spinner)findViewById(R.id.room);
            room=spinner.getSelectedItem().toString();
            //유저 아이디 정보를 받아오기 위해 FriebaseUser 필드 생성
            FirebaseUser userId = FirebaseAuth.getInstance().getCurrentUser();
            //MapActivity로 가는 인텐트 생성
            Intent intent = new Intent(getApplication(), MapActivity.class);
            //파이어베이스에 정보 저장하기
            FirebaseFirestore db= FirebaseFirestore.getInstance();
            Map<String, Object> user = new HashMap<>();
            user.put("name", name);
            user.put("id", id);
            user.put("mStrTime",mStrTime);
            user.put("mStrFinishTime",mStrFinishTime);
            user.put("mStrDate",mStrDate);
            user.put("building","도서관 스터디룸");
            user.put("room",room);
            user.put("mId", userId.getEmail());
            db.collection("AllLibraryStudyRoom")
                    .add(user)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(id, "DocumentSnapshot added with ID: " + documentReference.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(id, "Error adding document", e);
                        }
                    });
        }
    }
}