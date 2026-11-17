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
    private data class Workshop(val id:Long,val name:String,val code:String,val rate:Double)
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
    private lateinit var insuredRateField:EditText
    private lateinit var employerRateField:EditText
    private lateinit var unemploymentRateField:EditText
    private lateinit var specialRatesConfirmed:CheckBox
    private lateinit var employerReliefField:EditText
    private lateinit var employerReliefReference:EditText
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
    private fun decimalInput(hintText:String):EditText=EditText(this).apply{
        hint=hintText
        inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        textSize=15f
        setTextColor(ink)
        setSingleLine(true)
        gravity=Gravity.CENTER
    }
    private fun sectionTitle(s:String)=label(s,17f,blue).apply {
        setTypeface(null,Typeface.BOLD);setPadding(0,dp(10),0,dp(8))
    }
    private fun action(caption:String,symbol:String,run:()->Unit)=
        label("$symbol  $caption",13.5f,blue).apply {
            gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD)
            background=bg(Color.WHITE,12,border);elevation=dp(2).toFloat()
            SuraUi.touch(this@InsuranceListActivity,this)
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
        window.statusBarColor=SuraUi.charcoal
        workshops=mutableListOf<Workshop>().also { all ->
            store.readableDatabase.rawQuery(
                "SELECT id,name,code,insurance_rate FROM workshops ORDER BY name,code",null
            ).use { c->while(c.moveToNext())
                all.add(Workshop(c.getLong(0),c.getString(1),c.getString(2),c.getDouble(3)))
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
        root.addView(SuraUi.banner(this,"لیست بیمه ماهانه","ثبت دوره، جمع‌بندی مزایا، نرخ‌ها و محاسبات قانونی","list"),
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
        searchBox.addView(action(if(SuraUi.isCompact(this)) "" else "جست‌وجو","⌕"){
            query=searchField.text.toString().trim();refreshRows()
        }.apply { contentDescription="جست‌وجوی لیست‌های ماهانه" },LinearLayout.LayoutParams(dp(if(SuraUi.isCompact(this)) 54 else 105),dp(48)))
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
        ).forEach {
            if(SuraUi.isCompact(this)) {
                fieldsRow.orientation=LinearLayout.VERTICAL
                fieldsRow.addView(it,LinearLayout.LayoutParams(-1,-2).apply {
                    bottomMargin=dp(8)
                })
            }else {
                fieldsRow.addView(it,LinearLayout.LayoutParams(0,-2,1f).apply {
                    leftMargin=dp(3);rightMargin=dp(3)
                })
            }
        }
        editor.addView(fieldsRow)
        editor.addView(sectionTitle("نرخ‌های حق بیمه (درصد)"))
        insuredRateField=decimalInput("۷")
        employerRateField=decimalInput("۲۰")
        unemploymentRateField=decimalInput("۳")
        listOf(
            framed("سهم بیمه‌شده",insuredRateField),
            framed("سهم کارفرما",employerRateField),
            framed("بیمه بیکاری",unemploymentRateField)
        ).forEach{editor.addView(it,LinearLayout.LayoutParams(-1,-2).apply{
            bottomMargin=dp(7)
        })}
        specialRatesConfirmed=CheckBox(this).apply{
            text="نرخ اختصاصی مطابق مستند معتبر تأیید شده است"
            textSize=12.5f
            setTextColor(ink)
        }
        editor.addView(specialRatesConfirmed)
        editor.addView(label(
            "برای کارگاه عادی، نسبت ۷ / ۲۰ / ۳ ثابت است. برای نرخ اختصاصی، نرخ‌های تأییدشده را وارد کنید؛ مجموع باید برابر نرخ ثبت‌شده کارگاه باشد.",
            11.5f,muted).apply{setPadding(dp(4),dp(3),dp(4),dp(11))})
        employerReliefField=decimalInput("۰ ریال")
        employerReliefField.inputType=InputType.TYPE_CLASS_NUMBER
        employerReliefReference=EditText(this).apply{
            hint="شماره تأییدیه/ابلاغ شعبه"
            textSize=13f
            setTextColor(ink)
            setSingleLine(true)
        }
        editor.addView(sectionTitle("معافیت تأییدشده سهم کارفرما"))
        editor.addView(framed("مبلغ معافیت تأییدشده این ماه (ریال)",employerReliefField),
            LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
        editor.addView(framed("مستند تأییدیه معافیت",employerReliefReference),
            LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
        editor.addView(label(
            "معافیت خودکار نیست؛ مبلغ و مستند تأییدشده شعبه را ثبت کنید. جمع پرداختی پس از کسر مبلغ مجاز محاسبه می‌شود.",
            11.5f,muted).apply{setPadding(dp(4),0,dp(4),dp(12))})
        root.addView(editor,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(12)})

        details=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        root.addView(details)
        cancel(initial=true)
        refreshRows()
    }
    private fun editable(value:Boolean){
        editing=value;workshopField.isEnabled=value;yearField.isEnabled=value;monthField.isEnabled=value
        insuredRateField.isEnabled=value
        employerRateField.isEnabled=value
        unemploymentRateField.isEnabled=value
        specialRatesConfirmed.isEnabled=value
        employerReliefField.isEnabled=value
        employerReliefReference.isEnabled=value
    }
    private fun cancel(initial:Boolean=false){
        selectedId=null
        editable(false)
        workshopField.setSelection(0);yearField.setText("");monthField.setSelection(0)
        numberField.text="خودکار"
        insuredRateField.setText("7");employerRateField.setText("20")
        unemploymentRateField.setText("3");specialRatesConfirmed.isChecked=false
        employerReliefField.setText("0");employerReliefReference.setText("")
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
            """SELECT workshop_id,year,month,list_number,insured_rate_bps,employer_rate_bps,
                      unemployment_rate_bps,rates_confirmed,employer_relief_amount,
                      employer_relief_reference FROM insurance_lists WHERE id=?""",
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
            insuredRateField.setText(MonthlyInsuranceRules.formatRate(c.getInt(4)))
            employerRateField.setText(MonthlyInsuranceRules.formatRate(c.getInt(5)))
            unemploymentRateField.setText(MonthlyInsuranceRules.formatRate(c.getInt(6)))
            specialRatesConfirmed.isChecked=c.getInt(7)==1
            employerReliefField.setText(c.getLong(8).toString())
            employerReliefReference.setText(c.getString(9) ?: "")
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
                    COALESCE(SUM(i.days*i.daily_wage+i.benefits+i.days*i.seniority_daily+i.marital_allowance),0)
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
    private fun loadMonthlyEntries(id:Long):List<MonthlyInsuranceEntry>{
        val result=mutableListOf<MonthlyInsuranceEntry>()
        store.readableDatabase.rawQuery(
            """SELECT days,daily_wage,benefits,seniority_daily,marital_allowance,
                      non_subject_benefits FROM insurance_list_items
               WHERE list_id=? ORDER BY id""",arrayOf(id.toString())
        ).use{c->while(c.moveToNext()){
            result.add(MonthlyInsuranceEntry(
                c.getInt(0),c.getLong(1),c.getLong(2),
                c.getLong(3),c.getLong(4),c.getLong(5)))
        }}
        return result
    }
    private fun showDetails(id:Long){
        details.removeAllViews()
        data class Config(
            val year:Int,val month:Int,val rate:Double,
            val insured:Int,val employer:Int,val unemployment:Int,
            val confirmed:Boolean,val relief:Long,val reference:String
        )
        val config=store.readableDatabase.rawQuery(
            """SELECT w.code,w.contract_row,w.name,w.employer,w.address,l.year,l.month,
                      l.list_number,w.insurance_rate,l.insured_rate_bps,l.employer_rate_bps,
                      l.unemployment_rate_bps,l.rates_confirmed,l.employer_relief_amount,
                      l.employer_relief_reference
               FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id WHERE l.id=?""",
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
            Config(c.getInt(5),c.getInt(6),c.getDouble(8),c.getInt(9),
                c.getInt(10),c.getInt(11),c.getInt(12)==1,c.getLong(13),
                c.getString(14) ?: "")
        }
        try{
            val entries=loadMonthlyEntries(id)
            var days=0L;var wage=0L;var seniority=0L;var marital=0L
            var benefits=0L;var nonSubject=0L
            entries.forEach{
                days=Math.addExact(days,it.days.toLong())
                wage=Math.addExact(wage,Math.multiplyExact(it.days.toLong(),it.dailyWage))
                seniority=Math.addExact(seniority,Math.multiplyExact(it.days.toLong(),it.seniorityDaily))
                marital=Math.addExact(marital,it.maritalAllowance)
                benefits=Math.addExact(benefits,it.benefits)
                nonSubject=Math.addExact(nonSubject,it.nonSubjectBenefits)
            }
            val subject=Math.addExact(Math.addExact(wage,seniority),
                Math.addExact(benefits,marital))
            group("خلاصه محاسبات خودکار",listOf(
                "تعداد نفرات" to entries.size.toString(),
                "روزهای کارکرد" to days.toString(),
                "دستمزد ماهانه" to "${amount(wage)} ریال",
                "پایه سنوات" to "${amount(seniority)} ریال",
                "حق تأهل" to "${amount(marital)} ریال",
                "سایر مزایای مشمول" to "${amount(benefits)} ریال",
                "مزایای غیرمشمول" to "${amount(nonSubject)} ریال",
                "جمع مزد و مزایای مشمول" to "${amount(subject)} ریال",
                "کل پرداختی قبل از کسورات" to "${amount(Math.addExact(subject,nonSubject))} ریال"
            ))
            val ordinary=InsurancePremiumMath.isOrdinaryRate(config.rate)
            if(!ordinary && !config.confirmed){
                details.addView(label(
                    "نرخ اختصاصی کارگاه باید در فرم لیست با مستند معتبر تأیید شود؛ محاسبه پرداختی تا آن زمان متوقف است.",
                    12f,Color.rgb(166,55,45)).apply{setPadding(dp(7),dp(10),dp(7),dp(10))})
                return
            }
            val standard=Triple(config.insured,config.employer,config.unemployment)==
                Triple(700,2000,300)
            if(ordinary && !standard){
                details.addView(label("نرخ عادی باید دقیقاً ۷، ۲۰ و ۳ درصد باشد.",
                    12f,Color.rgb(166,55,45)))
                return
            }
            val wanted=MonthlyInsuranceRules.rateBps(config.rate.toString())
            val sumRates=config.insured+config.employer+config.unemployment
            if(!ordinary && (wanted==null || sumRates!=wanted)){
                details.addView(label(
                    "جمع نرخ‌های ثبت‌شده با نرخ اختصاصی کارگاه یکسان نیست.",
                    12f,Color.rgb(166,55,45)))
                return
            }
            val premium=MonthlyInsuranceRules.calculate(
                config.year,config.month,entries,
                config.insured,config.employer,config.unemployment,
                config.relief,config.reference)
            group("محاسبه حق بیمه",listOf(
                "جمع مشمول قبل از سقف" to "${amount(premium.subjectGross)} ریال",
                "مازاد حذف‌شده بابت سقف" to "${amount(premium.excludedByCap)} ریال",
                "مبنای حق بیمه پس از سقف" to "${amount(premium.cappedSubject)} ریال",
                "سهم بیمه‌شده (${MonthlyInsuranceRules.formatRate(config.insured)}٪)" to
                    "${amount(premium.insured)} ریال",
                "سهم کارفرما (${MonthlyInsuranceRules.formatRate(config.employer)}٪)" to
                    "${amount(premium.employer)} ریال",
                "بیمه بیکاری (${MonthlyInsuranceRules.formatRate(config.unemployment)}٪)" to
                    "${amount(premium.unemployment)} ریال",
                "معافیت تأییدشده سهم کارفرما" to
                    "${amount(premium.approvedEmployerRelief)} ریال",
                "سهم پرداختنی کارفرما پس از معافیت" to
                    "${amount(premium.employerPayable)} ریال",
                "جمع حق بیمه قابل پرداخت" to "${amount(premium.totalPayable)} ریال"
            ))
            premium.warnings.forEach{warning->
                details.addView(label("⚠ $warning",12f,Color.rgb(166,55,45)).apply{
                    setPadding(dp(7),dp(5),dp(7),dp(5))
                })
            }
            details.addView(label(
                "معافیت فقط بر مبنای مبلغ تأییدشده اعمال می‌شود. سهم ۴٪ مشاغل سخت بدهی جداگانه است و در لیست عادی محاسبه نمی‌شود.",
                12f,Color.rgb(151,88,37)).apply{setPadding(dp(7),dp(13),dp(7),dp(13))})
        }catch(ex:IllegalArgumentException){
            details.addView(label("نیازمند اصلاح یا تأیید: ${ex.message}",
                12f,Color.rgb(166,55,45)).apply{setPadding(dp(7),dp(12),dp(7),dp(12))})
        }catch(ex:ArithmeticException){
            details.addView(label("خطای سرریز محاسبات؛ ریزکارکرد را بررسی کنید.",
                12f,Color.rgb(166,55,45)))
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
        val insuredRate=MonthlyInsuranceRules.rateBps(insuredRateField.text.toString())
        val employerRate=MonthlyInsuranceRules.rateBps(employerRateField.text.toString())
        val unemploymentRate=MonthlyInsuranceRules.rateBps(unemploymentRateField.text.toString())
        val relief=WorkDetailRules.parseNonnegative(employerReliefField.text.toString())
        val reference=employerReliefReference.text.toString().trim()
        if(insuredRate==null || employerRate==null || unemploymentRate==null ||
            !MonthlyInsuranceRules.validateRates(insuredRate,employerRate,unemploymentRate)){
            msg("نرخ‌های سهم بیمه را به‌صورت درصد معتبر ثبت کنید");return
        }
        val ordinary=InsurancePremiumMath.isOrdinaryRate(workshop.rate)
        if(ordinary && Triple(insuredRate,employerRate,unemploymentRate)!=Triple(700,2000,300)){
            msg("نرخ کارگاه عادی باید ۷٪ بیمه‌شده، ۲۰٪ کارفرما و ۳٪ بیکاری باشد")
            return
        }
        if(!ordinary){
            val expectedRate=MonthlyInsuranceRules.rateBps(workshop.rate.toString())
            if(!specialRatesConfirmed.isChecked || expectedRate==null ||
                insuredRate+employerRate+unemploymentRate!=expectedRate){
                msg("برای نرخ اختصاصی، تأیید مستند و مجموع نرخ برابر نرخ کارگاه الزامی است")
                return
            }
        }
        if(relief==null || (relief>0L && reference.isEmpty())){
            msg("مبلغ معافیت نامعتبر است یا شماره تأییدیه وارد نشده است")
            return
        }
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
        val oldWorkshopId=if(oldId==null)null else db.rawQuery(
            "SELECT workshop_id FROM insurance_lists WHERE id=?",
            arrayOf(oldId.toString())
        ).use{c->if(c.moveToFirst())c.getLong(0) else null}
        if(oldId!=null && oldWorkshopId!=workshop.id && db.rawQuery(
            "SELECT 1 FROM insurance_list_items WHERE list_id=? LIMIT 1",
            arrayOf(oldId.toString())
        ).use{it.moveToFirst()}){
            msg("این لیست ریزکارکرد دارد و تغییر کارگاه آن مجاز نیست")
            return
        }
        if(relief>0L){
            if(oldId==null || MonthlyInsuranceRules.minimumDailyWage(year)==null){
                msg("برای معافیت ابتدا ریزکارکرد و دوره قابل محاسبه ثبت کنید")
                return
            }
            try{
                MonthlyInsuranceRules.calculate(year,month,loadMonthlyEntries(oldId),
                    insuredRate,employerRate,unemploymentRate,relief,reference)
            }catch(ex:Exception){
                msg(ex.message ?: "مبلغ معافیت با سهم کارفرما سازگار نیست")
                return
            }
        }
        var saved:Long?=null
        try{
            db.beginTransaction()
            try{
                val values=ContentValues().apply {
                    put("workshop_id",workshop.id);put("year",year);put("month",month)
                    put("insured_rate_bps",insuredRate)
                    put("employer_rate_bps",employerRate)
                    put("unemployment_rate_bps",unemploymentRate)
                    put("rates_confirmed",if(specialRatesConfirmed.isChecked)1 else 0)
                    put("employer_relief_amount",relief)
                    put("employer_relief_reference",reference)
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
