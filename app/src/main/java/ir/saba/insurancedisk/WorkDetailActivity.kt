package ir.saba.insurancedisk

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*

/** RTL monthly work-detail editor. Existing SQLite schema and historical rows are retained. */
class WorkDetailActivity : Activity() {
    private data class InsuranceList(val id:Long,val workshopId:Long,val workshop:String,val year:Int,val month:Int,val number:Int)
    private data class Employee(val id:Long,val name:String,val insurance:String)
    private data class Item(
        val id:Long,val employeeId:Long,val name:String,val insurance:String,
        val days:Int,val dailyWage:Long,val benefits:Long,val workshopId:Long?,
        val personnelNumber:String=""
    )

    private val store by lazy { MainActivity.Store(this) }
    private val blue=SuraUi.primary
    private val ink=SuraUi.title
    private val muted=SuraUi.muted
    private val pale=SuraUi.pastel
    private val border=SuraUi.line
    private val danger=Color.rgb(164,68,40)

    private lateinit var root:LinearLayout
    private lateinit var listPicker:Spinner
    private lateinit var employeePicker:Spinner
    private lateinit var searchField:EditText
    private lateinit var daysField:EditText
    private lateinit var wageField:EditText
    private lateinit var benefitField:EditText
    private lateinit var preview:TextView
    private lateinit var formStatus:TextView
    private lateinit var table:LinearLayout
    private lateinit var tableScroll:HorizontalScrollView
    private lateinit var rowsCount:TextView
    private lateinit var summary:LinearLayout

    private var lists=listOf<InsuranceList>()
    private var employees=listOf<Employee>()
    private var selectedItemId:Long?=null
    private var selectedEmployeeId:Long?=null
    private var editing=false
    private var query=""

    private fun dp(n:Int)=(n*resources.displayMetrics.density+0.5f).toInt()
    private fun shape(color:Int,radius:Int=12,stroke:Int=0)=GradientDrawable().apply {
        setColor(color);cornerRadius=dp(radius).toFloat()
        if(stroke!=0)setStroke(dp(1),stroke)
    }
    private fun label(value:String,size:Float=14f,color:Int=ink)=TextView(this).apply {
        text=value;textSize=size;setTextColor(color);gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    }
    private fun title(value:String)=label(value,17f,blue).apply {
        setTypeface(null,Typeface.BOLD)
        setPadding(0,dp(9),0,dp(8))
    }
    private fun amount(n:Long)=String.format(java.util.Locale.US,"%,d",n)
    private fun two(n:Int)=n.toString().padStart(2,'0')
    private fun toast(message:String)=Toast.makeText(this,message,Toast.LENGTH_LONG).show()
    private fun action(name:String,symbol:String,callback:()->Unit)=
        label("$symbol  $name",13.5f,blue).apply {
            gravity=Gravity.CENTER
            setTypeface(null,Typeface.BOLD)
            background=shape(Color.WHITE,12,border)
            elevation=dp(2).toFloat()
            SuraUi.touch(this@WorkDetailActivity,this)
            setOnClickListener { callback() }
        }
    private fun toolbar(vararg actions:Triple<String,String,()->Unit>) {
        val row=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
        }
        actions.forEach { (name,icon,callback) ->
            row.addView(action(name,icon,callback),
                LinearLayout.LayoutParams(0,dp(52),1f).apply {
                    setMargins(dp(3),dp(4),dp(3),dp(4))
                })
        }
        root.addView(row)
    }
    private fun frame(title:String,view:View)=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL
        background=shape(Color.rgb(251,253,255),11,border)
        setPadding(dp(9),dp(8),dp(9),dp(4))
        addView(label(title,12f,muted))
        addView(view,LinearLayout.LayoutParams(-1,dp(45)))
    }
    private fun numberInput(hintText:String)=EditText(this).apply {
        hint=hintText
        inputType=InputType.TYPE_CLASS_NUMBER
        textSize=15f
        setTextColor(ink)
        setHintTextColor(muted)
        gravity=Gravity.CENTER
        setSingleLine(true)
    }
    private fun currentList():InsuranceList?=lists.getOrNull(listPicker.selectedItemPosition)
    // Spinner position 0 is the label "انتخاب کارمند", not an employee.
    private fun selectedEmployee():Employee?=
        WorkDetailRules.fromPlaceholder(employees,employeePicker.selectedItemPosition)

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=SuraUi.charcoal
        loadLists()
        render()
    }
    private fun loadLists() {
        val found=mutableListOf<InsuranceList>()
        store.readableDatabase.rawQuery(
            """SELECT l.id,l.workshop_id,w.name,l.year,l.month,l.list_number
               FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id
               ORDER BY l.year DESC,l.month DESC,l.id DESC""",null
        ).use { c ->
            while(c.moveToNext())found.add(InsuranceList(
                c.getLong(0),c.getLong(1),c.getString(2),
                c.getInt(3),c.getInt(4),c.getInt(5)
            ))
        }
        lists=found
    }
    private fun render() {
        root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(15),dp(16),dp(15),dp(27))
            setBackgroundColor(SuraUi.canvas)
        }
        val scroll=ScrollView(this).apply {
            isFillViewport=true;clipToPadding=false;addView(root)
        }
        scroll.setOnApplyWindowInsetsListener { view,insets ->
            val top=if(android.os.Build.VERSION.SDK_INT>=30)
                insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
                else insets.systemWindowInsetTop
            val bottom=if(android.os.Build.VERSION.SDK_INT>=30)
                insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
                else insets.systemWindowInsetBottom
            view.setPadding(0,top,0,bottom+dp(12))
            insets
        }
        setContentView(SuraUi.mount(this,scroll,"details"))
        root.addView(SuraUi.banner(this,"ریزکارکرد کارکنان","روزهای کارکرد، دستمزد و مزایای مشمول","details"),
            LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})

        toolbar(
            Triple("جدید","＋",{newItem()}),
            Triple("ویرایش","✎",{editItem()}),
            Triple("حذف","×",{deleteItem()})
        )
        toolbar(
            Triple("ثبت","✓",{saveItem()}),
            Triple("لغو","↶",{resetForm()}),
            Triple("خروج","⌂",{finish()})
        )

        if(lists.isEmpty()) {
            root.addView(label(
                "هنوز لیست بیمه ماهانه‌ای ثبت نشده است. ابتدا در بخش «لیست بیمه» یک لیست ایجاد کنید.",
                14f,muted
            ).apply {setPadding(dp(10),dp(24),dp(10),dp(24))})
            return
        }

        root.addView(title("انتخاب لیست ماهانه"))
        listPicker=Spinner(this).apply {
            adapter=ArrayAdapter(this@WorkDetailActivity,
                android.R.layout.simple_spinner_dropdown_item,
                lists.map { "${it.workshop}  •  ${two(it.year%100)}/${two(it.month)}  •  ${two(it.number)}" }
            )
        }
        root.addView(frame("کارگاه، دوره و شماره لیست",listPicker),
            LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(14)})

        val searchRow=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER_VERTICAL
            background=shape(Color.WHITE,14,border)
            setPadding(dp(9),dp(5),dp(9),dp(5))
        }
        searchField=EditText(this).apply {
            hint="جست‌وجوی نام، شماره بیمه یا پرسنلی"
            textSize=13f
            setSingleLine(true)
            setTextColor(ink)
            setHintTextColor(muted)
        }
        searchRow.addView(searchField,LinearLayout.LayoutParams(0,dp(47),1f))
        searchRow.addView(action(if(SuraUi.isCompact(this)) "" else "جست‌وجو","⌕") {
            query=searchField.text.toString().trim()
            refreshRows()
        }.apply { contentDescription="جست‌وجوی ریزکارکرد" },LinearLayout.LayoutParams(dp(if(SuraUi.isCompact(this)) 54 else 107),dp(48)))
        root.addView(searchRow,LinearLayout.LayoutParams(-1,-2).apply {
            bottomMargin=dp(12)
        })

        root.addView(title("فهرست ریزکارکرد این لیست"))
        rowsCount=label("",12f,muted).apply {
            setPadding(0,0,0,dp(8))
        }
        root.addView(rowsCount)
        tableScroll=HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled=true
            background=shape(Color.WHITE,14,border)
        }
        table=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(4),dp(4),dp(4),dp(4))
        }
        tableScroll.addView(table)
        root.addView(tableScroll,LinearLayout.LayoutParams(-1,-2))
        root.addView(label("برای ویرایش، یک ردیف انتخاب کنید؛ جدول افقی پیمایش می‌شود.",
            12f,muted).apply {
            setPadding(0,dp(6),0,dp(12))
        })

        root.addView(title("ثبت و ویرایش کارکرد"))
        formStatus=label("",12f,muted).apply {
            setPadding(0,0,0,dp(8))
        }
        root.addView(formStatus)
        val editor=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=shape(Color.WHITE,14,border)
            setPadding(dp(12),dp(12),dp(12),dp(3))
        }
        employeePicker=Spinner(this)
        editor.addView(frame("انتخاب کارمند همین کارگاه *",employeePicker),
            LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(10)})
        daysField=numberInput("۰ تا ۳۱")
        wageField=numberInput("ریال")
        benefitField=numberInput("ریال")
        val formRow=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
        }
        listOf(
            frame("روزهای کارکرد *",daysField),
            frame("مزد روزانه *",wageField)
        ).forEach { frame ->
            if(SuraUi.isCompact(this)) {
                formRow.orientation=LinearLayout.VERTICAL
                formRow.addView(frame,LinearLayout.LayoutParams(-1,-2).apply {
                    bottomMargin=dp(8)
                })
            }else{
                formRow.addView(frame,LinearLayout.LayoutParams(0,-2,1f).apply {
                    leftMargin=dp(3);rightMargin=dp(3)
                })
            }
        }
        editor.addView(formRow,LinearLayout.LayoutParams(-1,-2).apply {
            bottomMargin=dp(10)
        })
        editor.addView(frame("مزایای مشمول ماهانه (ریال) *",benefitField),
            LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(11)})
        preview=label("جمع دستمزد و مزایا: —",14f,blue).apply {
            setTypeface(null,Typeface.BOLD)
            setPadding(dp(9),dp(12),dp(9),dp(12))
            background=shape(pale,9)
        }
        editor.addView(preview,LinearLayout.LayoutParams(-1,-2).apply {
            bottomMargin=dp(11)
        })
        root.addView(editor)

        root.addView(title("خلاصه ریزکارکرد ماهانه"))
        summary=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        root.addView(summary)
        root.addView(label(
            "این صفحه فقط دستمزد و مزایای ثبت‌شده را جمع می‌کند؛ محاسبات حق بیمه در بخش مستقل تکمیل می‌شوند.",
            12f,muted
        ).apply {setPadding(dp(5),dp(11),dp(5),dp(14))})

        val watcher=object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){}
            override fun afterTextChanged(s:Editable?){updatePreview()}
        }
        daysField.addTextChangedListener(watcher)
        wageField.addTextChangedListener(watcher)
        benefitField.addTextChangedListener(watcher)

        listPicker.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:android.widget.AdapterView<*>?){}
            override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:View?,position:Int,id:Long){
                changeList()
            }
        }
        changeList()
    }
    private fun changeList() {
        selectedItemId=null
        selectedEmployeeId=null
        employees=mutableListOf<Employee>().also { found ->
            val list=currentList()
            if(list!=null)store.readableDatabase.rawQuery(
                """SELECT id,first_name,last_name,insurance_no FROM employees
                   WHERE workshop_id=? ORDER BY last_name,first_name""",
                arrayOf(list.workshopId.toString())
            ).use { c->
                while(c.moveToNext())found.add(Employee(
                    c.getLong(0),"${c.getString(1)} ${c.getString(2)}",c.getString(3)
                ))
            }
        }
        employeePicker.adapter=ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("انتخاب کارمند")+employees.map {"${it.name}  •  ${it.insurance}"}
        )
        resetForm()
        refreshRows()
    }
    private fun enableEdit(value:Boolean,isNew:Boolean=false) {
        editing=value
        employeePicker.isEnabled=value && isNew
        daysField.isEnabled=value
        wageField.isEnabled=value
        benefitField.isEnabled=value
        updatePreview()
    }
    private fun resetForm() {
        if(!::employeePicker.isInitialized)return
        selectedItemId=null
        selectedEmployeeId=null
        employeePicker.setSelection(0)
        daysField.setText("")
        wageField.setText("")
        benefitField.setText("")
        enableEdit(false)
        formStatus.text=if(employees.isEmpty())
            "در این کارگاه کارمندی ثبت نشده است؛ ابتدا در بخش کارکنان، کارگاه فرد را انتخاب کنید."
            else "برای ثبت کارکرد جدید «جدید» و برای اصلاح یک سطر را انتخاب کنید."
        refreshRows()
    }
    private fun newItem() {
        if(!::employeePicker.isInitialized){toast("ابتدا لیست ماهانه ثبت کنید");return}
        resetForm()
        if(employees.isEmpty()){toast("کارمند مرتبط با این کارگاه پیدا نشد");return}
        enableEdit(true,isNew=true)
        benefitField.setText("0")
        formStatus.text="کارمند و ارقام ماهانه را وارد کنید، سپس «ثبت» را بزنید."
    }
    private fun editItem() {
        if(!::employeePicker.isInitialized){toast("لیست ماهانه‌ای موجود نیست");return}
        if(selectedItemId==null){toast("ابتدا یک ردیف را از جدول انتخاب کنید");return}
        val selected=selectedEmployeeId
        if(selected==null || employees.none{it.id==selected}){
            toast("کارمند این ردیف با کارگاه لیست هماهنگ نیست؛ ابتدا داده قدیمی را بررسی کنید")
            return
        }
        enableEdit(true,isNew=false)
        formStatus.text="ویرایش کارکرد فعال است؛ بعد از اصلاح، «ثبت» را بزنید."
    }
    private fun selectItem(item:Item) {
        selectedItemId=item.id
        selectedEmployeeId=item.employeeId
        daysField.setText(item.days.toString())
        wageField.setText(item.dailyWage.toString())
        benefitField.setText(item.benefits.toString())
        val index=employees.indexOfFirst{it.id==item.employeeId}
        employeePicker.setSelection(index+1)
        enableEdit(false)
        formStatus.text=if(index<0)
            "این ردیف متعلق به کارمند همین کارگاه نیست؛ برای حفاظت از سوابق، ویرایش آن مسدود است."
            else "ردیف انتخاب شد؛ برای تغییر مقادیر، دکمه «ویرایش» را بزنید."
        refreshRows()
    }
    private fun updatePreview() {
        if(!::preview.isInitialized || !::daysField.isInitialized)return
        val d=daysField.text.toString()
        val w=wageField.text.toString()
        val b=benefitField.text.toString()
        val month=currentList()?.month ?: 0
        val result=WorkDetailRules.calculate(d,w,b,month)
        preview.text=if(result==null)"جمع دستمزد و مزایا: —"
            else "جمع ماهانه: ${amount(result.total)} ریال"
    }
    private fun saveItem() {
        if(!::employeePicker.isInitialized || !editing){
            toast("ابتدا «جدید» یا «ویرایش» را انتخاب کنید")
            return
        }
        val list=currentList() ?: run{toast("لیست ماهانه موجود نیست");return}
        val employee=selectedEmployee() ?: run{toast("کارمند را انتخاب کنید");return}
        val computed=WorkDetailRules.calculate(
            daysField.text.toString(),wageField.text.toString(),
            benefitField.text.toString(),list.month
        ) ?: run{toast("روز کارکرد یا مبالغ معتبر نیستند؛ سقف روزها را بررسی کنید");return}
        if(selectedItemId!=null && employee.id!=selectedEmployeeId){
            toast("کارمند ردیف ثبت‌شده قابل تغییر نیست");return
        }
        val db=store.writableDatabase
        val valid=db.rawQuery(
            """SELECT 1 FROM employees e JOIN insurance_lists l
               ON l.workshop_id=e.workshop_id WHERE e.id=? AND l.id=? LIMIT 1""",
            arrayOf(employee.id.toString(),list.id.toString())
        ).use{it.moveToFirst()}
        if(!valid){toast("این کارمند متعلق به کارگاه لیست نیست");return}
        val id=selectedItemId
        val duplicate=db.rawQuery(
            "SELECT 1 FROM insurance_list_items WHERE list_id=? AND employee_id=? AND id<>? LIMIT 1",
            arrayOf(list.id.toString(),employee.id.toString(),(id?:-1L).toString())
        ).use{it.moveToFirst()}
        if(duplicate){toast("برای این کارمند قبلاً کارکرد ثبت شده است؛ سطر او را ویرایش کنید");return}

        val data=ContentValues().apply {
            put("list_id",list.id)
            put("employee_id",employee.id)
            put("days",computed.days)
            put("daily_wage",computed.dailyWage)
            put("benefits",computed.benefits)
        }
        try {
            db.beginTransaction()
            var savedId:Long?=null
            try {
                savedId=if(id==null)db.insertOrThrow("insurance_list_items",null,data)
                else {
                    if(db.update("insurance_list_items",data,
                        "id=? AND list_id=?",arrayOf(id.toString(),list.id.toString()))!=1)
                        throw IllegalStateException("ردیف انتخاب‌شده یافت نشد")
                    id
                }
                db.setTransactionSuccessful()
            }finally{db.endTransaction()}
            val rowId=savedId
            resetForm()
            if(rowId!=null)loadItemById(rowId)
            refreshRows()
            toast("ریزکارکرد با موفقیت ذخیره شد")
        }catch(ex:Exception){
            toast("خطا در ثبت ریزکارکرد؛ اطلاعات تکراری یا نامعتبر است")
        }
    }
    private fun loadItemById(id:Long){
        val list=currentList() ?:return
        store.readableDatabase.rawQuery(
            """SELECT i.id,i.employee_id,COALESCE(e.first_name,''),
                      COALESCE(e.last_name,''),COALESCE(e.insurance_no,''),
                      i.days,i.daily_wage,i.benefits,e.workshop_id
               FROM insurance_list_items i LEFT JOIN employees e ON e.id=i.employee_id
               WHERE i.id=? AND i.list_id=?""",
            arrayOf(id.toString(),list.id.toString())
        ).use{c->
            if(c.moveToFirst())selectItem(Item(
                c.getLong(0),c.getLong(1),
                "${c.getString(2)} ${c.getString(3)}",
                c.getString(4),c.getInt(5),c.getLong(6),c.getLong(7),
                if(c.isNull(8))null else c.getLong(8)
            ))
        }
    }
    private fun deleteItem() {
        if(!::employeePicker.isInitialized){toast("لیستی وجود ندارد");return}
        val itemId=selectedItemId ?: run{toast("ابتدا یک ردیف از جدول انتخاب کنید");return}
        val listId=currentList()?.id ?:return
        AlertDialog.Builder(this)
            .setTitle("حذف ریزکارکرد")
            .setMessage("این ردیف کارکرد از لیست ماهانه حذف خواهد شد. ادامه می‌دهید؟")
            .setPositiveButton("حذف"){_,_->
                try {
                    val removed=store.writableDatabase.delete("insurance_list_items",
                        "id=? AND list_id=?",arrayOf(itemId.toString(),listId.toString()))
                    if(removed==1){resetForm();toast("ریزکارکرد حذف شد")}
                    else toast("ردیف پیدا نشد")
                }catch(ex:Exception){toast("حذف ریزکارکرد انجام نشد")}
            }.setNegativeButton("انصراف",null).show()
    }
    private fun cell(s:String,width:Int,header:Boolean)=
        label(s,if(header)12f else 12.5f,if(header)Color.WHITE else ink).apply {
            gravity=Gravity.CENTER
            maxLines=2
            ellipsize=TextUtils.TruncateAt.END
            minHeight=dp(53)
            setPadding(dp(5),dp(9),dp(5),dp(9))
            if(header)setTypeface(null,Typeface.BOLD)
            layoutParams=LinearLayout.LayoutParams(dp(width),-2)
        }
    private fun tableRow(columns:List<String>,header:Boolean=false,chosen:Boolean=false,
        index:Int=0,invalid:Boolean=false,callback:(()->Unit)?=null)=
        LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER_VERTICAL
            val background=when {
                header->blue
                invalid->Color.rgb(255,240,229)
                chosen->Color.rgb(210,231,255)
                index%2==0->Color.WHITE
                else->SuraUi.pastel
            }
            this.background=shape(background,7,if(chosen)Color.rgb(55,128,210) else 0)
            val widths=listOf(148,105,65,132,132,147)
            columns.forEachIndexed { n,value->addView(cell(value,widths[n],header)) }
            if(callback!=null){
                SuraUi.touch(this@WorkDetailActivity,this,7)
                setOnClickListener{callback()}
                isFocusable=true
                contentDescription="انتخاب کارکرد ${columns.joinToString("، ")}"
            }
        }
    private fun infoRows(pairs:List<Pair<String,String>>):LinearLayout {
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=shape(Color.WHITE,13,border)
            setPadding(dp(5),dp(5),dp(5),dp(5))
        }
        pairs.forEachIndexed { index,(key,value)->
            val line=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                layoutDirection=View.LAYOUT_DIRECTION_RTL
                gravity=Gravity.CENTER_VERTICAL
                background=shape(if(index%2==0)Color.WHITE else pale,7)
            }
            line.addView(label(key,13f,muted).apply{
                setPadding(dp(10),dp(10),dp(5),dp(10))
            },LinearLayout.LayoutParams(0,-2,1f))
            line.addView(label(value,13f,ink).apply{
                if(key=="جمع دستمزد و مزایا")setTypeface(null,Typeface.BOLD)
                setPadding(dp(5),dp(10),dp(10),dp(10))
            },LinearLayout.LayoutParams(0,-2,1.25f))
            box.addView(line)
        }
        return box
    }
    private fun refreshRows() {
        if(!::table.isInitialized)return
        val list=currentList() ?:return
        val sx=tableScroll.scrollX
        table.removeAllViews()
        table.addView(tableRow(
            listOf("کارمند","شماره بیمه","روز","مزد روزانه","مزایای مشمول","جمع ماهانه"),header=true))
        var visible=0
        var people=0L
        var daysTotal=0L
        var wagesTotal=0L
        var benefitsTotal=0L
        var grandTotal=0L
        var mismatch=0
        var overflow=false
        val needle=EmployeeRules.normalizeDigits(query.trim())
        store.readableDatabase.rawQuery(
            """SELECT i.id,i.employee_id,COALESCE(e.first_name,''),
                      COALESCE(e.last_name,''),COALESCE(e.insurance_no,''),
                      i.days,i.daily_wage,i.benefits,e.workshop_id,
                      COALESCE(e.personnel_no,'')
               FROM insurance_list_items i
               LEFT JOIN employees e ON e.id=i.employee_id
               WHERE i.list_id=? ORDER BY e.last_name,e.first_name,i.id""",
            arrayOf(list.id.toString())
        ).use{c->
            while(c.moveToNext()){
                val item=Item(c.getLong(0),c.getLong(1),
                    "${c.getString(2)} ${c.getString(3)}".trim(),
                    c.getString(4),c.getInt(5),c.getLong(6),c.getLong(7),
                    if(c.isNull(8))null else c.getLong(8),c.getString(9))
                val incompatible=item.workshopId!=list.workshopId
                if(incompatible)mismatch++
                try {
                    people=Math.addExact(people,1L)
                    daysTotal=Math.addExact(daysTotal,item.days.toLong())
                    wagesTotal=Math.addExact(wagesTotal,
                        Math.multiplyExact(item.days.toLong(),item.dailyWage))
                    benefitsTotal=Math.addExact(benefitsTotal,item.benefits)
                    grandTotal=Math.addExact(wagesTotal,benefitsTotal)
                }catch(ex:ArithmeticException){overflow=true}
                val name=item.name.ifEmpty{"کارمند ناموجود"}
                if(!WorkDetailRules.matchesSearch(
                    name,item.insurance,item.personnelNumber,item.employeeId,needle
                ))continue
                val total=try {
                    amount(Math.addExact(Math.multiplyExact(item.days.toLong(),item.dailyWage),
                        item.benefits))
                }catch(ex:ArithmeticException){"خطای محاسبه"}
                table.addView(tableRow(listOf(
                    (if(incompatible)"⚠ " else "")+name,
                    item.insurance,
                    item.days.toString(),
                    amount(item.dailyWage),
                    amount(item.benefits),
                    total
                ),chosen=selectedItemId==item.id,index=visible,invalid=incompatible,
                    callback={selectItem(item)}))
                visible++
            }
        }
        if(visible==0)table.addView(label(
            if(needle.isEmpty())"برای این لیست هنوز ریزکارکردی ثبت نشده است."
                else "نتیجه‌ای مطابق جست‌وجو پیدا نشد.",
            13f,muted).apply {setPadding(dp(10),dp(19),dp(10),dp(19))})
        rowsCount.text="${visible} ردیف نمایش داده‌شده  •  ${people} کارمند در لیست"
        tableScroll.post {tableScroll.scrollTo(sx,0)}
        summary.removeAllViews()
        summary.addView(infoRows(listOf(
            "تعداد کارکنان لیست" to people.toString(),
            "جمع روزهای کارکرد" to daysTotal.toString(),
            "جمع دستمزد ماهانه" to (if(overflow)"خطای محاسبات" else "${amount(wagesTotal)} ریال"),
            "جمع مزایای مشمول" to (if(overflow)"خطای محاسبات" else "${amount(benefitsTotal)} ریال"),
            "جمع دستمزد و مزایا" to (if(overflow)"خطای محاسبات" else "${amount(grandTotal)} ریال")
        )))
        if(mismatch>0)summary.addView(label(
            "هشدار: $mismatch ردیف قدیمی به کارمند این کارگاه متصل نیست. برای جلوگیری از تغییر نادرست سوابق، ویرایش این ردیف‌ها محدود شده است.",
            12f,danger).apply{setPadding(dp(6),dp(11),dp(6),dp(6))})
        if(overflow)summary.addView(label(
            "جمع مبالغ از محدوده عددی مجاز بیشتر است؛ داده‌ها باید بررسی شوند.",
            12f,danger))
    }
}
