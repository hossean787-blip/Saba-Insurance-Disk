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

    class Store(activity: Activity): SQLiteOpenHelper(activity,"insurance.db",null,1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE workshops (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, code TEXT NOT NULL)")
            db.execSQL("CREATE TABLE employees (id INTEGER PRIMARY KEY AUTOINCREMENT, workshop_id INTEGER NOT NULL, name TEXT NOT NULL, national_id TEXT NOT NULL, insurance_no TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
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
        page("لیست دیسک صبا")
        text("نسخه ۰.۲ • اطلاعات روی همین گوشی ذخیره می‌شود")
        button("کارگاه‌ها") { workshops() }
        button("کارکنان") { employees() }
        button("لیست ماهانه (در دست توسعه)") { notice() }
        button("خروجی DBF (در دست توسعه)") { notice() }
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
