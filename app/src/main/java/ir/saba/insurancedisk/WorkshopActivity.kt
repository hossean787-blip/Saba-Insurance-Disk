package ir.saba.insurancedisk

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.ContentValues
import android.graphics.Color
import android.text.InputType
import android.view.View
import android.widget.*

class WorkshopActivity : Activity() {
    private lateinit var store: MainActivity.Store
    private lateinit var root: LinearLayout
    private lateinit var code: EditText
    private lateinit var contract: EditText
    private lateinit var name: EditText
    private lateinit var employer: EditText
    private lateinit var address: EditText
    private lateinit var rate: EditText
    private var selectedId: Long? = null
    private var editing = false
    private val navy = Color.rgb(245,248,254)
    private val white = Color.rgb(12,47,95)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = MainActivity.Store(this)
        render()
    }

    private fun field(label: String, numeric: Boolean = false): EditText {
        root.addView(TextView(this).apply {
            text=label; textSize=14f; setTextColor(Color.rgb(61,86,120))
            setPadding(0,10,0,2)
        })
        return EditText(this).also {
            it.setTextColor(Color.rgb(12,47,95))
            it.setHintTextColor(Color.GRAY)
            it.setSingleLine(label!="آدرس کارگاه")
            if(numeric) it.inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            it.isEnabled=false
            root.addView(it)
        }
    }

    private fun action(label:String, onClick:()->Unit) {
        root.addView(Button(this).apply { text=label; isAllCaps=false; setTextColor(Color.rgb(9,62,132)); setOnClickListener { onClick() } })
    }

    private fun render(search:String="") {
        root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(22,22,22,22)
            setBackgroundColor(navy)
        }
        val scroll=ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        root.addView(TextView(this).apply { text="مشخصات کارگاه‌ها"; textSize=25f; setTextColor(white);setTypeface(null,1) })
        action("جدید") { clear(true) }
        action("ویرایش") { if(selectedId!=null) enable(true) else toast("ابتدا کارگاه را انتخاب کنید") }
        action("حذف") { deleteSelected() }
        action("ثبت") { save() }
        action("لغو") { clear(false) }
        action("جست‌وجو") {
            val q=EditText(this).apply { hint="کد، ردیف، نام کارگاه یا کارفرما" }
            AlertDialog.Builder(this).setTitle("جست‌وجوی کارگاه").setView(q)
                .setPositiveButton("جست‌وجو") { _,_-> render(q.text.toString().trim()) }
                .setNegativeButton("لغو",null).show()
        }
        action("خروج") { finish() }
        root.addView(TextView(this).apply { text="فهرست کارگاه‌ها (برای انتخاب لمس کنید)"; textSize=17f; setTextColor(white);setPadding(0,20,0,12) })
        val sql="SELECT id,code,contract_row,name,employer,address,insurance_rate FROM workshops WHERE code LIKE ? OR contract_row LIKE ? OR name LIKE ? OR employer LIKE ? ORDER BY id DESC"
        val arg="%$search%"
        store.readableDatabase.rawQuery(sql,arrayOf(arg,arg,arg,arg)).use { c ->
            while(c.moveToNext()) {
                val id=c.getLong(0)
                val label="${c.getString(3)} | ${c.getString(1)} | ${c.getString(2)}"
                action(label) { load(id) }
            }
        }
        code=field("کد کارگاه",true)
        contract=field("ردیف پیمان",true)
        name=field("نام کارگاه")
        employer=field("نام کارفرما")
        address=field("آدرس کارگاه")
        rate=field("نرخ حق بیمه (%)",true)
    }

    private fun enable(value:Boolean) {
        editing=value
        listOf(code,contract,name,employer,address,rate).forEach { it.isEnabled=value }
        if(value) code.requestFocus()
    }

    private fun clear(startEditing:Boolean) {
        selectedId=null
        listOf(code,contract,name,employer,address,rate).forEach { it.setText("") }
        enable(startEditing)
    }

    private fun load(id:Long) {
        store.readableDatabase.rawQuery("SELECT code,contract_row,name,employer,address,insurance_rate FROM workshops WHERE id=?",arrayOf(id.toString())).use { c ->
            if(c.moveToFirst()) {
                selectedId=id
                listOf(code,contract,name,employer,address,rate).forEachIndexed { i, edit ->
                    edit.setText(c.getString(i) ?: "")
                }
                enable(false)
            }
        }
    }

    private fun save() {
        if(!editing) { toast("ابتدا جدید یا ویرایش را انتخاب کنید");return }
        val fields=listOf(code,contract,name,employer,address,rate).map { it.text.toString().trim() }
        val percent=fields[5].toDoubleOrNull()
        if(fields.any { it.isBlank() } || percent==null || !percent.isFinite() || percent<0 || percent>100 ||
           !fields[0].matches(Regex("[0-9]+")) || !fields[1].matches(Regex("[0-9]{1,3}"))) {
            toast("همه فیلدها الزامی‌اند؛ ردیف پیمان حداکثر سه رقم و نرخ بین صفر تا صد باشد")
            return
        }
        val dup=store.readableDatabase.rawQuery(
            "SELECT id FROM workshops WHERE code=? AND contract_row=? AND id<>?",
            arrayOf(fields[0],fields[1],(selectedId ?: -1L).toString())
        ).use { it.moveToFirst() }
        if(dup) { toast("این کد کارگاه و ردیف پیمان قبلاً ثبت شده است");return }
        val values=ContentValues().apply {
            put("code",fields[0]);put("contract_row",fields[1]);put("name",fields[2])
            put("employer",fields[3]);put("address",fields[4]);put("insurance_rate",percent)
        }
        try {
            val database=store.writableDatabase
            val id=selectedId
            if(id==null) database.insertOrThrow("workshops",null,values)
            else database.update("workshops",values,"id=?",arrayOf(id.toString()))
            toast("اطلاعات کارگاه ذخیره شد")
            render()
        } catch(e:Exception) { toast("خطا در ذخیره اطلاعات") }
    }

    private fun deleteSelected() {
        val id=selectedId ?: run { toast("ابتدا کارگاه را انتخاب کنید");return }
        val count=store.readableDatabase.rawQuery("SELECT COUNT(*) FROM employees WHERE workshop_id=?",arrayOf(id.toString())).use {
            it.moveToFirst();it.getInt(0)
        }
        if(count>0) { toast("این کارگاه کارمند ثبت‌شده دارد؛ حذف مجاز نیست");return }
        AlertDialog.Builder(this).setTitle("حذف کارگاه")
            .setMessage("کارگاه انتخاب‌شده حذف شود؟ این عملیات برگشت‌پذیر نیست.")
            .setPositiveButton("حذف") { _,_->
                store.writableDatabase.delete("workshops","id=?",arrayOf(id.toString()))
                render()
            }.setNegativeButton("انصراف",null).show()
    }

    private fun toast(message:String) = Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}
