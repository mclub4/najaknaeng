package com.example.najakneang.model;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.najakneang.R;
import com.example.najakneang.activity.MainActivity;
import com.example.najakneang.db.DBContract;
import com.example.najakneang.db.DBHelper;

import java.lang.reflect.Array;


public class Dialog_Fridge extends Dialog implements View.OnClickListener {

    public interface DialogEventListener {
        public void DialogEvent(boolean value);
    }

    private Context context;
    private DialogEventListener onDialogEventListener;

    private EditText fridge_name;
    private TextView btn_ok;
    private TextView btn_cancel;
    private Spinner spinner;

    SQLiteDatabase db = MainActivity.db;

    public Dialog_Fridge(@NonNull Context context){
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //Dialog 실행시, 주변 화면 흐리게 하기
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);

        //Dialog 레이아웃 지정
        setContentView(R.layout.dialog_fridge);

        fridge_name = findViewById(R.id.editFridgeName);
        btn_ok = findViewById(R.id.btn_ok);
        btn_cancel = findViewById(R.id.btn_cancel);
        spinner = findViewById(R.id.category_spinner);

        btn_ok.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);

        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.spinnerArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("spinner", spinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onClick(View v){
        switch(v.getId()){
            case R.id.btn_ok:
                String name = fridge_name.getText().toString();
                if(name.trim().getBytes().length > 0){
                    Cursor cursor = db.query(
                            DBContract.FridgeEntry.TABLE_NAME,
                            null,
                            DBContract.FridgeEntry.COLUMN_NAME + " = ? ",
                            new String[]{ name },
                            null,
                            null,
                            null,
                            "1"
                    );

                    if(spinner.getSelectedItemPosition() == 0){
                        cursor.close();
                        Toast.makeText(context.getApplicationContext(), "냉장고 종류를 지정해주세요",Toast.LENGTH_SHORT).show();
                    }
                    else if (cursor.getCount()>0) {
                        cursor.close();
                        Toast.makeText(context.getApplicationContext(), "이미 있는 냉장고입니다",Toast.LENGTH_SHORT).show();
                    } else {
                        cursor.close();
                        ContentValues values = new ContentValues();
                        values.put(DBContract.FridgeEntry.COLUMN_NAME, name);
                        values.put(DBContract.FridgeEntry.COLUMN_CATEGORY, spinner.getSelectedItem().toString());
                        db.insert(DBContract.FridgeEntry.TABLE_NAME, null, values);

                        //현재 페이지에 정보갱신
                        ((MainActivity)context).setupFridgeViewPager();
                        Toast.makeText(context.getApplicationContext(), "냉장고가 추가되었습니다",Toast.LENGTH_SHORT).show();
                        dismiss();
                    }

                }
                else{
                    Toast.makeText(context.getApplicationContext(), "이름을 입력해주세요",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_cancel:
                dismiss();
                break;
        }
    }
};