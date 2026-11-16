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
    private lateinit var workshopPicker: Spinner
    private var workshops=listOf<Pair<Long,String>>()
    private var selectedId: Long? = null
    private var editing = false
    private val blue = SuraUi.primary
    private val ink = SuraUi.title
    private val muted = SuraUi.muted

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
        background=shape(Color.WHITE,13,SuraUi.line)
        elevation=dp(2).toFloat()
        SuraUi.touch(this@EmployeeActivity,this)
        setOnClickListener{fn()}
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
            orientation=LinearLayout.VERTICAL;background=shape(Color.WHITE,12,SuraUi.line)
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
        if(SuraUi.isCompact(this)) {
            row.orientation=LinearLayout.VERTICAL
            row.addView(right,LinearLayout.LayoutParams(-1,-2))
            row.addView(left,LinearLayout.LayoutParams(-1,-2))
        }else{
            row.addView(right,LinearLayout.LayoutParams(0,-2,1f).apply{leftMargin=dp(5)})
            row.addView(left,LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(5)})
        }
        root.addView(row)
        field(right,a,at,an,readonly);field(left,b,bt,bn)
    }
    private fun spinnerBox(title:String,choices:List<String>):Spinner {
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;background=shape(Color.WHITE,12,SuraUi.line)
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
        workshops=mutableListOf<Pair<Long,String>>().also { found ->
            store.readableDatabase.rawQuery("SELECT id,name,code FROM workshops ORDER BY name,code",null).use { c ->
                while(c.moveToNext()) found.add(c.getLong(0) to "${c.getString(1)} — ${c.getString(2)}")
            }
        }
        root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(15),dp(16),dp(15),dp(22));setBackgroundColor(SuraUi.canvas)
        }
        val scroll=ScrollView(this).apply { isFillViewport=true;addView(root) }
        scroll.setOnApplyWindowInsetsListener { view,insets ->
            val top=if(android.os.Build.VERSION.SDK_INT>=30) insets.getInsets(android.view.WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom=if(android.os.Build.VERSION.SDK_INT>=30) insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(0,top,0,bottom+dp(12));insets
        }
        setContentView(SuraUi.mount(this,scroll,"employees"))
        root.addView(SuraUi.banner(this,"مدیریت کارکنان","پرونده پرسنلی و اتصال به کارگاه","users"),
            LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})
        toolbar(listOf(Triple("جدید","＋",{clear(true)}),Triple("ویرایش","✎",{if(selectedId!=null)enable(true) else toast("ابتدا کارمند را انتخاب کنید")}),Triple("حذف","×",{deleteSelected()})))
        toolbar(listOf(Triple("ثبت","✓",{save()}),Triple("لغو","↶",{clear(false)}),Triple("خروج","⌂",{finish()})))
        val searchBox=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER_VERTICAL
            background=shape(Color.WHITE,14,SuraUi.line);setPadding(dp(12),dp(5),dp(12),dp(5))
        }
        val searchInput=EditText(this).apply {
            hint="جست‌وجوی نام، کد ملی، بیمه، شغل یا کارگاه"
            setSingleLine(true);textSize=13f;setTextColor(ink);setText(search);setSelection(search.length)
        }
        searchBox.addView(searchInput,LinearLayout.LayoutParams(0,dp(49),1f))
        searchBox.addView(action("جست‌وجو","⌕"){render(searchInput.text.toString().trim())},LinearLayout.LayoutParams(dp(110),dp(43)))
        root.addView(searchBox,LinearLayout.LayoutParams(-1,-2).apply {topMargin=dp(12);bottomMargin=dp(18)})
        root.addView(label("فهرست کارکنان",17f,blue).apply {setTypeface(null,Typeface.BOLD);setPadding(0,0,0,dp(9))})
        val table=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;background=shape(Color.WHITE,13,SuraUi.line);setPadding(dp(4),dp(4),dp(4),dp(4))
        }
        val arg="%$search%"
        var count=0
        var unassigned=0
        store.readableDatabase.rawQuery(
            """SELECT e.id,e.insurance_no,e.personnel_no,e.last_name,e.first_name,e.job_title,e.national_id,
                      COALESCE(w.name,'بدون کارگاه') FROM employees e
               LEFT JOIN workshops w ON w.id=e.workshop_id
               WHERE e.insurance_no LIKE ? OR e.personnel_no LIKE ? OR e.last_name LIKE ?
                  OR e.first_name LIKE ? OR e.job_title LIKE ? OR e.national_id LIKE ?
                  OR w.name LIKE ? ORDER BY e.id DESC""",
            arrayOf(arg,arg,arg,arg,arg,arg,arg)
        ).use { c ->
            while(c.moveToNext()) {
                val id=c.getLong(0)
                val assigned=c.getString(7)
                if(assigned=="بدون کارگاه")unassigned++
                val card=LinearLayout(this).apply {
                    orientation=LinearLayout.VERTICAL
                    background=shape(if(count%2==0)Color.WHITE else Color.rgb(235,244,255),10,Color.rgb(214,228,245))
                    setPadding(dp(12),dp(11),dp(12),dp(11))
                    SuraUi.touch(this@EmployeeActivity,this,10)
                    contentDescription="انتخاب کارمند: ${c.getString(4)} ${c.getString(3)}"
                    setOnClickListener { load(id) }
                }
                card.addView(label("${c.getString(4)} ${c.getString(3)}  •  ${c.getString(2)}",15f,blue).apply {
                    setTypeface(null,Typeface.BOLD)
                })
                card.addView(label("کارگاه: $assigned  |  بیمه: ${c.getString(1)}",13f,if(assigned=="بدون کارگاه")Color.rgb(166,72,23) else muted))
                card.addView(label("کد ملی: ${c.getString(6)}  |  شغل: ${c.getString(5)}",12f,muted))
                table.addView(card,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(5)})
                count++
            }
        }
        if(count==0)table.addView(label("کارمندی برای نمایش وجود ندارد.",13f,muted).apply {
            gravity=Gravity.CENTER;setPadding(dp(8),dp(16),dp(8),dp(16))
        })
        if(unassigned>0)table.addView(label("$unassigned نفر بدون کارگاه هستند؛ برای اصلاح، رکورد را انتخاب و ویرایش کنید.",13f,Color.rgb(166,72,23)))
        root.addView(table,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(18)})
        root.addView(label("اطلاعات پرسنل",17f,blue).apply {setTypeface(null,Typeface.BOLD);setPadding(0,0,0,dp(10))})
        workshopPicker=spinnerBox("کارگاه محل اشتغال *",listOf("انتخاب کارگاه") + workshops.map { it.second })
        if(workshops.isEmpty()) root.addView(label("برای ثبت کارمند ابتدا کارگاه ایجاد کنید.",13f,Color.rgb(166,72,23)))
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
        gender.isEnabled=value;nationality.isEnabled=value;workshopPicker.isEnabled=value
    }
    private fun clear(start:Boolean) {
        selectedId=null
        fields.forEach { (key,e)->e.setText(if(key=="personnel_no")"خودکار" else "") }
        gender.setSelection(0);nationality.setSelection(0);workshopPicker.setSelection(0);enable(start)
    }
    private fun load(id:Long) {
        val keys=fields.keys.toList()
        val columns=keys.joinToString(",")
        store.readableDatabase.rawQuery("SELECT $columns,gender,nationality,workshop_id FROM employees WHERE id=?",arrayOf(id.toString())).use { c->
            if(c.moveToFirst()) {
                selectedId=id
                keys.forEachIndexed { i,key->fields[key]?.setText(c.getString(i) ?: "") }
                gender.setSelection(listOf("انتخاب کنید","مرد","زن").indexOf(c.getString(keys.size)).coerceAtLeast(0))
                nationality.setSelection(listOf("انتخاب کنید","ایرانی","غیرایرانی").indexOf(c.getString(keys.size+1)).coerceAtLeast(0))
                val assigned=workshops.indexOfFirst { it.first==c.getLong(keys.size+2) }
                workshopPicker.setSelection(assigned+1)
                if(assigned<0)toast("این کارمند کارگاه ندارد؛ با ویرایش، کارگاه را انتخاب کنید.")
                enable(false)
            }
        }
    }
    private fun save() {
        if(!editing){toast("ابتدا جدید یا ویرایش را انتخاب کنید");return}
        val workshopId=workshops.getOrNull(workshopPicker.selectedItemPosition-1)?.first
        if(workshopId==null){toast("انتخاب کارگاه الزامی است");return}
        val values=fields.mapValues { it.value.text.toString().trim() }.toMutableMap()
        val insuranceNo=EmployeeRules.normalizeInsuranceNo(values["insurance_no"] ?: "")
        if(insuranceNo==null){toast("شماره بیمه باید ۱ تا ۱۰ رقم و غیرصفر باشد");return}
        values["insurance_no"]=insuranceNo
        listOf("national_id","birth_date","job_code","birth_cert_no").forEach { key ->
            values[key]=EmployeeRules.normalizeDigits(values[key] ?: "")
        }
        if(fields.keys.filter { it!="personnel_no" }.any { values[it].isNullOrBlank() }
            || gender.selectedItemPosition==0 || nationality.selectedItemPosition==0) {
            toast("تمام اطلاعات پرسنل الزامی است");return
        }
        if(!EmployeeRules.validNationalId(values["national_id"] ?: "")){toast("کد ملی معتبر نیست");return}
        if(!EmployeeRules.validSolarDate(values["birth_date"] ?: "")){toast("تاریخ تولد معتبر نیست؛ نمونه: ۱۳۷۰/۰۱/۰۱");return}
        if(!EmployeeRules.validJobCode(values["job_code"] ?: "")){toast("کد شغل باید عددی باشد");return}
        val id=selectedId
        val db=store.writableDatabase
        if(!db.rawQuery("SELECT 1 FROM workshops WHERE id=?",arrayOf(workshopId.toString())).use { it.moveToFirst() }) {
            toast("کارگاه انتخاب‌شده دیگر موجود نیست");return
        }
        if(db.rawQuery(
            "SELECT 1 FROM employees WHERE workshop_id=? AND id<>? AND (national_id=? OR insurance_no=?) LIMIT 1",
            arrayOf(workshopId.toString(),(id ?: -1L).toString(),values["national_id"]!!,insuranceNo)
        ).use { it.moveToFirst() }) {toast("کد ملی یا شماره بیمه تکراری در همین کارگاه است");return}
        if(id!=null && db.rawQuery(
            """SELECT 1 FROM insurance_list_items i JOIN insurance_lists l ON l.id=i.list_id
               WHERE i.employee_id=? AND l.workshop_id<>? LIMIT 1""",
            arrayOf(id.toString(),workshopId.toString())
        ).use { it.moveToFirst() }) {
            toast("این فرد در لیست کارگاه دیگری سابقه دارد؛ انتقال مستقیم برای حفظ سوابق مجاز نیست")
            return
        }
        val data=ContentValues().apply {
            values.forEach { (key,v)->if(key!="personnel_no")put(key,v) }
            put("workshop_id",workshopId)
            put("gender",gender.selectedItem.toString())
            put("nationality",nationality.selectedItem.toString())
        }
        try {
            if(id==null) {
                db.beginTransaction()
                try {
                    val newId=db.insertOrThrow("employees",null,data)
                    db.execSQL("UPDATE employees SET personnel_no=? WHERE id=?",
                        arrayOf(newId.toString().padStart(6,'0'),newId))
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
            } else db.update("employees",data,"id=?",arrayOf(id.toString()))
            toast("اطلاعات پرسنلی ذخیره شد");render()
        } catch(ex:Exception){toast("خطا در ذخیره اطلاعات")}
    }
    private fun deleteSelected() {
        val id=selectedId ?: run {toast("ابتدا کارمند را انتخاب کنید");return}
        val linked=store.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM insurance_list_items WHERE employee_id=?",arrayOf(id.toString())
        ).use { it.moveToFirst();it.getInt(0) }
        if(linked>0) {
            AlertDialog.Builder(this).setTitle("حفظ سوابق بیمه")
                .setMessage("این کارمند در $linked ریزکارکرد ثبت‌شده سابقه دارد. برای جلوگیری از مخدوش‌شدن گزارش‌ها، حذف امکان‌پذیر نیست.")
                .setPositiveButton("متوجه شدم",null).show()
            return
        }
        AlertDialog.Builder(this).setTitle("حذف کارمند").setMessage("اطلاعات کارمند انتخاب‌شده حذف شود؟")
            .setPositiveButton("حذف"){_,_->
                val used=store.readableDatabase.rawQuery(
                    "SELECT 1 FROM insurance_list_items WHERE employee_id=? LIMIT 1",arrayOf(id.toString())
                ).use { it.moveToFirst() }
                if(used)toast("کارمند دارای سوابق بیمه است و قابل حذف نیست")
                else {store.writableDatabase.delete("employees","id=?",arrayOf(id.toString()));render()}
            }.setNegativeButton("انصراف",null).show()
    }
    private fun toast(message:String)=Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}
