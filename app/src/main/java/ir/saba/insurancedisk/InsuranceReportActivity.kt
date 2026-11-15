package ir.saba.insurancedisk

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File

/**
 * Independent data preview and PDF export entry points.
 * PDF mode renders the actual A4 PDF, not a separately simulated preview.
 */
open class InsuranceReportActivity : Activity() {
    private val store by lazy { MainActivity.Store(this) }
    private val pdfMode by lazy { intent.getBooleanExtra("pdf",false) }
    private val navy=Color.rgb(8,49,105)
    private val ink=Color.rgb(23,48,83)
    private val muted=Color.rgb(93,111,137)
    private val outline=Color.rgb(211,225,241)
    private val pale=Color.rgb(240,247,255)
    private val requestSavePdf=8271

    private lateinit var selector:Spinner
    private lateinit var previewPanel:LinearLayout
    private lateinit var summaryPanel:LinearLayout
    private lateinit var statusText:TextView
    private lateinit var pageLabel:TextView
    private lateinit var image:ImageView
    private lateinit var shareButton:TextView
    private val listRefs=mutableListOf<Pair<Long,String>>()
    private var current:InsuranceReportData?=null
    private var lastSavedUri:Uri?=null
    private var previewFile:File?=null
    private var pageIndex=0
    private var pages=0

    private fun dp(n:Int)=(n*resources.displayMetrics.density+0.5f).toInt()
    private fun shape(color:Int,radius:Int=12,stroke:Int=0)=GradientDrawable().apply {
        setColor(color);cornerRadius=dp(radius).toFloat()
        if(stroke!=0)setStroke(dp(1),stroke)
    }
    private fun label(value:String,size:Float=14f,color:Int=ink)=TextView(this).apply {
        text=value;textSize=size;setTextColor(color)
        gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
    }
    private fun headline(value:String)=label(value,17f,navy).apply {
        setTypeface(null,Typeface.BOLD)
        setPadding(0,dp(10),0,dp(8))
    }
    private fun action(text:String,icon:String,run:()->Unit)=
        label("$icon  $text",14f,navy).apply {
            gravity=Gravity.CENTER
            setTypeface(null,Typeface.BOLD)
            background=shape(Color.WHITE,12,outline)
            elevation=dp(2).toFloat()
            setOnClickListener{run()}
        }
    private fun notice(message:String)=Toast.makeText(this,message,Toast.LENGTH_LONG).show()

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(5,39,88)
        savedInstanceState?.getString("lastUri")?.let {
            lastSavedUri=Uri.parse(it)
        }
        val lastId=savedInstanceState?.getLong("listId",-1L) ?: -1L
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(15),dp(16),dp(15),dp(27))
            setBackgroundColor(Color.rgb(242,247,253))
        }
        val scroll=ScrollView(this).apply {isFillViewport=true;addView(root)}
        scroll.setOnApplyWindowInsetsListener { view,insets ->
            val top=if(android.os.Build.VERSION.SDK_INT>=30)
                insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
                else insets.systemWindowInsetTop
            val bottom=if(android.os.Build.VERSION.SDK_INT>=30)
                insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
                else insets.systemWindowInsetBottom
            view.setPadding(0,top,0,bottom+dp(12));insets
        }
        setContentView(scroll)
        val header=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(18),dp(18),dp(19))
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(6,45,104),Color.rgb(10,109,197))).apply {
                cornerRadius=dp(20).toFloat()
            }
        }
        header.addView(label(
            if(pdfMode)"▧  خروجی PDF لیست بیمه" else "◉  نمایش لیست بیمه",
            22f,Color.WHITE
        ).apply{setTypeface(null,Typeface.BOLD)})
        header.addView(label(
            if(pdfMode)"پیش‌نمایش واقعی، ذخیره و اشتراک‌گذاری گزارش A4"
                else "مشاهده جزئیات و ارقام ثبت‌شده، بدون تغییر اطلاعات",
            12f,Color.rgb(219,234,255)
        ).apply{setPadding(0,dp(7),0,0)})
        root.addView(header,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(15)})

        root.addView(headline("انتخاب لیست بیمه ماهانه"))
        store.readableDatabase.rawQuery(
            """SELECT l.id,w.name,w.code,l.year,l.month,l.list_number
               FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id
               ORDER BY l.year DESC,l.month DESC,l.id DESC""",null
        ).use { c->
            while(c.moveToNext())listRefs.add(
                c.getLong(0) to "${c.getString(1)}  •  ${c.getString(2)}  •  ${c.getInt(3)}/${c.getInt(4).toString().padStart(2,'0')}  •  ${c.getInt(5).toString().padStart(2,'0')}"
            )
        }
        if(listRefs.isEmpty()) {
            root.addView(label("هیچ لیست ماهانه‌ای ثبت نشده است؛ ابتدا از بخش «لیست بیمه» یک لیست ایجاد کنید.",14f,muted).apply {
                setPadding(dp(12),dp(20),dp(12),dp(20))
            })
            root.addView(action("خروج","⌂"){finish()},
                LinearLayout.LayoutParams(-1,dp(50)))
            return
        }
        selector=Spinner(this).apply {
            adapter=ArrayAdapter(this@InsuranceReportActivity,
                android.R.layout.simple_spinner_dropdown_item,listRefs.map {it.second})
            background=shape(Color.WHITE,12,outline)
            setPadding(dp(9),dp(4),dp(9),dp(4))
        }
        root.addView(selector,LinearLayout.LayoutParams(-1,dp(55)))

        val actions=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
        }
        actions.addView(action("نمایش","◉"){renderSelected()},
            LinearLayout.LayoutParams(0,dp(52),1f).apply{setMargins(dp(3),dp(9),dp(3),dp(6))})
        actions.addView(action("خروج","⌂"){finish()},
            LinearLayout.LayoutParams(0,dp(52),1f).apply{setMargins(dp(3),dp(9),dp(3),dp(6))})
        root.addView(actions)

        if(pdfMode){
            val export=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL
            }
            export.addView(action("ذخیره PDF","↓"){savePdf()},
                LinearLayout.LayoutParams(0,dp(52),1f).apply{setMargins(dp(3),dp(3),dp(3),dp(6))})
            shareButton=action("اشتراک‌گذاری","↗"){sharePdf()}
            export.addView(shareButton,
                LinearLayout.LayoutParams(0,dp(52),1f).apply{setMargins(dp(3),dp(3),dp(3),dp(6))})
            shareButton.isEnabled=lastSavedUri!=null
            shareButton.alpha=if(shareButton.isEnabled)1f else 0.45f
            root.addView(export)
        }

        statusText=label("",12f,muted).apply{setPadding(dp(2),dp(7),dp(2),dp(7))}
        root.addView(statusText)
        summaryPanel=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        root.addView(summaryPanel)
        if(pdfMode){
            root.addView(headline("پیش‌نمایش صفحات PDF"))
            val navigation=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                layoutDirection=View.LAYOUT_DIRECTION_RTL
            }
            navigation.addView(action("قبلی","‹"){showPdfPage(pageIndex-1)},
                LinearLayout.LayoutParams(0,dp(44),1f).apply {rightMargin=dp(3)})
            pageLabel=label("—",12f,muted).apply{gravity=Gravity.CENTER}
            navigation.addView(pageLabel,LinearLayout.LayoutParams(0,dp(44),1f))
            navigation.addView(action("بعدی","›"){showPdfPage(pageIndex+1)},
                LinearLayout.LayoutParams(0,dp(44),1f).apply{leftMargin=dp(3)})
            root.addView(navigation)
            image=ImageView(this).apply {
                adjustViewBounds=true
                scaleType=ImageView.ScaleType.FIT_CENTER
                background=shape(Color.WHITE,8,outline)
                contentDescription="پیش‌نمایش صفحه سند PDF"
            }
            root.addView(image,LinearLayout.LayoutParams(-1,-2).apply{
                topMargin=dp(10);bottomMargin=dp(8)
            })
        } else {
            root.addView(headline("ریزکارکرد کارکنان"))
        }
        previewPanel=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        root.addView(previewPanel)

        if(lastId!=-1L)selector.setSelection(
            listRefs.indexOfFirst{it.first==lastId}.coerceAtLeast(0)
        )
        renderSelected()
    }
    override fun onSaveInstanceState(outState:Bundle) {
        super.onSaveInstanceState(outState)
        if(::selector.isInitialized && listRefs.isNotEmpty())
            outState.putLong("listId",listRefs[selector.selectedItemPosition].first)
        lastSavedUri?.let {outState.putString("lastUri",it.toString())}
    }
    private fun format(n:Long)=String.format(java.util.Locale.US,"%,d",n)
    private fun currentId():Long?=if(!::selector.isInitialized || listRefs.isEmpty())null
        else listRefs.getOrNull(selector.selectedItemPosition)?.first

    private fun renderSelected(){
        val id=currentId() ?:return
        try {
            val data=InsuranceReportRepository(store.readableDatabase).load(id)
            current=data
            statusText.text="دوره ${data.header.year}/${data.header.month.toString().padStart(2,'0')} • شماره لیست ${data.header.listNumber.toString().padStart(2,'0')} • ${data.totals.employeeCount} نفر"
            renderSummary(data)
            if(pdfMode){
                val output=File(cacheDir,"sura-report-preview.pdf")
                output.outputStream().use { InsurancePdfWriter.write(data,it) }
                previewFile=output
                pageIndex=0
                showPdfPage(0)
                previewPanel.removeAllViews()
            } else {
                renderRows(data)
            }
        }catch(ex:Exception){
            current=null
            statusText.text="خطا در تهیه گزارش: ${ex.message ?: "اطلاعات نامعتبر"}"
            summaryPanel.removeAllViews()
            previewPanel.removeAllViews()
            if(pdfMode){
                image.setImageDrawable(null);pageLabel.text="—"
                previewFile=null;pages=0
            }
        }
    }
    private fun rowCard(items:List<Pair<String,String>>):LinearLayout {
        val card=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=shape(Color.WHITE,13,outline)
            setPadding(dp(5),dp(5),dp(5),dp(5))
        }
        items.forEachIndexed { index,(key,value)->
            val line=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                layoutDirection=View.LAYOUT_DIRECTION_RTL
                gravity=Gravity.CENTER_VERTICAL
                background=shape(if(index%2==0)Color.WHITE else pale,7)
            }
            line.addView(label(key,13f,muted).apply{
                setPadding(dp(9),dp(10),dp(5),dp(10))
            },LinearLayout.LayoutParams(0,-2,1f))
            line.addView(label(value,13f,ink).apply{
                setPadding(dp(5),dp(10),dp(10),dp(10))
            },LinearLayout.LayoutParams(0,-2,1.25f))
            card.addView(line)
        }
        return card
    }
    private fun renderSummary(data:InsuranceReportData) {
        summaryPanel.removeAllViews()
        summaryPanel.addView(headline("خلاصه گزارش — فقط خواندنی"))
        summaryPanel.addView(rowCard(listOf(
            "کارگاه" to data.header.workshopName,
            "کد کارگاه / ردیف پیمان" to "${data.header.workshopCode} / ${data.header.contractRow}",
            "تعداد کارکنان" to data.totals.employeeCount.toString(),
            "جمع روزها" to data.totals.days.toString(),
            "دستمزد ماهانه" to "${format(data.totals.wages)} ریال",
            "مزایای مشمول" to "${format(data.totals.benefits)} ریال",
            "جمع مشمول" to "${format(data.totals.total)} ریال",
            "حق بیمه" to "محاسبه‌نشده"
        )))
        summaryPanel.addView(label(
            "این PDF گزارش داخلی است؛ سهم‌های بیمه و خروجی دیسک در این نسخه محاسبه یا تولید نمی‌شوند.",
            12f,Color.rgb(151,86,37)
        ).apply {setPadding(dp(8),dp(12),dp(8),dp(8))})
        if(data.warnings.isNotEmpty()){
            summaryPanel.addView(headline("هشدارهای اطلاعاتی"))
            data.warnings.take(12).forEach { warning->
                summaryPanel.addView(label("• $warning",12f,Color.rgb(163,59,47)).apply{
                    setPadding(dp(8),dp(5),dp(8),dp(5))
                })
            }
            if(data.warnings.size>12)summaryPanel.addView(label(
                "${data.warnings.size-12} هشدار دیگر نیز در فایل PDF درج می‌شود.",12f,muted))
        }
    }
    private fun renderRows(data:InsuranceReportData){
        previewPanel.removeAllViews()
        if(data.people.isEmpty()){
            previewPanel.addView(label("برای این لیست هنوز ریزکارکرد ثبت نشده است.",14f,muted))
            return
        }
        for((index,p) in data.people.withIndex()){
            val item=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                background=shape(Color.WHITE,12,outline)
                setPadding(dp(12),dp(11),dp(12),dp(11))
            }
            item.addView(label("${index+1}. ${p.name}",15f,navy).apply{
                setTypeface(null,Typeface.BOLD)
            })
            item.addView(label(
                "بیمه: ${p.insuranceNumber}  •  کد ملی: ${p.nationalId}",12f,muted))
            item.addView(label(
                "کارکرد: ${p.days} روز  •  مزد روزانه: ${format(p.dailyWage)} ریال",12f,ink))
            item.addView(label(
                "مزایا: ${format(p.benefits)}  •  جمع: ${format(InsuranceReportMath.monthlyTotal(p))} ریال",
                12f,ink))
            previewPanel.addView(item,LinearLayout.LayoutParams(-1,-2).apply{
                bottomMargin=dp(8)
            })
        }
    }
    private fun showPdfPage(next:Int){
        val file=previewFile ?:return
        try {
            ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    pages=renderer.pageCount
                    if(pages<=0)return
                    pageIndex=next.coerceIn(0,pages-1)
                    renderer.openPage(pageIndex).use { page ->
                        val width=720
                        val height=(width.toFloat()*page.height/page.width).toInt()
                        val bmp=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        image.setImageBitmap(bmp)
                        pageLabel.text="صفحه ${pageIndex+1} از $pages"
                    }
                }
            }
        }catch(ex:Exception){
            pageLabel.text="خطای پیش‌نمایش"
            notice("پیش‌نمایش PDF باز نشد: ${ex.message}")
        }
    }
    private fun savePdf(){
        val selected=currentId() ?:run{notice("لیست انتخاب کنید");return}
        if(current?.header?.listId!=selected)renderSelected()
        val report=current ?:run{notice("ابتدا گزارش معتبر را نمایش دهید");return}
        val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type="application/pdf"
            putExtra(Intent.EXTRA_TITLE,InsurancePdfWriter.fileName(report))
        }
        try {startActivityForResult(intent,requestSavePdf)}
        catch(ex:Exception){notice("دسترسی به انتخاب محل ذخیره PDF ممکن نشد")}
    }
    @Deprecated("The platform callback is used to support API 26+ without extra dependencies.")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=requestSavePdf || resultCode!=RESULT_OK)return
        val uri=data?.data ?:run{notice("محل ذخیره انتخاب نشده است");return}
        val report=current ?:run{notice("گزارش برای ذخیره در دسترس نیست");return}
        try {
            contentResolver.openOutputStream(uri,"w")?.use { stream ->
                InsurancePdfWriter.write(report,stream)
            } ?: error("امکان نوشتن فایل وجود ندارد")
            lastSavedUri=uri
            shareButton.isEnabled=true
            shareButton.alpha=1f
            notice("فایل PDF با موفقیت در محل انتخاب‌شده ذخیره شد")
        }catch(ex:Exception){
            lastSavedUri=null
            shareButton.isEnabled=false
            shareButton.alpha=0.45f
            notice("ذخیره PDF ناموفق بود: ${ex.message}")
        }
    }
    private fun sharePdf(){
        val uri=lastSavedUri ?:run{notice("ابتدا PDF را ذخیره کنید");return}
        val intent=Intent(Intent.ACTION_SEND).apply {
            type="application/pdf"
            putExtra(Intent.EXTRA_STREAM,uri)
            clipData=ClipData.newUri(contentResolver,"SURA insurance PDF",uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { startActivity(Intent.createChooser(intent,"اشتراک‌گذاری گزارش PDF")) }
        catch(ex:Exception){notice("اشتراک‌گذاری فایل امکان‌پذیر نشد")}
    }
}
