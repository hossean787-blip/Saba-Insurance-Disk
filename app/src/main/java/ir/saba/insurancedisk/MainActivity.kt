package ir.saba.insurancedisk

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var db: Store
    private lateinit var root: LinearLayout
    private val navy = Color.rgb(14,25,45)

    class Store(activity: Activity): SQLiteOpenHelper(activity,"insurance.db",null,5) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE workshops (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, code TEXT NOT NULL, contract_row TEXT NOT NULL DEFAULT '000', employer TEXT NOT NULL DEFAULT '', address TEXT NOT NULL DEFAULT '', insurance_rate REAL NOT NULL DEFAULT 0)")
            db.execSQL("CREATE TABLE employees (id INTEGER PRIMARY KEY AUTOINCREMENT, workshop_id INTEGER, name TEXT NOT NULL DEFAULT '', national_id TEXT NOT NULL DEFAULT '', insurance_no TEXT NOT NULL DEFAULT '', personnel_no TEXT NOT NULL DEFAULT '', first_name TEXT NOT NULL DEFAULT '', last_name TEXT NOT NULL DEFAULT '', father_name TEXT NOT NULL DEFAULT '', birth_cert_no TEXT NOT NULL DEFAULT '', birth_date TEXT NOT NULL DEFAULT '', issue_city TEXT NOT NULL DEFAULT '', gender TEXT NOT NULL DEFAULT '', nationality TEXT NOT NULL DEFAULT '', job_title TEXT NOT NULL DEFAULT '', job_code TEXT NOT NULL DEFAULT '')")
            db.execSQL("CREATE TABLE insurance_lists (id INTEGER PRIMARY KEY AUTOINCREMENT, workshop_id INTEGER NOT NULL, year INTEGER NOT NULL, month INTEGER NOT NULL, list_number INTEGER NOT NULL DEFAULT 1, UNIQUE(workshop_id,year,month))")
            db.execSQL("CREATE TABLE insurance_list_items (id INTEGER PRIMARY KEY AUTOINCREMENT, list_id INTEGER NOT NULL, employee_id INTEGER NOT NULL, days INTEGER NOT NULL, daily_wage INTEGER NOT NULL, benefits INTEGER NOT NULL DEFAULT 0, UNIQUE(list_id,employee_id))")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if(oldVersion<2) {
                db.execSQL("ALTER TABLE workshops ADD COLUMN contract_row TEXT NOT NULL DEFAULT '000'")
                db.execSQL("ALTER TABLE workshops ADD COLUMN employer TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workshops ADD COLUMN address TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workshops ADD COLUMN insurance_rate REAL NOT NULL DEFAULT 0")
            }
            if(oldVersion<3) {
                val additions=listOf("personnel_no","first_name","last_name","father_name","birth_cert_no","birth_date","issue_city","gender","nationality","job_title","job_code")
                additions.forEach { db.execSQL("ALTER TABLE employees ADD COLUMN $it TEXT NOT NULL DEFAULT ''") }
                db.execSQL("UPDATE employees SET personnel_no=printf('%06d',id), first_name=name WHERE personnel_no=''")
            }
            if(oldVersion<4) {
                db.execSQL("CREATE TABLE insurance_lists (id INTEGER PRIMARY KEY AUTOINCREMENT, workshop_id INTEGER NOT NULL, year INTEGER NOT NULL, month INTEGER NOT NULL, UNIQUE(workshop_id,year,month))")
                db.execSQL("CREATE TABLE insurance_list_items (id INTEGER PRIMARY KEY AUTOINCREMENT, list_id INTEGER NOT NULL, employee_id INTEGER NOT NULL, days INTEGER NOT NULL, daily_wage INTEGER NOT NULL, benefits INTEGER NOT NULL DEFAULT 0, UNIQUE(list_id,employee_id))")
            }
            if(oldVersion<5) {
                db.execSQL("ALTER TABLE insurance_lists ADD COLUMN list_number INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE insurance_lists SET list_number=(SELECT COUNT(*) FROM insurance_lists previous WHERE previous.workshop_id=insurance_lists.workshop_id AND previous.year=insurance_lists.year AND (previous.month<insurance_lists.month OR (previous.month=insurance_lists.month AND previous.id<=insurance_lists.id)))")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db=Store(this)
        home()
    }

    private fun page(title:String) {
        root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(28,32,28,20)
            setBackgroundColor(navy)
        }
        val scroll=ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        text(title,25f)
    }

    private fun text(value:String,size:Float=16f) {
        root.addView(TextView(this).apply {
            text=value; textSize=size; setTextColor(Color.WHITE); setPadding(4,12,4,12)
        })
    }

    private fun button(label:String,action:()->Unit) {
        root.addView(Button(this).apply { text=label; isAllCaps=false; setOnClickListener { action() } })
    }

    private fun input(hint:String):EditText = EditText(this).apply {
        this.hint=hint; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
    }

    private fun home() {
        window.statusBarColor=SuraUi.charcoal
        fun dp(v:Int)=(v*resources.displayMetrics.density+0.5f).toInt()
        val content=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16),dp(19),dp(16),dp(27))
            setBackgroundColor(SuraUi.canvas)
        }
        val scroll=ScrollView(this).apply{
            isFillViewport=true
            clipToPadding=false
            addView(content)
            setOnApplyWindowInsetsListener{view,insets->
                val top=if(android.os.Build.VERSION.SDK_INT>=30)
                    insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
                    else insets.systemWindowInsetTop
                view.setPadding(0,top,0,0);insets
            }
        }
        setContentView(SuraUi.mount(this,scroll,"home"))
        val hero=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            background=SuraUi.gradient(this@MainActivity,24)
            elevation=dp(5).toFloat()
            setPadding(dp(20),dp(20),dp(20),dp(19))
        }
        val brand=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=android.view.Gravity.CENTER_VERTICAL
        }
        val heading=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        heading.addView(SuraUi.label(this,"سورا",30f,Color.WHITE,true))
        heading.addView(SuraUi.label(this,"دستیار لیست بیمه تأمین اجتماعی",12f,
            Color.rgb(226,231,255)).apply{setPadding(0,dp(5),0,0)})
        brand.addView(heading,LinearLayout.LayoutParams(0,-2,1f))
        val logo=ImageView(this).apply{
            setImageResource(R.drawable.sura_launcher)
            scaleType=ImageView.ScaleType.FIT_CENTER
            background=SuraUi.surface(this@MainActivity,Color.rgb(71,76,169),
                15,Color.TRANSPARENT)
            setPadding(dp(5),dp(5),dp(5),dp(5))
        }
        brand.addView(logo,LinearLayout.LayoutParams(dp(60),dp(60)).apply{
            leftMargin=dp(8)
        })
        hero.addView(brand)
        hero.addView(SuraUi.label(this,
            "مدیریت دقیق کارگاه، کارکنان و لیست‌های ماهانه",
            12.5f,Color.rgb(235,238,255)).apply{setPadding(0,dp(19),0,dp(14))})
        val cta=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=android.view.Gravity.CENTER
            background=SuraUi.surface(this@MainActivity,Color.WHITE,13,Color.TRANSPARENT)
            setOnClickListener{
                startActivity(android.content.Intent(this@MainActivity,InsuranceListActivity::class.java))
            }
        }
        cta.addView(SuraUi.glyph(this,"list",SuraUi.primary,20))
        cta.addView(SuraUi.label(this,"مدیریت لیست ماهانه",
            14f,SuraUi.primary,true).apply{setPadding(dp(9),0,0,0)})
        hero.addView(cta,LinearLayout.LayoutParams(-1,dp(48)))
        content.addView(hero,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(15)})

        fun count(table:String):Long=try{
            db.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table",null).use{
                if(it.moveToFirst())it.getLong(0) else 0L
            }
        }catch(ex:Exception){0L}
        fun metric(value:Long,title:String,icon:String):View{
            val box=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL
                background=SuraUi.surface(this@MainActivity)
                elevation=dp(1).toFloat()
                setPadding(dp(13),dp(14),dp(13),dp(13))
            }
            val row=LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL
                layoutDirection=View.LAYOUT_DIRECTION_RTL
                gravity=android.view.Gravity.CENTER_VERTICAL
            }
            val badge=LinearLayout(this).apply{
                gravity=android.view.Gravity.CENTER
                background=SuraUi.surface(this@MainActivity,SuraUi.pastel,
                    12,Color.TRANSPARENT)
            }
            badge.addView(SuraUi.glyph(this,icon,SuraUi.primary,19))
            row.addView(badge,LinearLayout.LayoutParams(dp(39),dp(39)))
            row.addView(SuraUi.label(this,value.toString(),24f,SuraUi.title,true).apply{
                setPadding(dp(9),0,0,0)
            })
            box.addView(row)
            box.addView(SuraUi.label(this,title,12f,SuraUi.muted).apply{
                setPadding(0,dp(11),0,0)
            })
            return box
        }
        fun pair(right:View,left:View,height:Int=0) {
            val row=LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL
                layoutDirection=View.LAYOUT_DIRECTION_RTL
            }
            row.addView(right,LinearLayout.LayoutParams(0,if(height==0)-2 else dp(height),1f).apply{
                leftMargin=dp(5)
            })
            row.addView(left,LinearLayout.LayoutParams(0,if(height==0)-2 else dp(height),1f).apply{
                rightMargin=dp(5)
            })
            content.addView(row,LinearLayout.LayoutParams(-1,-2).apply{
                bottomMargin=dp(10)
            })
        }
        content.addView(SuraUi.section(this,"نمای کلی فعالیت‌ها"))
        pair(
            metric(count("workshops"),"کارگاه ثبت‌شده","building"),
            metric(count("employees"),"کارمند ثبت‌شده","users")
        )
        pair(
            metric(count("insurance_lists"),"لیست ماهانه","list"),
            metric(count("insurance_list_items"),"ریزکارکرد ثبت‌شده","details")
        )
        fun feature(name:String,caption:String,icon:String,
            target:Class<out Activity>,pdf:Boolean=false):View{
            val card=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL
                setPadding(dp(13),dp(14),dp(13),dp(12))
                background=SuraUi.surface(this@MainActivity)
                elevation=dp(2).toFloat()
                isClickable=true;isFocusable=true
                setOnClickListener{
                    val next=android.content.Intent(this@MainActivity,target)
                    if(pdf)next.putExtra("pdf",true)
                    startActivity(next)
                }
            }
            val mark=LinearLayout(this).apply{
                gravity=android.view.Gravity.CENTER
                background=SuraUi.surface(this@MainActivity,SuraUi.pastel,14,Color.TRANSPARENT)
            }
            mark.addView(SuraUi.glyph(this,icon,SuraUi.primary,24))
            card.addView(mark,LinearLayout.LayoutParams(dp(47),dp(47)))
            card.addView(SuraUi.label(this,name,15.5f,SuraUi.title,true).apply{
                setPadding(0,dp(11),0,0)
            })
            card.addView(SuraUi.label(this,caption,11.5f,SuraUi.muted).apply{
                maxLines=2;setPadding(0,dp(5),0,0)
            })
            return card
        }
        content.addView(SuraUi.section(this,"دسترسی سریع"))
        pair(
            feature("کارگاه‌ها","مدیریت مشخصات کارگاه","building",WorkshopActivity::class.java),
            feature("کارکنان","ثبت و مدیریت پرسنل","users",EmployeeActivity::class.java),155
        )
        pair(
            feature("لیست ماهانه","مدیریت دوره‌های بیمه","list",InsuranceListActivity::class.java),
            feature("ریزکارکرد","کارکرد، مزد و مزایا","details",WorkDetailActivity::class.java),155
        )
        pair(
            feature("نمایش لیست","مشاهده اطلاعات ثبت‌شده","eye",InsurancePreviewActivity::class.java),
            feature("خروجی PDF","گزارش و اشتراک‌گذاری","report",InsuranceReportActivity::class.java,true),155
        )
        content.addView(SuraUi.section(this,"آخرین فعالیت"))
        val latest=try{
            db.readableDatabase.rawQuery(
                """SELECT w.name,l.year,l.month,COUNT(i.id)
                   FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id
                   LEFT JOIN insurance_list_items i ON i.list_id=l.id
                   GROUP BY l.id ORDER BY l.year DESC,l.month DESC,l.id DESC LIMIT 1""",null
            ).use{c->
                if(c.moveToFirst())
                    "${c.getString(0)} • ${c.getInt(1)}/${c.getInt(2).toString().padStart(2,'0')} • ${c.getInt(3)} نفر"
                else null
            }
        }catch(ex:Exception){null}
        val activity=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=android.view.Gravity.CENTER_VERTICAL
            background=SuraUi.surface(this@MainActivity)
            setPadding(dp(15),dp(16),dp(15),dp(16))
            isClickable=true
            setOnClickListener{
                startActivity(android.content.Intent(this@MainActivity,InsuranceListActivity::class.java))
            }
        }
        val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        info.addView(SuraUi.label(this,latest?:"هنوز لیستی ثبت نشده است",
            13.5f,SuraUi.title,true))
        info.addView(SuraUi.label(this,
            if(latest==null)"برای آغاز، اولین لیست ماهانه را ثبت کنید."
            else "برای مشاهده جزئیات وارد لیست‌ها شوید.",
            11.5f,SuraUi.muted).apply{setPadding(0,dp(5),0,0)})
        activity.addView(info,LinearLayout.LayoutParams(0,-2,1f))
        activity.addView(SuraUi.glyph(this,"list",SuraUi.primary,24))
        content.addView(activity)
        val about=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=android.view.Gravity.CENTER_VERTICAL
            background=SuraUi.surface(this@MainActivity,SuraUi.pastel,14,Color.TRANSPARENT)
            setPadding(dp(14),dp(13),dp(14),dp(13))
            isClickable=true
            setOnClickListener{
                val v=packageManager.getPackageInfo(packageName,0).versionName
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("درباره سورا")
                    .setMessage("سورا • نسخه آزمایشی $v\n\n" +
                        "مدیریت کارگاه‌ها، کارکنان، لیست و ریزکارکرد بیمه\n" +
                        "خروجی PDF: گزارش داخلی\n" +
                        "محاسبه حق بیمه و خروجی DBF هنوز تکمیل نشده‌اند.")
                    .setPositiveButton("بستن",null).show()
            }
        }
        about.addView(SuraUi.glyph(this,"shield",SuraUi.primary,22))
        about.addView(SuraUi.label(this,"درباره سورا و وضعیت نسخه آزمایشی",
            12f,SuraUi.primary,true).apply{setPadding(dp(10),0,0,0)})
        content.addView(about,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(18)})
    }

    private fun notice() = Toast.makeText(this,"این قابلیت هنوز آماده نیست",Toast.LENGTH_LONG).show()

    private fun workshops() {
        page("کارگاه‌ها")
        button("افزودن کارگاه") {
            val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,12,24,12) }
            val name=input("نام کارگاه")
            val code=input("کد کارگاه")
            layout.addView(name); layout.addView(code)
            AlertDialog.Builder(this).setTitle("کارگاه جدید").setView(layout)
                .setPositiveButton("ذخیره") { _,_ ->
                    if(name.text.isBlank() || code.text.isBlank()) { noticeInvalid(); return@setPositiveButton }
                    val values=ContentValues().apply { put("name",name.text.toString().trim());put("code",code.text.toString().trim()) }
                    db.writableDatabase.insertOrThrow("workshops",null,values)
                    workshops()
                }.setNegativeButton("انصراف",null).show()
        }
        val c=db.readableDatabase.rawQuery("SELECT id,name,code FROM workshops ORDER BY id DESC",null)
        c.use {
            while(it.moveToNext()) {
                val id=it.getLong(0)
                text("${it.getString(1)} • ${it.getString(2)}")
                button("کارکنان این کارگاه") { employees(id) }
            }
        }
        button("بازگشت") { home() }
    }

    private fun noticeInvalid() = Toast.makeText(this,"تمام فیلدها را تکمیل کنید",Toast.LENGTH_SHORT).show()

    private fun employees(workshopId:Long?=null) {
        page("کارکنان")
        button("افزودن کارمند") {
            val workshops=mutableListOf<Pair<Long,String>>()
            db.readableDatabase.rawQuery("SELECT id,name FROM workshops ORDER BY name",null).use { c ->
                while(c.moveToNext()) workshops.add(c.getLong(0) to c.getString(1))
            }
            if(workshops.isEmpty()) {
                Toast.makeText(this,"ابتدا یک کارگاه ثبت کنید",Toast.LENGTH_LONG).show()
                return@button
            }
            val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12) }
            val spinner=Spinner(this)
            spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,workshops.map { it.second })
            val selected=workshops.indexOfFirst { it.first==workshopId }
            if(selected>=0) spinner.setSelection(selected)
            layout.addView(spinner)
            val name=input("نام و نام خانوادگی")
            val national=input("کد ملی ۱۰ رقمی")
            val insurance=input("شماره بیمه")
            layout.addView(name);layout.addView(national);layout.addView(insurance)
            AlertDialog.Builder(this).setTitle("کارمند جدید").setView(layout)
                .setPositiveButton("ذخیره") { _,_ ->
                    val nationalId=national.text.toString().trim()
                    if(name.text.isBlank() || !nationalId.matches(Regex("[0-9]{10}")) || insurance.text.isBlank()) {
                        Toast.makeText(this,"نام، کد ملی ۱۰ رقمی و شماره بیمه الزامی است",Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    val values=ContentValues().apply {
                        put("workshop_id",workshops[spinner.selectedItemPosition].first)
                        put("name",name.text.toString().trim())
                        put("national_id",nationalId)
                        put("insurance_no",insurance.text.toString().trim())
                    }
                    db.writableDatabase.insertOrThrow("employees",null,values)
                    employees(workshopId)
                }.setNegativeButton("انصراف",null).show()
        }
        val sql=if(workshopId==null)
            "SELECT e.name,e.national_id,w.name FROM employees e JOIN workshops w ON w.id=e.workshop_id ORDER BY e.id DESC"
        else "SELECT e.name,e.national_id,w.name FROM employees e JOIN workshops w ON w.id=e.workshop_id WHERE w.id=? ORDER BY e.id DESC"
        val args=if(workshopId==null) null else arrayOf(workshopId.toString())
        db.readableDatabase.rawQuery(sql,args).use { c ->
            while(c.moveToNext()) text("${c.getString(0)} | ${c.getString(1)} | ${c.getString(2)}")
        }
        button("بازگشت") { home() }
    }
}
