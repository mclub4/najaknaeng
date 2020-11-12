package com.example.najakneang;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context){
        super(context, "najakneang.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        try{
            String SQLCreate = "CREATE TABLE IF NOT EXISTS NAJAKNEANG (NAME TEXT, QUANTITY INTEGER NOT NULL, " +
                    "REGISTDATE TEXT, EXPIREDATE TEXT, CLASSID TEXT, FRIDGEID TEXT, STORESTATE TEXT, STORAGEID TEXT);";
            db.execSQL(SQLCreate);
            Log.i("SQL", "DB_CREATED");
        }catch(SQLiteException e){
            e.printStackTrace() ;
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int older, int newer) {
        Log.i("SQL", "DB_UPGRADED");
        String SQLDrop = "DROP TABLE IF EXISTS NAJAKNEANG;";
        db.execSQL(SQLDrop);
        onCreate(db);
    }

    public void onOpen(SQLiteDatabase db){
        Log.i("SQL", "DB_OPENED");
    }

};

public class MainActivity extends AppCompatActivity {

    //네트워크 파싱 스레드
    protected class ParsingThread extends Thread{
        public void run(){
            getYoutube();
        }
    }

    ArrayList<MainRecommendRecyclerItem> dataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ParsingThread parsingThread = new ParsingThread(); //데이터 파싱 시작
        parsingThread.start();

        setupFreshnessRecycler();
        setupOnClickFreshnessLayout();
        setupFridgeViewPager();

        try {
            parsingThread.join(); // ParsingThread가 종료될때까지 대기
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setupRecommendRecycler(dataList); //Parsing이 완료되면 데이터 표시
    }

    /**
     *  DB 관리
     */

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private boolean openDataBase(){
        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        return true;
    }

    /**
     * 신선도 위험품목 설정
     * TODO: 이후에 신선도가 적은 품목을 받아와서 수정하면 됨.
     */

    private void setupFreshnessItem(){
        Date currentTime = Calendar.getInstance().getTime();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentTime);
        String now[] = date.split("-");

        //유통기한 받아오기 제작중
    }

    private void setupFreshnessRecycler() {
        RecyclerView recyclerView = findViewById(R.id.recycler_freshness_main);
        // 가데이터
        MainFreshnessRecyclerItem[] items = {
                new MainFreshnessRecyclerItem(
                        "품목 1", R.drawable.ic_launcher_background, 3),
                new MainFreshnessRecyclerItem(
                        "품목 2", R.drawable.ic_launcher_background, 30)
        };

        MainFreshnessRecyclerAdapter adapter = new MainFreshnessRecyclerAdapter(items);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this, LinearLayoutManager.HORIZONTAL, false
                )
        );
    }

    private void setupOnClickFreshnessLayout() {
        RelativeLayout freshness = findViewById(R.id.layout_freshness_main);
        freshness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), FreshnessActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * 냉장고 슬라이더 설정
     * TODO: 현재 자신의 냉장고 목록에 따라 조정되야됨
     * TODO: 클릭하면 그 냉장고 화면으로 갈수 있어야됨
     * TODO: 맨 마지막에 추가버튼 있어야됨
     * 필요에 따라 item 클래스 생성해서 이름, 사진, 냉장고 id 같은것들 담을수있게해야할듯?
     */
    private void setupFridgeViewPager() {
        ViewPager2 viewPager = findViewById(R.id.viewpager_fridge_main);
        // 가데이터
        String[] items = {
                "냉장고 1",
                "냉장고 2",
                "김치 냉장고 1"
        };

        MainFridgeViewPagerAdapter adapter = new MainFridgeViewPagerAdapter(items);
        viewPager.setAdapter(adapter);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
    }


    /**
     * 오늘의 추천메뉴 설정
     */

    final String server_key = "AIzaSyBvbUl4A4Y7lAbfAoUunccnorGm0YoqNfE";
    /**TODO:감자 부분을 DB에서 받아온 재료로 바꿀것*/
    final String ingredient = "감자" + "레시피";

    private void getYoutube(){
        try{

            //Youtube Data Api를 이용하여 영상 검색 결과 정보가 담긴 json을 가져옴
            String youtube = "https://www.googleapis.com/youtube/v3/search?q=" + ingredient + "&key="
                    + server_key + "&maxResults=3&part=snippet";
            URL url = new URL(youtube);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();

            //접속 성공 확인
            if(connect.getResponseCode() == HttpURLConnection.HTTP_OK){
                InputStreamReader tmp = new InputStreamReader(connect.getInputStream(), "UTF-8");
                BufferedReader reader = new BufferedReader(tmp);

                //웹의 json을 줄단위로 읽어 page에 하나하나 저장함
                String line = null;
                StringBuffer buffer = new StringBuffer();

                while ((line = reader.readLine()) != null) {
                    Log.i("통신 결과", line);
                    buffer.append(line);
                }

                String receiveMsg = buffer.toString();
                Log.i("통신 결과", "변환성공");
                reader.close();

                //JSON 파싱
                try {
                    JSONObject jsonObject = new JSONObject(receiveMsg);

                    JSONArray youtubeArray = jsonObject.getJSONArray("items");

                    for (int i = 0; i < youtubeArray.length(); i++) {
                        JSONObject youtubeObject = youtubeArray.getJSONObject(i);

                        MainRecommendRecyclerItem youtubeData = new MainRecommendRecyclerItem();

                        //Youtube 제목, 채널명 받아오기
                        JSONObject tmpObject = youtubeObject.getJSONObject("snippet");
                        youtubeData.setTitle(tmpObject.getString("title"));
                        /**eplisze 또는 marquee속성을 통해서 긴 제목 자르기*/
                        Log.i("통신 결과", tmpObject.getString("title") + "를 받아옴");
                        youtubeData.setCreator(tmpObject.getString("channelTitle"));
                        Log.i("통신 결과", tmpObject.getString("channelTitle") + "를 받아옴");

                        //VideoId 받아오기기
                        tmpObject = youtubeObject.getJSONObject("id");
                        youtubeData.setVideoId(tmpObject.getString("videoId"));
                        Log.i("통신 결과", tmpObject.getString("videoId") + "를 받아옴");

                        //썸네일 받아오기
//                        String thumbnail = "http://img.youtube.com/vi/" + youtubeData.getVideoId() + "/default.jpg";
                        String thumbnail = "https://i.ytimg.com/vi/" + youtubeData.getVideoId() + "/mqdefault.jpg";
                        URL thumbnail_url = new URL(thumbnail);
                        Bitmap bitmap = BitmapFactory.decodeStream(thumbnail_url.openStream());
                        youtubeData.setBitmap(bitmap);

                        //RecyclerView에 dataset추가
                        dataList.add(youtubeData);
                    }
                }catch(JSONException e) {
                    e.printStackTrace();
                }

            }
            else{
                Log.i("통신 결과", connect.getResponseCode() + "에러");
            }
            connect.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupRecommendRecycler(ArrayList<MainRecommendRecyclerItem> dataList) {
        RecyclerView recyclerView = findViewById(R.id.recycler_recommend_main);

        MainRecommendRecyclerAdapter adapter = new MainRecommendRecyclerAdapter(dataList);

        //클릭시 유튜브로 이동
        adapter.setItemClickListener(new MainRecommendRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Log.i("데이터 클릭", position + "가 클릭되었음");
                String videoId = dataList.get(position).getVideoId();
                Log.i("통신", videoId + "가 나왔음");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse( "http://youtube.com/watch?v=/" + videoId ));
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        );
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
    }
}