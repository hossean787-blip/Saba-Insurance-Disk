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

    class Store(activity: Activity): SQLiteOpenHelper(activity,"insurance.db",null,2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE workshops (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, code TEXT NOT NULL, contract_row TEXT NOT NULL DEFAULT '000', employer TEXT NOT NULL DEFAULT '', address TEXT NOT NULL DEFAULT '', insurance_rate REAL NOT NULL DEFAULT 0)")
            db.execSQL("CREATE TABLE employees (id INTEGER PRIMARY KEY AUTOINCREMENT, workshop_id INTEGER NOT NULL, name TEXT NOT NULL, national_id TEXT NOT NULL, insurance_no TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if(oldVersion<2) {
                db.execSQL("ALTER TABLE workshops ADD COLUMN contract_row TEXT NOT NULL DEFAULT '000'")
                db.execSQL("ALTER TABLE workshops ADD COLUMN employer TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workshops ADD COLUMN address TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workshops ADD COLUMN insurance_rate REAL NOT NULL DEFAULT 0")
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
        window.statusBarColor=Color.rgb(4,43,99)
        val shell=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(241,246,254))
        }
        val header=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            gravity=android.view.Gravity.CENTER_VERTICAL
            setPadding(22,38,22,32)
            setBackgroundColor(Color.rgb(6,53,117))
        }
        header.addView(ImageView(this).apply {
            setImageResource(R.drawable.saba_launcher)
            layoutParams=LinearLayout.LayoutParams(72,72)
        })
        val heading=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(18,0,0,0) }
        heading.addView(TextView(this).apply { text="صبا";textSize=33f;setTextColor(Color.WHITE);setTypeface(null,1) })
        heading.addView(TextView(this).apply { text="لیست دیسک تأمین اجتماعی";textSize=14f;setTextColor(Color.rgb(217,234,255)) })
        header.addView(heading)
        shell.addView(header)
        val scroll=ScrollView(this)
        val menu=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(22,24,22,24) }
        scroll.addView(menu)
        shell.addView(scroll)
        fun tile(title:String,subtitle:String,symbol:String,bg:Int,fg:Int,click:()->Unit) {
            val card=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                gravity=android.view.Gravity.CENTER_VERTICAL
                setPadding(22,23,22,23)
                background=android.graphics.drawable.GradientDrawable().apply {
                    setColor(bg);cornerRadius=22f
                    setStroke(2,Color.rgb(215,225,239))
                }
                elevation=5f
                setOnClickListener { click() }
            }
            val mark=TextView(this).apply { text=symbol;textSize=28f;setTextColor(fg);gravity=android.view.Gravity.CENTER }
            card.addView(mark,LinearLayout.LayoutParams(64,76))
            val labels=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(12,0,0,0) }
            labels.addView(TextView(this).apply { text=title;textSize=20f;setTypeface(null,1);setTextColor(fg) })
            labels.addView(TextView(this).apply { text=subtitle;textSize=13f;setTextColor(Color.rgb(78,97,126)) })
            card.addView(labels,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
            card.addView(TextView(this).apply { text="‹";textSize=28f;setTextColor(fg) })
            menu.addView(card,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin=16 })
        }
        tile("کارگاه‌ها","مدیریت مشخصات کارگاه‌ها","▣",Color.rgb(220,239,255),Color.rgb(4,92,188)) {
            startActivity(android.content.Intent(this,WorkshopActivity::class.java))
        }
        tile("کارکنان","مدیریت اطلاعات کارکنان","♟",Color.rgb(224,249,239),Color.rgb(6,130,83)) { employees() }
        tile("لیست بیمه","ثبت و تهیه لیست ماهانه • در دست توسعه","▤",Color.rgb(255,240,219),Color.rgb(188,106,0)) { notice() }
        tile("تنظیمات","پیکربندی برنامه • در دست توسعه","⚙",Color.rgb(236,230,255),Color.rgb(98,69,194)) { notice() }
        tile("درباره برنامه","صبا • نسخه آزمایشی ۰.۳","ⓘ",Color.rgb(233,242,255),Color.rgb(61,94,151)) {
            android.app.AlertDialog.Builder(this).setTitle("صبا").setMessage("نرم‌افزار لیست دیسک بیمه — نسخه آزمایشی. خروجی DBF هنوز آماده نیست.").setPositiveButton("باشه",null).show()
        }
        shell.setOnApplyWindowInsetsListener { view, insets ->
            val top=if(android.os.Build.VERSION.SDK_INT>=30) insets.getInsets(android.view.WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom=if(android.os.Build.VERSION.SDK_INT>=30) insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(0,top,0,bottom+24)
            insets
        }
        setContentView(shell)
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
