package ir.saba.insurancedisk

import android.app.Activity
import android.content.ContentValues
import android.os.Bundle
import android.widget.*
import android.view.View

class InsuranceListActivity : Activity() {
    private lateinit var db: MainActivity.Store
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db=MainActivity.Store(this)
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(24,24,24,24) }
        setContentView(ScrollView(this).apply { addView(layout) })
        val workshops=mutableListOf<Pair<Long,String>>()
        db.readableDatabase.rawQuery("SELECT id,name FROM workshops ORDER BY name",null).use { c->
            while(c.moveToNext()) workshops.add(c.getLong(0) to c.getString(1))
        }
        val heading=TextView(this).apply { text="لیست بیمه ماهانه";textSize=24f }
        layout.addView(heading)
        if(workshops.isEmpty()) {layout.addView(TextView(this).apply{text="ابتدا کارگاه ثبت کنید"});return}
        val workshop=Spinner(this).apply {adapter=ArrayAdapter(this@InsuranceListActivity,android.R.layout.simple_spinner_dropdown_item,workshops.map{it.second})}
        layout.addView(workshop)
        val year=EditText(this).apply {hint="سال شمسی";inputType=2}
        layout.addView(year)
        val month=Spinner(this).apply {adapter=ArrayAdapter(this@InsuranceListActivity,android.R.layout.simple_spinner_dropdown_item,(1..12).map{"ماه $it"})}
        layout.addView(month)
        val result=TextView(this).apply {textSize=17f}
        layout.addView(Button(this).apply {text="ایجاد یا بازیابی لیست";setOnClickListener {
            val y=year.text.toString().toIntOrNull()
            if(y==null || y !in 1300..1600) {Toast.makeText(this@InsuranceListActivity,"سال معتبر نیست",Toast.LENGTH_SHORT).show();return@setOnClickListener}
            val wid=workshops[workshop.selectedItemPosition].first
            val m=month.selectedItemPosition+1
            val data=ContentValues().apply {put("workshop_id",wid);put("year",y);put("month",m)}
            val database=db.writableDatabase
            database.insertWithOnConflict("insurance_lists",null,data,android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
            database.rawQuery("SELECT id FROM insurance_lists WHERE workshop_id=? AND year=? AND month=?",arrayOf(wid.toString(),y.toString(),m.toString())).use { c ->
                if(c.moveToFirst())result.text="لیست شماره ${c.getLong(0)} آماده ثبت ردیف است"
            }
        }})
        layout.addView(result)
    }
}
