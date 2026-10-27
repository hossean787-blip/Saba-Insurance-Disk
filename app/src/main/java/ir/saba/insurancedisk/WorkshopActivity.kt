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
    private val blue = Color.rgb(8, 58, 125)
    private val gold = Color.rgb(234, 174, 52)
    private val ink = Color.rgb(23, 48, 83)
    private val muted = Color.rgb(92, 111, 137)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(5, 39, 88)
        store = MainActivity.Store(this)
        render()
    }

    private fun dp(value:Int):Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun shape(color:Int, radius:Int=16, stroke:Int=0):GradientDrawable =
        GradientDrawable().apply {
            setColor(color); cornerRadius=dp(radius).toFloat()
            if(stroke!=0) setStroke(dp(1),stroke)
        }

    private fun label(value:String,size:Float=14f,color:Int=ink):TextView =
        TextView(this).apply {
            text=value; textSize=size; setTextColor(color)
            gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }

    private fun button(title:String, icon:String, color:Int, action:()->Unit):TextView =
        label("$icon  $title",14f,color).apply {
            gravity=Gravity.CENTER
            setTypeface(null,Typeface.BOLD)
            background=shape(Color.WHITE,13,Color.rgb(213,225,241))
            elevation=dp(2).toFloat()
            setOnClickListener { action() }
        }

    private fun addButtonRow(actions:List<Triple<String,String,()->Unit>>) {
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; layoutDirection=View.LAYOUT_DIRECTION_RTL }
        actions.forEach { (title,icon,action) ->
            val item=button(title,icon,blue,action)
            row.addView(item,LinearLayout.LayoutParams(0,dp(56),1f).apply { setMargins(dp(3),dp(4),dp(3),dp(4)) })
        }
        root.addView(row)
    }

    private fun field(container:LinearLayout, title:String, decimal:Boolean=false, numeric:Boolean=false):EditText {
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=shape(Color.WHITE,12,Color.rgb(220,230,242))
            setPadding(dp(10),dp(8),dp(10),dp(4))
        }
        box.addView(label(title,12f,muted))
        val edit=EditText(this).apply {
            textSize=15f; setTextColor(ink); setHintTextColor(muted)
            setSingleLine(title!="آدرس کارگاه")
            if(numeric) inputType=InputType.TYPE_CLASS_NUMBER or (if(decimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0)
            isEnabled=false
            minHeight=dp(40)
        }
        box.addView(edit,LinearLayout.LayoutParams(-1,dp(46)))
        container.addView(box,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(10) })
        return edit
    }

    private fun pair(firstTitle:String, secondTitle:String, firstNumeric:Boolean=false, secondNumeric:Boolean=false):Pair<EditText,EditText> {
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; layoutDirection=View.LAYOUT_DIRECTION_RTL }
        val right=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val left=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        row.addView(right,LinearLayout.LayoutParams(0,-2,1f).apply { leftMargin=dp(5) })
        row.addView(left,LinearLayout.LayoutParams(0,-2,1f).apply { rightMargin=dp(5) })
        root.addView(row)
        return field(right,firstTitle,numeric=firstNumeric) to field(left,secondTitle,numeric=secondNumeric)
    }

    private fun render(search:String="") {
        root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(15),dp(16),dp(15),dp(22))
            setBackgroundColor(Color.rgb(242,247,253))
        }
        val scroll=ScrollView(this).apply { fillViewport=true; addView(root) }
        setContentView(scroll)

        val header=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(6,45,104),Color.rgb(10,109,197))).apply { cornerRadius=dp(20).toFloat() }
            setPadding(dp(18),dp(18),dp(18),dp(19))
        }
        header.addView(label("▣  مشخصات کارگاه‌ها",23f,Color.WHITE).apply { setTypeface(null,Typeface.BOLD) })
        header.addView(label("صبا  |  مدیریت اطلاعات کارگاه‌های بیمه",12f,Color.rgb(222,235,255)).apply { setPadding(0,dp(6),0,0) })
        root.addView(header,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(14) })

        addButtonRow(listOf(
            Triple("جدید","＋",{ clear(true) }),
            Triple("ویرایش","✎",{ if(selectedId!=null) enable(true) else toast("ابتدا کارگاه را انتخاب کنید") }),
            Triple("حذف","×",{ deleteSelected() })
        ))
        addButtonRow(listOf(
            Triple("ثبت","✓",{ save() }),
            Triple("لغو","↶",{ clear(false) }),
            Triple("خروج","⌂",{ finish() })
        ))

        val searchBox=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER_VERTICAL
            background=shape(Color.WHITE,14,Color.rgb(207,222,241))
            setPadding(dp(12),dp(5),dp(12),dp(5))
        }
        val searchInput=EditText(this).apply {
            hint="جست‌وجوی کد، ردیف، نام کارگاه یا کارفرما"
            setSingleLine(true); textSize=13f; setTextColor(ink)
            setText(search); setSelection(search.length)
        }
        searchBox.addView(searchInput,LinearLayout.LayoutParams(0,dp(49),1f))
        searchBox.addView(button("جست‌وجو","⌕",blue) { render(searchInput.text.toString().trim()) },
            LinearLayout.LayoutParams(dp(110),dp(43)))
        root.addView(searchBox,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(12);bottomMargin=dp(18) })

        root.addView(label("فهرست کارگاه‌ها",17f,blue).apply {
            setTypeface(null,Typeface.BOLD); setPadding(0,0,0,dp(9))
        })
        val table=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=shape(Color.WHITE,13,Color.rgb(209,224,241)); setPadding(dp(4),dp(4),dp(4),dp(4)) }
        fun tableRow(columns:List<String>, header:Boolean, id:Long?=null, index:Int=0) {
            val row=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL; layoutDirection=View.LAYOUT_DIRECTION_RTL; gravity=Gravity.CENTER_VERTICAL
                background=shape(if(header) blue else if(index%2==0) Color.WHITE else Color.rgb(235,244,255),6)
                if(id!=null) setOnClickListener { load(id) }
            }
            columns.forEachIndexed { columnIndex, value ->
                val cell=label(value,if(header) 12f else 12.5f,if(header) Color.WHITE else ink).apply {
                    gravity=Gravity.CENTER; setPadding(dp(3),dp(12),dp(3),dp(12))
                    if(header) setTypeface(null,Typeface.BOLD)
                }
                row.addView(cell,LinearLayout.LayoutParams(0,-2,if(columnIndex==2) 1.7f else 1f))
            }
            table.addView(row,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(2) })
        }
        tableRow(listOf("کد کارگاه","ردیف پیمان","نام کارگاه","نام کارفرما"),true)
        val arg="%$search%"
        var count=0
        store.readableDatabase.rawQuery(
            "SELECT id,code,contract_row,name,employer FROM workshops WHERE code LIKE ? OR contract_row LIKE ? OR name LIKE ? OR employer LIKE ? ORDER BY id DESC",
            arrayOf(arg,arg,arg,arg)
        ).use { cursor ->
            while(cursor.moveToNext()) {
                val id=cursor.getLong(0)
                tableRow(listOf(cursor.getString(1) ?: "",
                    (cursor.getString(2) ?: "").takeUnless { it=="000" } ?: "—",
                    cursor.getString(3) ?: "", cursor.getString(4) ?: ""),false,id,count)
                count++
            }
        }
        if(count==0) table.addView(label("کارگاهی برای نمایش وجود ندارد.",13f,muted).apply {
            gravity=Gravity.CENTER;setPadding(dp(8),dp(16),dp(8),dp(16))
        })
        root.addView(table,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(18) })
        root.addView(label("اطلاعات کارگاه",17f,blue).apply {
            setTypeface(null,Typeface.BOLD); setPadding(0,0,0,dp(10))
        })
        val codes=pair("کد کارگاه","ردیف پیمان",true,true)
        code=codes.first; contract=codes.second
        val names=pair("نام کارگاه","نام کارفرما")
        name=names.first; employer=names.second
        address=field(root,"آدرس کارگاه")
        rate=field(root,"نرخ حق بیمه (%)",decimal=true,numeric=true)
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
        store.readableDatabase.rawQuery(
            "SELECT code,contract_row,name,employer,address,insurance_rate FROM workshops WHERE id=?",
            arrayOf(id.toString())
        ).use { cursor ->
            if(cursor.moveToFirst()) {
                selectedId=id
                listOf(code,contract,name,employer,address).forEachIndexed { i, edit ->
                    val value=cursor.getString(i) ?: ""
                    edit.setText(if(i==1 && value=="000") "" else value)
                }
                val percent=cursor.getDouble(5)
                rate.setText(if(percent==0.0) "" else percent.toString())
                enable(false)
            }
        }
    }

    private fun save() {
        if(!editing) { toast("ابتدا جدید یا ویرایش را انتخاب کنید");return }
        val values=listOf(code,contract,name,employer,address,rate).map { it.text.toString().trim() }
        val percent=if(values[5].isBlank()) 0.0 else values[5].toDoubleOrNull()
        if(!values[0].matches(Regex("[0-9]+")) ||
            (values[1].isNotEmpty() && !values[1].matches(Regex("[0-9]{1,3}"))) ||
            values[2].isBlank() || values[3].isBlank() || values[4].isBlank() ||
            percent==null || !percent.isFinite() || percent<0 || percent>100) {
            toast("کد کارگاه، نام‌ها و آدرس را تکمیل کنید؛ مقدار ردیف و نرخ را بررسی کنید")
            return
        }
        val row=values[1].ifBlank { "000" }
        val duplicate=store.readableDatabase.rawQuery(
            "SELECT id FROM workshops WHERE code=? AND contract_row=? AND id<>?",
            arrayOf(values[0],row,(selectedId ?: -1L).toString())
        ).use { it.moveToFirst() }
        if(duplicate) { toast("این کد کارگاه و ردیف قبلاً ثبت شده است");return }
        val data=ContentValues().apply {
            put("code",values[0]);put("contract_row",row)
            put("name",values[2]);put("employer",values[3])
            put("address",values[4]);put("insurance_rate",percent)
        }
        try {
            val database=store.writableDatabase
            val id=selectedId
            if(id==null) database.insertOrThrow("workshops",null,data)
            else database.update("workshops",data,"id=?",arrayOf(id.toString()))
            toast("اطلاعات کارگاه ذخیره شد")
            render()
        } catch(e:Exception) { toast("خطا در ذخیره اطلاعات") }
    }

    private fun deleteSelected() {
        val id=selectedId ?: run { toast("ابتدا کارگاه را انتخاب کنید");return }
        val count=store.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM employees WHERE workshop_id=?",arrayOf(id.toString())
        ).use { it.moveToFirst();it.getInt(0) }
        if(count>0) { toast("کارگاه دارای کارمند است و حذف نمی‌شود");return }
        AlertDialog.Builder(this).setTitle("حذف کارگاه")
            .setMessage("کارگاه انتخاب‌شده حذف شود؟")
            .setPositiveButton("حذف") { _,_->
                store.writableDatabase.delete("workshops","id=?",arrayOf(id.toString()))
                render()
            }.setNegativeButton("انصراف",null).show()
    }

    private fun toast(message:String) = Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}
