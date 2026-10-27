package ir.saba.insurancedisk

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

class EmployeeActivity : Activity() {
    private lateinit var store: MainActivity.Store
    private lateinit var root: LinearLayout
    private val fields = linkedMapOf<String, EditText>()
    private lateinit var gender: Spinner
    private lateinit var nationality: Spinner
    private var selectedId: Long? = null
    private var editing = false
    private val blue = Color.rgb(8,58,125)
    private val ink = Color.rgb(23,48,83)
    private val muted = Color.rgb(92,111,137)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(5,39,88)
        store=MainActivity.Store(this)
        render()
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density+0.5f).toInt()
    private fun shape(color:Int,radius:Int=14,stroke:Int=0)=GradientDrawable().apply {
        setColor(color);cornerRadius=dp(radius).toFloat()
        if(stroke!=0)setStroke(dp(1),stroke)
    }
    private fun label(t:String,size:Float=14f,color:Int=ink)=TextView(this).apply {
        text=t;textSize=size;setTextColor(color);gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    }
    private fun action(t:String,symbol:String,fn:()->Unit)=label("$symbol  $t",14f,blue).apply {
        gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD)
        background=shape(Color.WHITE,13,Color.rgb(213,225,241))
        elevation=dp(2).toFloat();setOnClickListener{fn()}
    }
    private fun toolbar(items:List<Triple<String,String,()->Unit>>) {
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL }
        items.forEach { (t,i,fn)->row.addView(action(t,i,fn),LinearLayout.LayoutParams(0,dp(56),1f).apply {
            setMargins(dp(3),dp(4),dp(3),dp(4))
        }) }
        root.addView(row)
    }
    private fun field(parent:LinearLayout,key:String,title:String,numeric:Boolean=false,readonly:Boolean=false):EditText {
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;background=shape(Color.WHITE,12,Color.rgb(220,230,242))
            setPadding(dp(10),dp(8),dp(10),dp(4))
        }
        box.addView(label(title,12f,muted))
        val e=EditText(this).apply {
            textSize=15f;setTextColor(ink);setSingleLine(true)
            if(numeric)inputType=InputType.TYPE_CLASS_NUMBER
            isEnabled=false
            if(readonly)setText("خودکار")
        }
        box.addView(e,LinearLayout.LayoutParams(-1,dp(46)))
        parent.addView(box,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(10) })
        fields[key]=e
        return e
    }
    private fun pair(a:String,at:String,b:String,bt:String,an:Boolean=false,bn:Boolean=false,readonly:Boolean=false) {
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL }
        val right=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val left=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        row.addView(right,LinearLayout.LayoutParams(0,-2,1f).apply { leftMargin=dp(5) })
        row.addView(left,LinearLayout.LayoutParams(0,-2,1f).apply { rightMargin=dp(5) })
        root.addView(row)
        field(right,a,at,an,readonly);field(left,b,bt,bn)
    }
    private fun spinnerBox(title:String,choices:List<String>):Spinner {
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;background=shape(Color.WHITE,12,Color.rgb(220,230,242))
            setPadding(dp(10),dp(8),dp(10),dp(8))
        }
        box.addView(label(title,12f,muted))
        val s=Spinner(this).apply {
            adapter=ArrayAdapter(this@EmployeeActivity,android.R.layout.simple_spinner_dropdown_item,choices)
            isEnabled=false
        }
        box.addView(s)
        root.addView(box,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(10) })
        return s
    }
    private fun render(search:String="") {
        fields.clear();selectedId=null;editing=false
        root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(15),dp(16),dp(15),dp(22));setBackgroundColor(Color.rgb(242,247,253))
        }
        val scroll=ScrollView(this).apply { isFillViewport=true;addView(root) }
        scroll.setOnApplyWindowInsetsListener { view,insets ->
            val top=if(android.os.Build.VERSION.SDK_INT>=30) insets.getInsets(android.view.WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom=if(android.os.Build.VERSION.SDK_INT>=30) insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(0,top,0,bottom+dp(12));insets
        }
        setContentView(scroll)
        val header=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(6,45,104),Color.rgb(10,109,197))).apply { cornerRadius=dp(20).toFloat() }
            setPadding(dp(18),dp(18),dp(18),dp(19))
        }
        header.addView(label("♟  مشخصات کارکنان",23f,Color.WHITE).apply {setTypeface(null,Typeface.BOLD)})
        header.addView(label("صبا  |  مدیریت مستقل اطلاعات پرسنل",12f,Color.rgb(222,235,255)))
        root.addView(header,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(14)})
        toolbar(listOf(Triple("جدید","＋",{clear(true)}),Triple("ویرایش","✎",{if(selectedId!=null)enable(true) else toast("ابتدا کارمند را انتخاب کنید")}),Triple("حذف","×",{deleteSelected()})))
        toolbar(listOf(Triple("ثبت","✓",{save()}),Triple("لغو","↶",{clear(false)}),Triple("خروج","⌂",{finish()})))
        val searchBox=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER_VERTICAL
            background=shape(Color.WHITE,14,Color.rgb(207,222,241));setPadding(dp(12),dp(5),dp(12),dp(5))
        }
        val searchInput=EditText(this).apply {
            hint="جست‌وجوی نام، کد ملی، شماره بیمه یا شغل"
            setSingleLine(true);textSize=13f;setTextColor(ink);setText(search);setSelection(search.length)
        }
        searchBox.addView(searchInput,LinearLayout.LayoutParams(0,dp(49),1f))
        searchBox.addView(action("جست‌وجو","⌕"){render(searchInput.text.toString().trim())},LinearLayout.LayoutParams(dp(110),dp(43)))
        root.addView(searchBox,LinearLayout.LayoutParams(-1,-2).apply {topMargin=dp(12);bottomMargin=dp(18)})
        root.addView(label("فهرست کارکنان",17f,blue).apply {setTypeface(null,Typeface.BOLD);setPadding(0,0,0,dp(9))})
        val table=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;background=shape(Color.WHITE,13,Color.rgb(209,224,241));setPadding(dp(4),dp(4),dp(4),dp(4))
        }
        fun tableRow(cells:List<String>,header:Boolean,id:Long?=null,index:Int=0) {
            val row=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER_VERTICAL
                background=shape(if(header)blue else if(index%2==0)Color.WHITE else Color.rgb(235,244,255),6)
                if(id!=null)setOnClickListener{load(id)}
            }
            cells.forEachIndexed { i,v ->
                row.addView(label(v,if(header)11f else 11.5f,if(header)Color.WHITE else ink).apply {
                    gravity=Gravity.CENTER;setPadding(dp(2),dp(12),dp(2),dp(12))
                    if(header)setTypeface(null,Typeface.BOLD)
                },LinearLayout.LayoutParams(0,-2,if(i==2)1.4f else 1f))
            }
            table.addView(row)
        }
        tableRow(listOf("شماره بیمه","پرسنلی","نام خانوادگی","نام","شغل","کد ملی"),true)
        val arg="%$search%";var count=0
        store.readableDatabase.rawQuery(
            "SELECT id,insurance_no,personnel_no,last_name,first_name,job_title,national_id FROM employees WHERE insurance_no LIKE ? OR personnel_no LIKE ? OR last_name LIKE ? OR first_name LIKE ? OR job_title LIKE ? OR national_id LIKE ? ORDER BY id DESC",
            arrayOf(arg,arg,arg,arg,arg,arg)
        ).use { c->while(c.moveToNext()) {
            tableRow((1..6).map{c.getString(it) ?: ""},false,c.getLong(0),count);count++
        } }
        if(count==0)table.addView(label("کارمندی برای نمایش وجود ندارد.",13f,muted).apply {gravity=Gravity.CENTER;setPadding(dp(8),dp(16),dp(8),dp(16))})
        root.addView(table,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(18)})
        root.addView(label("اطلاعات پرسنل",17f,blue).apply {setTypeface(null,Typeface.BOLD);setPadding(0,0,0,dp(10))})
        pair("personnel_no","شماره پرسنلی","insurance_no","شماره بیمه",false,true,true)
        pair("first_name","نام","last_name","نام خانوادگی")
        pair("father_name","نام پدر","national_id","کد ملی",false,true)
        pair("birth_cert_no","شماره شناسنامه","birth_date","تاریخ تولد (مثلاً ۱۳۷۰/۰۱/۰۱)")
        field(root,"issue_city","شهر محل صدور")
        gender=spinnerBox("جنسیت",listOf("انتخاب کنید","مرد","زن"))
        nationality=spinnerBox("ملیت",listOf("انتخاب کنید","ایرانی","غیرایرانی"))
        pair("job_title","عنوان شغل","job_code","کد شغل",false,true)
    }
    private fun enable(value:Boolean) {
        editing=value
        fields.forEach { (key,e)->e.isEnabled=value && key!="personnel_no" }
        gender.isEnabled=value;nationality.isEnabled=value
    }
    private fun clear(start:Boolean) {
        selectedId=null
        fields.forEach { (key,e)->e.setText(if(key=="personnel_no")"خودکار" else "") }
        gender.setSelection(0);nationality.setSelection(0);enable(start)
    }
    private fun load(id:Long) {
        val keys=fields.keys.toList()
        val columns=keys.joinToString(",")
        store.readableDatabase.rawQuery("SELECT $columns,gender,nationality FROM employees WHERE id=?",arrayOf(id.toString())).use { c->
            if(c.moveToFirst()) {
                selectedId=id
                keys.forEachIndexed { i,key->fields[key]?.setText(c.getString(i) ?: "") }
                gender.setSelection(listOf("انتخاب کنید","مرد","زن").indexOf(c.getString(keys.size)).coerceAtLeast(0))
                nationality.setSelection(listOf("انتخاب کنید","ایرانی","غیرایرانی").indexOf(c.getString(keys.size+1)).coerceAtLeast(0))
                enable(false)
            }
        }
    }
    private fun validNationalId(s:String):Boolean {
        if(!s.matches(Regex("[0-9]{10}")) || s.toSet().size==1)return false
        val sum=(0..8).sumOf { (s[it]-'0')*(10-it) }
        val remainder=sum%11
        val check=s[9]-'0'
        return check==if(remainder<2)remainder else 11-remainder
    }
    private fun save() {
        if(!editing){toast("ابتدا جدید یا ویرایش را انتخاب کنید");return}
        val values=fields.mapValues{it.value.text.toString().trim()}.toMutableMap()
        val raw=values["insurance_no"] ?: ""
        if(!raw.matches(Regex("[0-9]{1,10}"))){toast("شماره بیمه باید حداکثر ۱۰ رقم و فقط عدد باشد");return}
        values["insurance_no"]=raw.padStart(10,'0')
        if(fields.keys.filter{it!="personnel_no"}.any{values[it].isNullOrBlank()} || gender.selectedItemPosition==0 || nationality.selectedItemPosition==0) {
            toast("تمام اطلاعات پرسنل الزامی است");return
        }
        if(!validNationalId(values["national_id"] ?: "")){toast("کد ملی معتبر نیست");return}
        val birth=values["birth_date"] ?: ""
        if(!birth.matches(Regex("[0-9۰-۹]{4}/[0-9۰-۹]{2}/[0-9۰-۹]{2}"))){toast("تاریخ تولد را به‌شکل ۱۳۷۰/۰۱/۰۱ وارد کنید");return}
        if(!(values["job_code"] ?: "").matches(Regex("[0-9]+"))){toast("کد شغل باید عددی باشد");return}
        val data=ContentValues().apply {
            values.forEach { (key,v)->if(key!="personnel_no")put(key,v) }
            put("gender",gender.selectedItem.toString());put("nationality",nationality.selectedItem.toString())
        }
        try {
            val db=store.writableDatabase
            val id=selectedId
            if(id==null) {
                db.beginTransaction()
                try {
                    val newId=db.insertOrThrow("employees",null,data)
                    val personnel=newId.toString().padStart(6,'0')
                    db.execSQL("UPDATE employees SET personnel_no=? WHERE id=?",arrayOf(personnel,newId))
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
            } else db.update("employees",data,"id=?",arrayOf(id.toString()))
            toast("اطلاعات پرسنل ذخیره شد");render()
        } catch(e:Exception){toast("خطا در ذخیره اطلاعات")}
    }
    private fun deleteSelected() {
        val id=selectedId ?: run {toast("ابتدا کارمند را انتخاب کنید");return}
        AlertDialog.Builder(this).setTitle("حذف کارمند").setMessage("اطلاعات کارمند انتخاب‌شده حذف شود؟")
            .setPositiveButton("حذف"){_,_->store.writableDatabase.delete("employees","id=?",arrayOf(id.toString()));render()}
            .setNegativeButton("انصراف",null).show()
    }
    private fun toast(message:String)=Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}
