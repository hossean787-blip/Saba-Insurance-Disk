package ir.saba.insurancedisk

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.os.Bundle
import android.view.View
import android.widget.*
import java.lang.Math

class InsuranceListActivity : Activity() {
    private val store by lazy { MainActivity.Store(this) }
    private lateinit var root:LinearLayout
    private lateinit var listArea:LinearLayout
    private lateinit var detailArea:LinearLayout
    private var workshops= listOf<Triple<Long,String,String>>()
    private var selectedId:Long?=null
    private fun twoDigits(value:Int)=value.toString().padStart(2,'0')
    private fun yearTwoDigits(value:Int)=twoDigits(value%100)
    private fun label(s:String)=TextView(this).apply {text=s;textSize=16f;setPadding(6,12,6,12)}
    private fun button(s:String,action:()->Unit)=Button(this).apply{text=s;isAllCaps=false;setOnClickListener{action()}}
    private fun message(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    override fun onCreate(savedInstanceState:Bundle?) {super.onCreate(savedInstanceState);draw()}
    private fun draw() {
        root=LinearLayout(this).apply{orientation=1;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(24,24,24,32)}
        setContentView(ScrollView(this).apply{addView(root)})
        root.addView(label("لیست بیمه ماهانه").apply{textSize=24f})
        val actions=LinearLayout(this).apply{orientation=1}
        root.addView(actions)
        val first=LinearLayout(this);val second=LinearLayout(this)
        actions.addView(first);actions.addView(second)
        fun add(parent:LinearLayout,s:String,run:()->Unit){parent.addView(button(s,run),LinearLayout.LayoutParams(0,-2,1f))}
        add(first,"جدید"){editor(null)}
        add(first,"ویرایش"){selectedId?.let{editor(it)}?:message("لیستی انتخاب نشده است")}
        add(first,"حذف"){selectedId?.let{deleteList(it)}?:message("لیستی انتخاب نشده است")}
        add(second,"ثبت"){editor(selectedId)}
        add(second,"لغو"){selectedId=null;refresh()}
        add(second,"خروج"){finish()}
        val search=EditText(this).apply{hint="جست‌وجوی کارگاه، سال یا ماه"}
        root.addView(search)
        root.addView(button("جست‌وجو"){refresh(search.text.toString().trim())})
        detailArea=LinearLayout(this).apply{orientation=1};root.addView(detailArea)
        root.addView(label("فهرست لیست‌های ماهانه").apply{textSize=19f})
        listArea=LinearLayout(this).apply{orientation=1};root.addView(listArea)
        refresh()
    }
    private fun refresh(query:String="") {
        listArea.removeAllViews()
        detailArea.removeAllViews()
        val sql="SELECT l.id,w.name,l.year,l.month,l.list_number,COUNT(i.id),COALESCE(SUM(i.days),0),COALESCE(SUM(i.days*i.daily_wage+i.benefits),0) FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id LEFT JOIN insurance_list_items i ON i.list_id=l.id GROUP BY l.id,w.name,l.year,l.month ORDER BY l.year DESC,l.month DESC,l.id DESC"
        store.readableDatabase.rawQuery(sql,null).use{c->
            while(c.moveToNext()){
                val id=c.getLong(0);val name=c.getString(1);val y=c.getInt(2);val m=c.getInt(3)
                val display="شماره ${twoDigits(c.getInt(4))} | $name | ${yearTwoDigits(y)}/${twoDigits(m)} | ${c.getInt(5)} نفر | ${c.getLong(6)} روز | ${c.getLong(7)} ریال"
                if(query.isNotEmpty()&&!display.contains(query))continue
                listArea.addView(button((if(selectedId==id)"✓ " else "")+display){selectedId=id;refresh(query)})
            }
        }
        selectedId?.let { id->
            showDetails(id)
        }
    }
    private fun showDetails(id:Long) {
        store.readableDatabase.rawQuery("SELECT w.code,w.contract_row,w.name,w.employer,w.address,l.year,l.month,l.list_number FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id WHERE l.id=?",arrayOf(id.toString())).use { c ->
            if(!c.moveToFirst())return@use
            detailArea.addView(label("مشخصات لیست").apply{textSize=20f})
            detailArea.addView(label("شماره لیست: ${twoDigits(c.getInt(7))}"))
            listOf("کد کارگاه" to c.getString(0),"ردیف پیمان" to c.getString(1),"نام کارگاه" to c.getString(2),"نام کارفرما" to c.getString(3),"آدرس" to c.getString(4),"سال" to yearTwoDigits(c.getInt(5)),"ماه" to twoDigits(c.getInt(6))).forEach { (k,v) ->
                detailArea.addView(label("$k: $v"))
            }
        }
        var people=0L;var days=0L;var daily=0L;var monthly=0L;var benefits=0L
        try {
            store.readableDatabase.rawQuery("SELECT days,daily_wage,benefits FROM insurance_list_items WHERE list_id=?",arrayOf(id.toString())).use { c ->
                while(c.moveToNext()){
                    people=Math.addExact(people,1L)
                    days=Math.addExact(days,c.getLong(0))
                    daily=Math.addExact(daily,c.getLong(1))
                    monthly=Math.addExact(monthly,Math.multiplyExact(c.getLong(0),c.getLong(1)))
                    benefits=Math.addExact(benefits,c.getLong(2))
                }
            }
            val total=Math.addExact(monthly,benefits)
            detailArea.addView(label("خلاصه محاسبات خودکار — فقط خواندنی").apply{textSize=19f})
            listOf("تعداد نفرات" to people.toString(),"جمع روزهای کارکرد" to days.toString(),"جمع دستمزد روزانه (ریال)" to daily.toString(),"جمع دستمزد ماهانه (ریال)" to monthly.toString(),"جمع مزایای ماهانه مشمول (ریال)" to benefits.toString(),"جمع دستمزد و مزایای ثبت‌شده (ریال)" to total.toString()).forEach { (k,v) ->
                detailArea.addView(label("$k: $v"))
            }
            detailArea.addView(label("جمع کل دستمزد و مزایای ماهانه").apply{textSize=19f})
            detailArea.addView(label("مشمول: ${String.format("%,d",total)} ریال"))
            detailArea.addView(label("مشمول و غیرمشمول: محاسبه‌نشده"))
            detailArea.addView(label("حق بیمه").apply{textSize=19f})
            listOf("پایه سنواتی","حق تأهل","سهم بیمه‌شده","سهم کارفرما","بیمه بیکاری","حق بیمه مشاغل سخت و زیان‌آور ۴٪").forEach { title ->
                detailArea.addView(label("$title: محاسبه‌نشده"))
            }
            detailArea.addView(label("مبالغ فوق تا زمان ثبت داده‌های تفکیکی و تأیید قواعد دوره، محاسبه نمی‌شوند."))
        } catch(e:ArithmeticException) {
            detailArea.addView(label("خطا: سرریز محاسبات؛ مقادیر ریزکارکرد را بررسی کنید."))
        }
    }
    private fun editor(id:Long?) {
        val ws=mutableListOf<Triple<Long,String,String>>()
        store.readableDatabase.rawQuery("SELECT id,name,code FROM workshops ORDER BY name",null).use{c->while(c.moveToNext())ws.add(Triple(c.getLong(0),c.getString(1),c.getString(2)))}
        if(ws.isEmpty()){message("ابتدا کارگاه ثبت کنید");return}
        val panel=LinearLayout(this).apply{orientation=1;setPadding(20,12,20,12)}
        val picker=Spinner(this).apply{adapter=ArrayAdapter(this@InsuranceListActivity,android.R.layout.simple_spinner_dropdown_item,ws.map{"${it.second} — ${it.third}"})}
        val y=EditText(this).apply{hint="سال دو رقمی (مثلاً ۰۵)";inputType=2;filters=arrayOf(android.text.InputFilter.LengthFilter(2))}
        val m=Spinner(this).apply{adapter=ArrayAdapter(this@InsuranceListActivity,android.R.layout.simple_spinner_dropdown_item,(1..12).map{"ماه $it"})}
        if(id!=null)store.readableDatabase.rawQuery("SELECT workshop_id,year,month FROM insurance_lists WHERE id=?",arrayOf(id.toString())).use{c->
            if(c.moveToFirst()){picker.setSelection(ws.indexOfFirst{it.first==c.getLong(0)}.coerceAtLeast(0));y.setText(yearTwoDigits(c.getInt(1)));m.setSelection(c.getInt(2)-1)}
        }
        panel.addView(label("کارگاه"));panel.addView(picker);panel.addView(y);panel.addView(m)
        AlertDialog.Builder(this).setTitle(if(id==null)"لیست جدید" else "ویرایش لیست").setView(panel).setPositiveButton("ثبت"){_,_->
            val shortYear=y.text.toString().toIntOrNull()
            val year=shortYear?.plus(1400)
            if(shortYear==null||shortYear !in 0..99){message("سال معتبر نیست")}else{
                val v=ContentValues().apply{put("workshop_id",ws[picker.selectedItemPosition].first);put("year",year);put("month",m.selectedItemPosition+1)}
                try {
                    val db=store.writableDatabase
                    if(id==null){
                        val wid=ws[picker.selectedItemPosition].first
                        val next=db.rawQuery("SELECT COALESCE(MAX(list_number),0)+1 FROM insurance_lists WHERE workshop_id=? AND year=?",arrayOf(wid.toString(),year.toString())).use{c->c.moveToFirst();c.getInt(0)}
                        if(next>99){message("ظرفیت شماره دو رقمی تکمیل شده است");return@setPositiveButton}
                        v.put("list_number",next)
                        selectedId=db.insertOrThrow("insurance_lists",null,v)
                    }
                    else {db.update("insurance_lists",v,"id=?",arrayOf(id.toString()));selectedId=id}
                    refresh()
                }catch(e:Exception){message("این دوره برای کارگاه تکراری است یا ثبت ناموفق بود")}
            }
        }.setNegativeButton("لغو",null).show()
    }
    private fun deleteList(id:Long){
        AlertDialog.Builder(this).setTitle("حذف لیست").setMessage("لیست و تمام ریزکارکردهای آن حذف شوند؟").setPositiveButton("حذف"){_,_->
            val db=store.writableDatabase
            db.beginTransaction()
            try{db.delete("insurance_list_items","list_id=?",arrayOf(id.toString()));db.delete("insurance_lists","id=?",arrayOf(id.toString()));db.setTransactionSuccessful();selectedId=null}
            finally{db.endTransaction()}
            refresh()
        }.setNegativeButton("لغو",null).show()
    }
    private fun showSummary(id:Long){
        var people=0L;var days=0L;var daily=0L;var monthly=0L;var benefits=0L
        try{
            store.readableDatabase.rawQuery("SELECT days,daily_wage,benefits FROM insurance_list_items WHERE list_id=?",arrayOf(id.toString())).use{c->
                while(c.moveToNext()){
                    people++;days=Math.addExact(days,c.getLong(0))
                    daily=Math.addExact(daily,c.getLong(1))
                    monthly=Math.addExact(monthly,Math.multiplyExact(c.getLong(0),c.getLong(1)))
                    benefits=Math.addExact(benefits,c.getLong(2))
                }
            }
            val total=Math.addExact(monthly,benefits)
            AlertDialog.Builder(this).setTitle("خلاصه محاسبات خودکار").setMessage("تعداد نفرات: $people\nجمع روزهای کارکرد: $days\nجمع دستمزد روزانه: $daily ریال\nجمع دستمزد ماهانه: $monthly ریال\nجمع مزایای ثبت‌شده: $benefits ریال\nجمع دستمزد و مزایا: $total ریال\n\nسهم بیمه‌شده، کارفرما و بیکاری پس از پیاده‌سازی قواعد معتبر دوره محاسبه می‌شود.").setPositiveButton("بستن",null).show()
        }catch(e:ArithmeticException){message("مقادیر از محدوده محاسبات مجاز فراتر رفته‌اند")}
    }
}
