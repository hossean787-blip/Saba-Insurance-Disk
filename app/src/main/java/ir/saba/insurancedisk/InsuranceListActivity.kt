package ir.saba.insurancedisk

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import java.lang.Math

class InsuranceListActivity : Activity() {
    private data class Workshop(val id:Long,val name:String,val code:String)
    private val store by lazy { MainActivity.Store(this) }
    private val blue=SuraUi.primary
    private val ink=SuraUi.title
    private val muted=SuraUi.muted
    private val pale=SuraUi.pastel
    private val border=SuraUi.line
    private lateinit var root:LinearLayout
    private lateinit var listTable:LinearLayout
    private lateinit var horizontal:HorizontalScrollView
    private lateinit var countText:TextView
    private lateinit var details:LinearLayout
    private lateinit var formHint:TextView
    private lateinit var workshopField:Spinner
    private lateinit var yearField:EditText
    private lateinit var monthField:Spinner
    private lateinit var numberField:TextView
    private lateinit var searchField:EditText
    private var workshops=listOf<Workshop>()
    private var selectedId:Long?=null
    private var editing=false
    private var query=""

    private fun dp(n:Int)=(n*resources.displayMetrics.density+0.5f).toInt()
    private fun bg(color:Int,radius:Int=12,stroke:Int=0)=GradientDrawable().apply{
        setColor(color);cornerRadius=dp(radius).toFloat()
        if(stroke!=0)setStroke(dp(1),stroke)
    }
    private fun label(s:String,size:Float=14f,color:Int=ink)=TextView(this).apply{
        text=s;textSize=size;setTextColor(color)
        gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    }
    private fun two(n:Int)=n.toString().padStart(2,'0')
    private fun amount(n:Long)=String.format(java.util.Locale.US,"%,d",n)
    private fun msg(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    private fun sectionTitle(s:String)=label(s,17f,blue).apply {
        setTypeface(null,Typeface.BOLD);setPadding(0,dp(10),0,dp(8))
    }
    private fun action(caption:String,symbol:String,run:()->Unit)=
        label("$symbol  $caption",13.5f,blue).apply {
            gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD)
            background=bg(Color.WHITE,12,border);elevation=dp(2).toFloat()
            setOnClickListener{run()}
        }
    private fun toolbar(vararg items:Triple<String,String,()->Unit>) {
        val row=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
        }
        items.forEach { (caption,icon,run)->
            row.addView(action(caption,icon,run),
                LinearLayout.LayoutParams(0,dp(52),1f).apply {
                    setMargins(dp(3),dp(4),dp(3),dp(4))
                })
        }
        root.addView(row)
    }
    private fun framed(title:String,control:View):LinearLayout =
        LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=bg(Color.rgb(251,253,255),11,border)
            setPadding(dp(9),dp(8),dp(9),dp(5))
            addView(label(title,12f,muted))
            addView(control,LinearLayout.LayoutParams(-1,dp(44)))
        }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(5,39,88)
        workshops=mutableListOf<Workshop>().also { all ->
            store.readableDatabase.rawQuery(
                "SELECT id,name,code FROM workshops ORDER BY name,code",null
            ).use { c->while(c.moveToNext())
                all.add(Workshop(c.getLong(0),c.getString(1),c.getString(2)))
            }
        }
        draw()
    }
    private fun draw(){
        root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(15),dp(16),dp(15),dp(30))
            setBackgroundColor(SuraUi.canvas)
        }
        val scroll=ScrollView(this).apply { isFillViewport=true;clipToPadding=false;addView(root) }
        scroll.setOnApplyWindowInsetsListener { view,insets ->
            val top=if(android.os.Build.VERSION.SDK_INT>=30)
                insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
                else insets.systemWindowInsetTop
            val bottom=if(android.os.Build.VERSION.SDK_INT>=30)
                insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
                else insets.systemWindowInsetBottom
            view.setPadding(0,top,0,bottom+dp(12));insets
        }
        setContentView(SuraUi.mount(this,scroll,"lists"))
        root.addView(SuraUi.banner(this,"لیست بیمه ماهانه","دوره‌های بیمه، گزارش و مشخصات لیست","list"),
            LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})
        toolbar(
            Triple("جدید","＋",{startNew()}),
            Triple("ویرایش","✎",{startEdit()}),
            Triple("حذف","×",{deleteSelected()})
        )
        toolbar(
            Triple("ثبت","✓",{save()}),
            Triple("لغو","↶",{cancel()}),
            Triple("خروج","⌂",{finish()})
        )
        val searchBox=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER_VERTICAL
            background=bg(Color.WHITE,14,border);setPadding(dp(10),dp(5),dp(10),dp(5))
        }
        searchField=EditText(this).apply {
            hint="جست‌وجوی کارگاه، کد، دوره یا شماره"
            setSingleLine(true);textSize=13f;setTextColor(ink);setHintTextColor(muted)
        }
        searchBox.addView(searchField,LinearLayout.LayoutParams(0,dp(47),1f))
        searchBox.addView(action("جست‌وجو","⌕"){
            query=searchField.text.toString().trim();refreshRows()
        },LinearLayout.LayoutParams(dp(105),dp(43)))
        root.addView(searchBox,LinearLayout.LayoutParams(-1,-2).apply {
            topMargin=dp(10);bottomMargin=dp(12)
        })

        root.addView(sectionTitle("فهرست لیست‌های ماهانه"))
        countText=label("",12f,muted).apply {setPadding(0,0,0,dp(7))}
        root.addView(countText)
        horizontal=HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled=true
            background=bg(Color.WHITE,14,border)
        }
        listTable=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;setPadding(dp(4),dp(4),dp(4),dp(4))
        }
        horizontal.addView(listTable)
        root.addView(horizontal,LinearLayout.LayoutParams(-1,-2))
        root.addView(label("برای انتخاب روی سطر بزنید؛ جدول به طرفین قابل پیمایش است.",
            12f,muted).apply{setPadding(0,dp(6),0,dp(12))})

        root.addView(sectionTitle("مشخصات لیست"))
        formHint=label("",12f,muted).apply {setPadding(0,0,0,dp(8))}
        root.addView(formHint)
        val editor=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=bg(Color.WHITE,14,border)
            setPadding(dp(11),dp(12),dp(11),dp(4))
        }
        workshopField=Spinner(this).apply {
            adapter=ArrayAdapter(this@InsuranceListActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("انتخاب کارگاه")+workshops.map{"${it.name} — ${it.code}"})
        }
        editor.addView(framed("کارگاه *",workshopField),
            LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
        yearField=EditText(this).apply {
            hint="مثلاً ۰۵";inputType=InputType.TYPE_CLASS_NUMBER
            filters=arrayOf(InputFilter.LengthFilter(2))
            textSize=15f;setTextColor(ink);gravity=Gravity.CENTER;setSingleLine(true)
        }
        monthField=Spinner(this).apply {
            adapter=ArrayAdapter(this@InsuranceListActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("انتخاب")+(1..12).map{"ماه ${two(it)}"})
        }
        numberField=label("خودکار",15f,muted).apply{gravity=Gravity.CENTER}
        val fieldsRow=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
        }
        listOf(
            framed("سال *",yearField),
            framed("ماه *",monthField),
            framed("شماره لیست",numberField)
        ).forEach { fieldsRow.addView(it,LinearLayout.LayoutParams(0,-2,1f).apply {
            leftMargin=dp(3);rightMargin=dp(3)
        }) }
        editor.addView(fieldsRow)
        root.addView(editor,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(12)})

        details=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        root.addView(details)
        cancel(initial=true)
        refreshRows()
    }
    private fun editable(value:Boolean){
        editing=value;workshopField.isEnabled=value;yearField.isEnabled=value;monthField.isEnabled=value
    }
    private fun cancel(initial:Boolean=false){
        selectedId=null
        editable(false)
        workshopField.setSelection(0);yearField.setText("");monthField.setSelection(0)
        numberField.text="خودکار"
        formHint.text="از جدول یک لیست انتخاب کنید یا «جدید» را بزنید."
        if(::details.isInitialized)details.removeAllViews()
        if(!initial)refreshRows()
    }
    private fun startNew(){
        cancel(initial=true)
        if(workshops.isEmpty()){msg("ابتدا کارگاه ثبت کنید");return}
        editable(true)
        formHint.text="لیست جدید: کارگاه، سال و ماه را وارد و «ثبت» را بزنید."
        refreshRows()
    }
    private fun startEdit(){
        if(selectedId==null){msg("ابتدا لیست را از جدول انتخاب کنید");return}
        editable(true)
        formHint.text="ویرایش فعال است؛ تغییرات را با «ثبت» ذخیره کنید."
    }
    private fun selectRow(id:Long){
        selectedId=id
        loadSelected()
        refreshRows()
    }
    private fun loadSelected(){
        val id=selectedId ?: return
        editable(false)
        store.readableDatabase.rawQuery(
            "SELECT workshop_id,year,month,list_number FROM insurance_lists WHERE id=?",
            arrayOf(id.toString())
        ).use { c->
            if(!c.moveToFirst()){
                cancel(initial=true)
                msg("لیست انتخاب‌شده وجود ندارد")
                return
            }
            val wi=workshops.indexOfFirst{it.id==c.getLong(0)}
            workshopField.setSelection(wi+1)
            yearField.setText(two(c.getInt(1)%100))
            monthField.setSelection(c.getInt(2).coerceIn(1,12))
            numberField.text=two(c.getInt(3))
            formHint.text=if(wi<0)"کارگاه این لیست در فهرست موجود نیست."
                else "لیست انتخاب شد؛ برای اصلاح «ویرایش» را بزنید."
        }
        showDetails(id)
    }

    private fun tableCell(value:String,width:Int,header:Boolean)=
        label(value,if(header)12f else 12.5f,if(header)Color.WHITE else ink).apply{
            gravity=Gravity.CENTER
            setPadding(dp(5),dp(9),dp(5),dp(9))
            maxLines=2;ellipsize=TextUtils.TruncateAt.END
            minHeight=dp(52)
            if(header)setTypeface(null,Typeface.BOLD)
            layoutParams=LinearLayout.LayoutParams(dp(width),-2)
        }
    private fun row(values:List<String>,header:Boolean=false,chosen:Boolean=false,index:Int=0,
        click:(()->Unit)?=null):LinearLayout{
        val widths=listOf(67,170,83,65,64,153)
        val color=when{
            header->blue
            chosen->Color.rgb(210,231,255)
            index%2==0->Color.WHITE
            else->SuraUi.pastel
        }
        return LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER_VERTICAL
            background=bg(color,7,if(chosen)Color.rgb(55,128,210) else 0)
            values.forEachIndexed{i,v->addView(tableCell(v,widths[i],header))}
            if(click!=null) {
                setOnClickListener{click()}
                isFocusable=true
                contentDescription="انتخاب لیست ${values.joinToString("، ")}"
            }
        }
    }
    private fun refreshRows(){
        if(!::listTable.isInitialized)return
        val sx=horizontal.scrollX
        listTable.removeAllViews()
        listTable.addView(row(
            listOf("شماره","کارگاه","دوره","نفرات","روزها","جمع مشمول"),header=true))
        var count=0
        val filter=EmployeeRules.normalizeDigits(query.trim())
        try{
            val sql="""SELECT l.id,w.name,w.code,l.year,l.month,l.list_number,
                    COUNT(i.id),COALESCE(SUM(i.days),0),
                    COALESCE(SUM(i.days*i.daily_wage+i.benefits),0)
                FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id
                LEFT JOIN insurance_list_items i ON i.list_id=l.id
                GROUP BY l.id,w.name,w.code,l.year,l.month,l.list_number
                ORDER BY l.year DESC,l.month DESC,l.id DESC"""
            store.readableDatabase.rawQuery(sql,null).use{c->
                while(c.moveToNext()){
                    val id=c.getLong(0);val name=c.getString(1);val code=c.getString(2)
                    val year=c.getInt(3);val month=c.getInt(4);val no=c.getInt(5)
                    val period="${two(year%100)}/${two(month)}"
                    val haystack="$name $code $year ${two(year%100)} $month ${two(month)} $period ${two(no)}"
                    if(filter.isNotEmpty()&&!haystack.contains(filter,ignoreCase=true))continue
                    val chosen=selectedId==id
                    listTable.addView(row(listOf(
                        (if(chosen)"✓ " else "")+two(no),name,period,
                        c.getLong(6).toString(),c.getLong(7).toString(),amount(c.getLong(8))
                    ),chosen=chosen,index=count,click={selectRow(id)}))
                    count++
                }
            }
        }catch(ex:Exception){
            listTable.addView(label("خطا در بارگذاری لیست‌ها؛ اطلاعات را بررسی کنید.",
                14f,Color.rgb(166,55,45)).apply{setPadding(dp(12),dp(18),dp(12),dp(18))})
        }
        if(count==0)listTable.addView(label("لیستی برای نمایش پیدا نشد.",14f,muted).apply {
            setPadding(dp(12),dp(18),dp(12),dp(18))
        })
        countText.text="$count لیست  •  سطر دلخواه را برای مشاهده مشخصات انتخاب کنید."
        horizontal.post{horizontal.scrollTo(sx,0)}
    }
    private fun infoTable(pairs:List<Pair<String,String>>):LinearLayout{
        val card=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            background=bg(Color.WHITE,13,border)
            setPadding(dp(5),dp(5),dp(5),dp(5))
        }
        pairs.forEachIndexed{index,(key,value)->
            val line=LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
                gravity=Gravity.CENTER_VERTICAL
                background=bg(if(index%2==0)Color.WHITE else pale,6)
            }
            line.addView(label(key,13f,muted).apply{
                setPadding(dp(10),dp(10),dp(5),dp(10))
            },LinearLayout.LayoutParams(0,-2,1f))
            line.addView(label(value,13f,ink).apply{
                if(key=="جمع دستمزد و مزایای مشمول")setTypeface(null,Typeface.BOLD)
                setPadding(dp(5),dp(10),dp(10),dp(10))
            },LinearLayout.LayoutParams(0,-2,1.25f))
            card.addView(line)
        }
        return card
    }
    private fun group(title:String,pairs:List<Pair<String,String>>){
        details.addView(sectionTitle(title))
        details.addView(infoTable(pairs))
    }
    private fun showDetails(id:Long){
        details.removeAllViews()
        store.readableDatabase.rawQuery(
            """SELECT w.code,w.contract_row,w.name,w.employer,w.address,l.year,l.month,l.list_number
                FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id
                WHERE l.id=?""",
            arrayOf(id.toString())
        ).use{c->
            if(!c.moveToFirst())return
            group("اطلاعات کارگاه و دوره",listOf(
                "کد کارگاه" to c.getString(0),
                "ردیف پیمان" to c.getString(1),
                "نام کارگاه" to c.getString(2),
                "کارفرما" to c.getString(3),
                "آدرس" to c.getString(4),
                "سال / ماه" to "${two(c.getInt(5)%100)} / ${two(c.getInt(6))}",
                "شماره لیست" to two(c.getInt(7))
            ))
        }
        try{
            var people=0L;var days=0L;var daily=0L;var monthly=0L;var benefits=0L
            store.readableDatabase.rawQuery(
                "SELECT days,daily_wage,benefits FROM insurance_list_items WHERE list_id=?",
                arrayOf(id.toString())
            ).use{c->while(c.moveToNext()){
                people=Math.addExact(people,1L)
                days=Math.addExact(days,c.getLong(0))
                daily=Math.addExact(daily,c.getLong(1))
                monthly=Math.addExact(monthly,Math.multiplyExact(c.getLong(0),c.getLong(1)))
                benefits=Math.addExact(benefits,c.getLong(2))
            }}
            val total=Math.addExact(monthly,benefits)
            group("خلاصه محاسبات خودکار — فقط خواندنی",listOf(
                "تعداد نفرات" to people.toString(),
                "جمع روزهای کارکرد" to days.toString(),
                "جمع دستمزد روزانه" to "${amount(daily)} ریال",
                "جمع دستمزد ماهانه" to "${amount(monthly)} ریال",
                "جمع مزایای مشمول" to "${amount(benefits)} ریال",
                "جمع دستمزد و مزایای مشمول" to "${amount(total)} ریال"
            ))
            group("حق بیمه و اقلام تکمیلی",listOf(
                "مزایای غیرمشمول" to "محاسبه‌نشده",
                "پایه سنواتی" to "محاسبه‌نشده",
                "حق تأهل" to "محاسبه‌نشده",
                "سهم بیمه‌شده" to "محاسبه‌نشده",
                "سهم کارفرما" to "محاسبه‌نشده",
                "بیمه بیکاری" to "محاسبه‌نشده",
                "۴٪ سخت و زیان‌آور" to "محاسبه‌نشده"
            ))
            details.addView(label(
                "تا تکمیل قواعد معتبر دوره و تفکیک مزایا، حق بیمه محاسبه نمی‌شود.",
                12f,Color.rgb(151,88,37)).apply{setPadding(dp(7),dp(13),dp(7),dp(13))})
        }catch(ex:ArithmeticException){
            details.addView(label("خطای سرریز محاسبات؛ ریزکارکرد را بررسی کنید.",
                13f,Color.rgb(166,55,45)))
        }
    }
    private fun save(){
        if(!editing){msg("ابتدا «جدید» یا «ویرایش» را بزنید");return}
        val workshop=workshops.getOrNull(workshopField.selectedItemPosition-1)
        val shortYear=EmployeeRules.normalizeDigits(yearField.text.toString().trim()).toIntOrNull()
        val month=monthField.selectedItemPosition
        if(workshop==null||shortYear==null||shortYear !in 0..99||month !in 1..12){
            msg("کارگاه، سال دو رقمی و ماه را به‌درستی مشخص کنید");return
        }
        val year=1400+shortYear
        val db=store.writableDatabase
        if(!db.rawQuery("SELECT 1 FROM workshops WHERE id=?",
            arrayOf(workshop.id.toString())).use{it.moveToFirst()}){
            msg("کارگاه انتخاب‌شده پیدا نشد");return
        }
        if(db.rawQuery(
            "SELECT 1 FROM insurance_lists WHERE workshop_id=? AND year=? AND month=? AND id<>? LIMIT 1",
            arrayOf(workshop.id.toString(),year.toString(),month.toString(),(selectedId?:-1L).toString())
        ).use{it.moveToFirst()}){
            msg("برای این کارگاه و ماه قبلاً لیست ثبت شده است");return
        }
        val oldId=selectedId
        if(oldId!=null && db.rawQuery(
            """SELECT 1 FROM insurance_list_items i JOIN insurance_lists l ON l.id=i.list_id
                WHERE i.list_id=? AND l.workshop_id<>? LIMIT 1""",
            arrayOf(oldId.toString(),workshop.id.toString())
        ).use{it.moveToFirst()}){
            msg("این لیست ریزکارکرد دارد؛ برای حفاظت از سوابق نمی‌توان کارگاه را تغییر داد")
            return
        }
        var saved:Long?=null
        try{
            db.beginTransaction()
            try{
                val values=ContentValues().apply {
                    put("workshop_id",workshop.id);put("year",year);put("month",month)
                }
                saved=if(oldId==null){
                    val next=db.rawQuery(
                        "SELECT COALESCE(MAX(list_number),0)+1 FROM insurance_lists WHERE workshop_id=? AND year=?",
                        arrayOf(workshop.id.toString(),year.toString())
                    ).use{c->c.moveToFirst();c.getInt(0)}
                    if(next>99)throw IllegalStateException("شماره لیست از ۹۹ بیشتر نمی‌شود")
                    values.put("list_number",next)
                    db.insertOrThrow("insurance_lists",null,values)
                } else {
                    if(db.update("insurance_lists",values,"id=?",arrayOf(oldId.toString()))!=1)
                        throw IllegalStateException("لیست موردنظر یافت نشد")
                    oldId
                }
                db.setTransactionSuccessful()
            }finally{db.endTransaction()}
            selectedId=saved
            query="";searchField.setText("")
            loadSelected()
            refreshRows()
            msg("لیست ماهانه ذخیره شد")
        }catch(ex:Exception){msg("ثبت ناموفق بود: ${ex.message ?: "خطای پایگاه داده"}")}
    }
    private fun deleteSelected(){
        val id=selectedId ?: run{msg("ابتدا یک لیست را از جدول انتخاب کنید");return}
        AlertDialog.Builder(this)
            .setTitle("حذف لیست بیمه")
            .setMessage("این لیست و تمامی ریزکارکردهای آن حذف خواهند شد. این عملیات برگشت‌پذیر نیست. مطمئن هستید؟")
            .setPositiveButton("حذف"){_,_->
                val db=store.writableDatabase
                try{
                    db.beginTransaction()
                    try{
                        db.delete("insurance_list_items","list_id=?",arrayOf(id.toString()))
                        db.delete("insurance_lists","id=?",arrayOf(id.toString()))
                        db.setTransactionSuccessful()
                    }finally{db.endTransaction()}
                    cancel()
                    msg("لیست حذف شد")
                }catch(ex:Exception){msg("حذف انجام نشد")}
            }
            .setNegativeButton("انصراف",null).show()
    }
}
