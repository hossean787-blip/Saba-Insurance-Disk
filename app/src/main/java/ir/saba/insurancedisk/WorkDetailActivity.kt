package ir.saba.insurancedisk

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.os.Bundle
import android.view.View
import android.widget.*

class WorkDetailActivity:Activity() {
    private val store by lazy {MainActivity.Store(this)}
    private lateinit var root:LinearLayout
    private lateinit var listPicker:Spinner
    private lateinit var employeePicker:Spinner
    private lateinit var rows:LinearLayout
    private var lists=listOf<Pair<Long,String>>()
    private var employees=listOf<Pair<Long,String>>()
    private fun text(s:String)=TextView(this).apply{text=s;textSize=17f;setPadding(4,12,4,12)}
    private fun button(s:String,run:()->Unit)=Button(this).apply{text=s;isAllCaps=false;setOnClickListener{run()}}
    private fun alert(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);draw()}
    private fun draw(){
        root=LinearLayout(this).apply{orientation=1;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(24,24,24,32)}
        setContentView(ScrollView(this).apply{addView(root)})
        root.addView(text("ریزکارکرد کارکنان").apply{textSize=24f})
        val l=mutableListOf<Pair<Long,String>>()
        store.readableDatabase.rawQuery("SELECT l.id,w.name,l.year,l.month FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id ORDER BY l.year DESC,l.month DESC",null).use{c->while(c.moveToNext())l.add(c.getLong(0) to "${c.getString(1)} — ${c.getInt(2)}/${c.getInt(3)}")}
        lists=l
        if(lists.isEmpty()){root.addView(text("ابتدا در بخش لیست بیمه، یک لیست ماهانه ایجاد کنید."));root.addView(button("خروج"){finish()});return}
        listPicker=Spinner(this).apply{adapter=ArrayAdapter(this@WorkDetailActivity,android.R.layout.simple_spinner_dropdown_item,lists.map{it.second})}
        root.addView(text("انتخاب لیست بیمه"));root.addView(listPicker)
        val e=mutableListOf<Pair<Long,String>>()
        store.readableDatabase.rawQuery("SELECT id,first_name,last_name,insurance_no FROM employees ORDER BY last_name,first_name",null).use{c->while(c.moveToNext())e.add(c.getLong(0) to "${c.getString(1)} ${c.getString(2)} — ${c.getString(3)}")}
        employees=e
        if(e.isNotEmpty()){
            employeePicker=Spinner(this).apply{adapter=ArrayAdapter(this@WorkDetailActivity,android.R.layout.simple_spinner_dropdown_item,employees.map{it.second})}
            root.addView(text("انتخاب کارمند"));root.addView(employeePicker)
            root.addView(button("افزودن / ویرایش کارکرد"){edit()})
        }else root.addView(text("ابتدا در بخش کارکنان، کارمند ثبت کنید."))
        rows=LinearLayout(this).apply{orientation=1};root.addView(rows)
        listPicker.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent:android.widget.AdapterView<*>?){}
            override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:View?,position:Int,id:Long){refresh()}
        }
        root.addView(button("خروج"){finish()})
        refresh()
    }
    private fun selectedList()=lists[listPicker.selectedItemPosition.coerceAtLeast(0)].first
    private fun edit(){
        val listId=selectedList()
        val empId=employees[employeePicker.selectedItemPosition].first
        val box=LinearLayout(this).apply{orientation=1;setPadding(20,12,20,12)}
        fun field(h:String)=EditText(this).apply{hint=h;inputType=2;box.addView(this)}
        val days=field("روزهای کارکرد")
        val wage=field("دستمزد روزانه (ریال)")
        val benefits=field("مزایای مشمول ماهانه (ریال)")
        benefits.setText("0")
        store.readableDatabase.rawQuery("SELECT days,daily_wage,benefits FROM insurance_list_items WHERE list_id=? AND employee_id=?",arrayOf(listId.toString(),empId.toString())).use{c->if(c.moveToFirst()){days.setText(c.getInt(0).toString());wage.setText(c.getLong(1).toString());benefits.setText(c.getLong(2).toString())}}
        AlertDialog.Builder(this).setTitle("ثبت ریزکارکرد").setView(box).setPositiveButton("ثبت"){_,_->
            val d=days.text.toString().toIntOrNull();val w=wage.text.toString().toLongOrNull();val b=benefits.text.toString().toLongOrNull()
            if(d==null||d !in 0..31||w==null||w<0||b==null||b<0|| (d>0&&w==0L)){alert("روز و مبالغ معتبر نیستند")}else{
                try{
                    Math.addExact(Math.multiplyExact(d.toLong(),w),b)
                    val values=ContentValues().apply{put("list_id",listId);put("employee_id",empId);put("days",d);put("daily_wage",w);put("benefits",b)}
                    store.writableDatabase.insertWithOnConflict("insurance_list_items",null,values,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                    refresh()
                }catch(ex:ArithmeticException){alert("مبلغ از محدوده مجاز بیشتر است")}
            }
        }.setNegativeButton("لغو",null).show()
    }
    private fun refresh(){
        if(!::rows.isInitialized)return
        rows.removeAllViews()
        var count=0
        store.readableDatabase.rawQuery("SELECT i.id,e.first_name,e.last_name,i.days,i.daily_wage,i.benefits FROM insurance_list_items i JOIN employees e ON e.id=i.employee_id WHERE i.list_id=? ORDER BY e.last_name",arrayOf(selectedList().toString())).use{c->
            while(c.moveToNext()){
                count++
                val id=c.getLong(0)
                val title="${c.getString(1)} ${c.getString(2)} | ${c.getInt(3)} روز | ${c.getLong(4)} ریال روزانه | ${c.getLong(5)} ریال مزایا"
                rows.addView(text(title))
                rows.addView(button("حذف این ردیف"){AlertDialog.Builder(this).setMessage("ریزکارکرد حذف شود؟").setPositiveButton("حذف"){_,_->store.writableDatabase.delete("insurance_list_items","id=?",arrayOf(id.toString()));refresh()}.setNegativeButton("لغو",null).show()})
            }
        }
        rows.addView(text("تعداد کارکنان این لیست: $count"))
    }
}
