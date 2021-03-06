package com.example.najakneang.model;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.najakneang.R;
import com.example.najakneang.activity.FreshnessActivity;
import com.example.najakneang.activity.GoodsActivity;
import com.example.najakneang.activity.MainActivity;
import com.example.najakneang.activity.SectionActivity;
import com.example.najakneang.db.DBContract;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

// 파일 설명 : 재료 추가 및 수정 Dialog의 기능을 구성하는 파일
// 파일 주요기능 : 사용자가 Dialog에 입력한 정보를 바탕으로 유효한지 검토하고 DB에 저장 및 수정

// 클래스 설명 : Dialog의 EditText, Spinner에서 입력한 정보를 검토하고 DB에 저장 및 수정

public class GoodsDialog extends Dialog implements View.OnClickListener {

    private Long id;
    private String nameStr;
    private String expireDateStr;
    private String quantityStr;
    private String fridge;
    private String section;
    private String fridgeCategory = "";
    private final Context context;

    private TextView dialogText;
    private EditText name;
    private EditText quantity;
    private EditText expireDate;
    private Spinner typeSpinner;
    private Spinner fridgeSpinner;
    private Spinner sectionSpinner;
    private LinearLayout fridgeLayout;

    SQLiteDatabase db = MainActivity.db;

    // 메서드 설명 : GoodsDialog 객체를 생성하고 Dialog가 생선된 Activity의 Context와 연결
    // (FreshnessActivity에서 재료를 추가할 경우)
    public GoodsDialog(@NonNull Context context){
        super(context);
        this.context = context;
    }

    // 메서드 설명 : GoodsDialog 객체를 생성하고 Dialog가 생선된 Activity의 Context, 수정할 재료의 id, 이름, 수량, 유통기한을 받아옴
    // (GoodsActivity에서 재료 수정할 경우)
    public GoodsDialog(@NonNull Context context, Long id, String nameStr, String quantityStr, String expireDateStr){
        super(context);
        this.context = context;
        this.id = id;
        this.quantityStr = quantityStr;
        this.nameStr = nameStr;
        this.expireDateStr = expireDateStr;
    }

    // 메서드 설명 : GoodsDialog 객체를 생성하고 Dialog가 생선된 Activity의 Context, 해당 구역의 냉장고, 이름, 보관 상태를 받아옴
    // (SectionActivity에서 재료를 추가할 경우)
    public GoodsDialog(@NonNull Context context, String fridge, String section, String fridgeCategory) {
        super(context);
        this.context = context;
        this.fridge = fridge;
        this.section = section;
        this.fridgeCategory = fridgeCategory;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // Dialog 실행시, 주변 화면 흐리게 하기
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);

        // Dialog 레이아웃 지정
        setContentView(R.layout.dialog_goods);

        setupDialog();
    }

    // 메서드 설명 : Dialog를 실행된 Activity에 따라 설정해주는 함수
    private void setupDialog() {
        // Dialog의 Widget을 id로 받아옴
        TextView okBtn = findViewById(R.id.btn_ok_goods_dialog);
        TextView cancelBtn = findViewById(R.id.btn_cancel_goods_dialog);
        name = findViewById(R.id.edit_name_goods_dialog);
        quantity = findViewById(R.id.edit_quantity_goods_dialog);
        expireDate = findViewById(R.id.edit_expire_date_goods_dialog);
        typeSpinner = findViewById(R.id.spinner_type_goods_dialog);
        fridgeSpinner = findViewById(R.id.spinner_fridge_goods_dialog);
        sectionSpinner = findViewById(R.id.spinner_section_goods_dialog);
        fridgeLayout = findViewById(R.id.fridge_layout_goods_dialog);
        dialogText = findViewById(R.id.dialogText);

        // 확인, 취소 버튼을 클릭 리스너와 연결
        okBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);

        // Dialog가 FreshnessActivity나 GoodsActivity에서 실행된 경우
        // (구역에서 재료추가할 경우, 냉장고 및 구역 선택이 안보이게 하기 위함)
        if (context.getClass() == FreshnessActivity.class || context.getClass() == GoodsActivity.class) {
            // 생성된 냉장고들 이름을 받아와서 ArrayList에 저장
            ArrayList<String> fridgeNameList = new ArrayList<>();
            Cursor cursor = db.query(
                    DBContract.FridgeEntry.TABLE_NAME,
                    new String[]{ DBContract.FridgeEntry.COLUMN_NAME },
                    null,
                    null,
                    null,
                    null,
                    null
            );

            while (cursor.moveToNext()) {
                fridgeNameList.add(cursor.getString(
                        cursor.getColumnIndex(DBContract.FridgeEntry.COLUMN_NAME)));
            }
            cursor.close();

            // GoodsActivity에서 생성된 경우는 재료 수정, 아니면 재료 추가
            if((context.getClass() == GoodsActivity.class)){
                name.setText(nameStr);
                quantity.setText(quantityStr);
                expireDate.setText(expireDateStr);
                dialogText.setText("재료 수정하기");
            }
            else{
                dialogText.setText("재료 추가하기");
            }

            // 냉장고 Spinner 데이터 설정
            final ArrayAdapter<String> fridgeAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, fridgeNameList);
            fridgeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            fridgeSpinner.setAdapter(fridgeAdapter);

            // 냉장고 Spinner에 Item이 선택됐을 경우, 그 냉장고의 구역 받아오기
            fridgeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    // 선택한 냉장고의 종류 받아오기
                    Cursor fridgeCursor = db.query(
                            DBContract.FridgeEntry.TABLE_NAME,
                            new String[]{ DBContract.FridgeEntry.COLUMN_CATEGORY },
                            DBContract.FridgeEntry.COLUMN_NAME + " = ? ",
                            new String[]{ fridgeSpinner.getSelectedItem().toString() },
                            null,
                            null,
                            null
                    );

                    fridgeCursor.moveToNext();
                    String category = fridgeCursor.getString(
                            fridgeCursor.getColumnIndex(DBContract.FridgeEntry.COLUMN_CATEGORY));

                    final ArrayAdapter<CharSequence> newTypeAdapter;
                    // 냉장고가 와인 냉장고일 경우, 재료 종류는 주류만 넣을 수 있게 만듦
                    if (category.equals("와인 냉장고")) {
                        newTypeAdapter = ArrayAdapter.createFromResource(context,
                                R.array.alchoholArray, android.R.layout.simple_spinner_item);
                    } else {
                        newTypeAdapter = ArrayAdapter.createFromResource(context,
                                R.array.ingredientArray, android.R.layout.simple_spinner_item);
                    }
                    newTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    typeSpinner.setAdapter(newTypeAdapter);
                    typeSpinner.setSelection(0);

                    // 선택한 냉장고의 구역 받아오기
                    ArrayList<String> sectionNameList = new ArrayList<>();
                    Cursor cursor = db.query(
                            DBContract.SectionEntry.TABLE_NAME,
                            new String[]{ DBContract.SectionEntry.COLUMN_NAME },
                            DBContract.SectionEntry.COLUMN_FRIDGE + " = ? ",
                            new String[]{ fridgeSpinner.getSelectedItem().toString() },
                            null,
                            null,
                            null
                    );

                    // 선택한 냉장고에 아무 구역이 없을 경우
                    if (cursor.getCount() == 0)
                        Toast.makeText(context, "구역이 없는 냉장고입니다.", Toast.LENGTH_SHORT).show();

                    while (cursor.moveToNext()) {
                        sectionNameList.add(cursor.getString(
                                cursor.getColumnIndex(DBContract.FridgeEntry.COLUMN_NAME)));
                    }
                    cursor.close();

                    // 구역 Spinner에 데이터 적용
                    final ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(context,
                            android.R.layout.simple_spinner_item, sectionNameList);
                    sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    sectionSpinner.setAdapter(sectionAdapter);
                    sectionSpinner.setSelection(0,false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });

            fridgeSpinner.setSelection(0,false);
        } else {
            // Dialog가 SectionActivity에서 생성된 경우
            dialogText.setText("재료 추가하기");
            final ArrayAdapter<CharSequence> typeAdapter;
            if (fridgeCategory.equals("와인 냉장고")) {
                typeAdapter = ArrayAdapter.createFromResource(context,
                        R.array.alchoholArray, android.R.layout.simple_spinner_item);
            } else {
                typeAdapter = ArrayAdapter.createFromResource(context,
                        R.array.ingredientArray, android.R.layout.simple_spinner_item);
            }
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeSpinner.setAdapter(typeAdapter);
            typeSpinner.setSelection(0);

            // SectionActivity에서 재료 추가는 냉장고 및 구역 Spinner는 보이지 않음
            fridgeLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.btn_ok_goods_dialog:
                String nameStr = name.getText().toString();
                String quantityStr = quantity.getText().toString();
                String expireDateStr = expireDate.getText().toString();

                // 냉장고가 선택되지 않았을 경우 (FreshnessActivity랑 GoodsActivity만)
                if (fridgeSpinner.getSelectedItemPosition() == -1 && fridgeLayout.getVisibility() == View.VISIBLE) {
                    Toast.makeText(context.getApplicationContext(),
                            "냉장고를 선택해주세요",Toast.LENGTH_SHORT).show();
                }
                // 구역이 선택되지 않았을 경우 (FreshnessActivity랑 GoodsActivity만)
                else if (sectionSpinner.getSelectedItemPosition() == -1 && fridgeLayout.getVisibility() == View.VISIBLE) {
                    Toast.makeText(context.getApplicationContext(),
                            "구역을 선택해주세요", Toast.LENGTH_SHORT).show();
                }
                // 이름이 공백일 경우
                else if (nameStr.trim().getBytes().length == 0) {
                    Toast.makeText(context.getApplicationContext(),
                            "이름을 입력해주세요",Toast.LENGTH_SHORT).show();
                }
                // 수량이 공백일 경우
                else if (quantityStr.trim().getBytes().length == 0) {
                    Toast.makeText(context.getApplicationContext(),
                            "수량을 입력해주세요", Toast.LENGTH_SHORT).show();
                }
                // 재료 종류가 선택되지 않았을 경우
                else if (typeSpinner.getSelectedItemPosition() == -1) {
                    Toast.makeText(context.getApplicationContext(),
                            "종류를 선택해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        // 날짜가 입력되지 않거나 형식이 잘못됐을 경우 Exception 발생
                        LocalDate expireDate = LocalDate.parse(expireDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

                        // 수량이 Int 범위를 초과한 경우, Exception 발생
                        int quantityInt = Integer.parseInt(quantityStr);

                        // 수량이 음수거나 터무니 없이 큰 경우
                        if(quantityInt>10000 || quantityInt<=0){
                            Toast.makeText(context.getApplicationContext(),
                                    "수량 형식이 잘못되었습니다", Toast.LENGTH_SHORT).show();
                        }

                        // 입력한 데이터 DB에 저장
                        ContentValues values = new ContentValues();
                        values.put(DBContract.GoodsEntry.COLUMN_NAME, nameStr);
                        values.put(DBContract.GoodsEntry.COLUMN_QUANTITY, quantityInt);
                        values.put(
                                DBContract.GoodsEntry.COLUMN_REGISTDATE,
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                        );
                        values.put(DBContract.GoodsEntry.COLUMN_EXPIREDATE, expireDateStr);
                        values.put(DBContract.GoodsEntry.COLUMN_TYPE, typeSpinner.getSelectedItem().toString());
                        values.put(DBContract.GoodsEntry.COLUMN_FRIDGE,
                                fridgeLayout.getVisibility() == View.GONE? fridge:fridgeSpinner.getSelectedItem().toString());
                        values.put(DBContract.GoodsEntry.COLUMN_SECTION,
                                fridgeLayout.getVisibility() == View.GONE? section:sectionSpinner.getSelectedItem().toString());

                        // FreshnessActivity와 SectionActivity는 재료 추가
                        if(context.getClass() != GoodsActivity.class) {
                            db.insert(DBContract.GoodsEntry.TABLE_NAME, null, values);
                            Toast.makeText(context.getApplicationContext(),
                                    "재료가 추가되었습니다",Toast.LENGTH_SHORT).show();
                        }
                        // GoodsActivity는 재료 갱신
                        else{
                            db.update(DBContract.GoodsEntry.TABLE_NAME, values, DBContract.GoodsEntry._ID + "=?",
                                    new String[]{String.valueOf(id)});
                            Toast.makeText(context.getApplicationContext(),
                                    "재료가 갱신되었습니다",Toast.LENGTH_SHORT).show();
                        }

                        // 각각이 실행된 Activity에 따라 화면에 갱신해줌
                        if (context.getClass() == FreshnessActivity.class) {
                            ((FreshnessActivity)context).setupFreshnessRecycler(); }
                        else if (context.getClass() == SectionActivity.class) {
                            ((SectionActivity)context).setupFreshnessRecycler(fridge, section);}
                        else if (context.getClass() == GoodsActivity.class){
                            ((GoodsActivity)context).getGoodsCursor(id);
                            ((GoodsActivity)context).setupGoodsView();}

                        dismiss();
                    }catch (NumberFormatException e) {
                        Toast.makeText(context.getApplicationContext(),
                                "수량 형식이 잘못되었습니다",Toast.LENGTH_SHORT).show();
                        return;
                    }catch (Exception e) {
                        Toast.makeText(context.getApplicationContext(),
                                "날짜 형식이 잘못되었습니다",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                break;
            case R.id.btn_cancel_goods_dialog:
                dismiss();
                break;
        }
    }
};