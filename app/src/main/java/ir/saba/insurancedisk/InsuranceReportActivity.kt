package ir.saba.insurancedisk

import android.app.Activity
import android.os.Bundle
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.content.ContentValues
import android.provider.MediaStore
import android.widget.*
import android.view.View

open class InsuranceReportActivity : Activity() {
    private val db by lazy { MainActivity.Store(this) }
    private val lists = mutableListOf<Pair<Long,String>>()
    private lateinit var selector: Spinner
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply { orientation=1; layoutDirection=View.LAYOUT_DIRECTION_RTL; setPadding(24,24,24,24) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(TextView(this).apply { text=if(intent.getBooleanExtra("pdf",false)) "فایل PDF لیست بیمه" else "نمایش لیست بیمه"; textSize=24f })
        db.readableDatabase.rawQuery("SELECT l.id,w.name,l.year,l.month FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id ORDER BY l.year DESC,l.month DESC",null).use { c ->
            while(c.moveToNext()) lists.add(c.getLong(0) to "${c.getString(1)} - ${c.getInt(2)}/${c.getInt(3)}")
        }
        if(lists.isEmpty()) { root.addView(TextView(this).apply { text="لیستی ثبت نشده است" }); return }
        selector=Spinner(this).apply { adapter=ArrayAdapter(this@InsuranceReportActivity,android.R.layout.simple_spinner_dropdown_item,lists.map { it.second }) }
        root.addView(selector)
        output=TextView(this).apply { textSize=17f; textDirection=View.TEXT_DIRECTION_RTL; setPadding(8,20,8,20) }
        root.addView(Button(this).apply { text="نمایش"; setOnClickListener { output.text=report() } })
        if(intent.getBooleanExtra("pdf",false)) root.addView(Button(this).apply { text="ذخیره PDF"; setOnClickListener { save() } })
        root.addView(output)
        output.text=report()
    }
    private fun report():String {
        val id=lists[selector.selectedItemPosition.coerceAtLeast(0)].first
        val text=StringBuilder("سورا - لیست بیمه\n")
        db.readableDatabase.rawQuery("SELECT w.name,w.code,l.year,l.month,l.list_number FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id WHERE l.id=?",arrayOf(id.toString())).use { c ->
            if(c.moveToFirst()) text.append("کارگاه: ${c.getString(0)}\nکد: ${c.getString(1)}\nدوره: ${c.getInt(2)}/${c.getInt(3)}\nشماره لیست: ${c.getInt(4)}\n")
        }
        var days=0L; var wages=0L; var benefits=0L; var count=0
        db.readableDatabase.rawQuery("SELECT e.first_name,e.last_name,i.days,i.daily_wage,i.benefits FROM insurance_list_items i JOIN employees e ON e.id=i.employee_id WHERE i.list_id=?",arrayOf(id.toString())).use { c ->
            while(c.moveToNext()) {
                val d=c.getLong(2);val w=c.getLong(3);val b=c.getLong(4)
                days=Math.addExact(days,d);wages=Math.addExact(wages,Math.multiplyExact(d,w));benefits=Math.addExact(benefits,b);count++
                text.append("\n${c.getString(0)} ${c.getString(1)}: $d روز، دستمزد $w، مزایا $b")
            }
        }
        text.append("\n\nتعداد نفرات: $count\nجمع روزها: $days\nدستمزد ماهانه: $wages ریال\nمزایا: $benefits ریال\nجمع: ${Math.addExact(wages,benefits)} ریال\nحق بیمه: محاسبه‌نشده")
        return text.toString()
    }
    private fun save() {
        val content=try { report() } catch(e:Exception) { Toast.makeText(this,"خطا در محاسبات",Toast.LENGTH_LONG).show();return }
        val doc=PdfDocument()
        try {
            val page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,1).create())
            val paint=android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize=13f;color=android.graphics.Color.BLACK }
            val layout=android.text.StaticLayout.Builder.obtain(content,0,content.length,paint,510).setAlignment(android.text.Layout.Alignment.ALIGN_OPPOSITE).setTextDirection(android.text.TextDirectionHeuristics.RTL).build()
            page.canvas.translate(40f,40f);layout.draw(page.canvas);doc.finishPage(page)
            val values=ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME,"SURA-${System.currentTimeMillis()}.pdf");put(MediaStore.Downloads.MIME_TYPE,"application/pdf");put(MediaStore.Downloads.RELATIVE_PATH,"Download/SURA") }
            val uri=contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values) ?: error("Storage unavailable")
            contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) } ?: error("Cannot write")
            Toast.makeText(this,"PDF ذخیره شد",Toast.LENGTH_LONG).show()
        } catch(e:Exception) { Toast.makeText(this,"خطا در ذخیره PDF: ${e.message}",Toast.LENGTH_LONG).show() }
        finally { doc.close() }
    }
}
