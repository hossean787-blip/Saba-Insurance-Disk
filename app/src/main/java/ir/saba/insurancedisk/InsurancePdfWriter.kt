package ir.saba.insurancedisk

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A4 RTL PDF with real Persian shaping, repeating headers and bounded multi-page tables. */
internal object InsurancePdfWriter {
    private const val WIDTH=595f
    private const val HEIGHT=842f
    private const val MARGIN=30f
    private const val BODY_WIDTH=WIDTH-2*MARGIN
    private const val CONTENT_BOTTOM=766f
    private const val NAVY=0xff2c3e4c.toInt()
    private const val BLUE=0xff3944bc.toInt()
    private const val INK=0xff263045.toInt()
    private const val MUTED=0xff6d778e.toInt()
    private const val BORDER=0xffe3e6f3.toInt()
    private const val PALE=0xffeeefff.toInt()

    fun write(report:InsuranceReportData, output:OutputStream):Int {
        val pdf=PdfDocument()
        try {
            val painter=Painter(pdf,report)
            painter.render()
            pdf.writeTo(output)
            return painter.pageCount
        } finally {
            pdf.close()
        }
    }

    fun fileName(report:InsuranceReportData):String {
        val h=report.header
        val code=h.workshopCode.filter{it.isLetterOrDigit()}.take(24).ifEmpty{"workshop"}
        val month=h.month.toString().padStart(2,'0')
        val no=h.listNumber.toString().padStart(2,'0')
        return "SURA-Insurance-$code-${h.year}-$month-$no.pdf"
    }

    private class Painter(private val document:PdfDocument,private val report:InsuranceReportData) {
        private var page:PdfDocument.Page?=null
        private var canvas:Canvas?=null
        private var y=0f
        var pageCount=0
            private set

        private fun fill(color:Int)=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color=color;style=Paint.Style.FILL
        }
        private fun border()=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=BORDER;strokeWidth=0.75f;style=Paint.Style.STROKE
        }
        private fun paragraph(
            value:String,width:Float,size:Float=11f,color:Int=INK,
            bold:Boolean=false,center:Boolean=false
        ):StaticLayout {
            val content=value.ifEmpty{"—"}
            val p=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color=color
                textSize=size
                typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)
                isSubpixelText=true
            }
            return StaticLayout.Builder.obtain(content,0,content.length,p,width.toInt().coerceAtLeast(1))
                .setAlignment(if(center)Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(false)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .build()
        }
        private fun text(
            value:String,x:Float,top:Float,width:Float,
            size:Float=11f,color:Int=INK,bold:Boolean=false,center:Boolean=false
        ):Float {
            val layout=paragraph(value,width,size,color,bold,center)
            val c=canvas ?:error("PDF page not open")
            c.save()
            c.translate(x,top)
            layout.draw(c)
            c.restore()
            return layout.height.toFloat()
        }
        private fun openPage() {
            if(page!=null)finishPage()
            pageCount++
            page=document.startPage(PdfDocument.PageInfo.Builder(
                WIDTH.toInt(),HEIGHT.toInt(),pageCount
            ).create())
            canvas=page!!.canvas
            val c=canvas!!
            c.drawColor(Color.WHITE)
            c.drawRoundRect(RectF(26f,22f,569f,105f),15f,15f,fill(NAVY))
            text("سورا | گزارش لیست بیمه ماهانه",52f,37f,492f,20f,Color.WHITE,true)
            text(
                "کارگاه: ${report.header.workshopName}    •    دوره: ${report.header.year}/${report.header.month.toString().padStart(2,'0')}",
                53f,73f,487f,11f,0xffd9e9ff.toInt()
            )
            y=122f
        }
        private fun finishPage() {
            val c=canvas ?:return
            val p=page ?:return
            c.drawLine(MARGIN,791f,WIDTH-MARGIN,791f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color=BORDER;strokeWidth=1f })
            text("سورا • گزارش داخلی حسابداری",MARGIN,800f,300f,9f,MUTED)
            text("صفحه $pageCount",WIDTH-MARGIN-100f,800f,100f,9f,MUTED,center=true)
            document.finishPage(p)
            page=null
            canvas=null
        }
        private fun ensure(height:Float) {
            if(page==null)openPage()
            if(y+height>CONTENT_BOTTOM)openPage()
        }
        private fun title(s:String,height:Float=27f) {
            ensure(height)
            text(s,MARGIN,y,BODY_WIDTH,14f,NAVY,true)
            y+=height
        }
        private fun pair(
            first:Pair<String,String>,
            second:Pair<String,String>?
        ) {
            val gap=8f
            val w=(BODY_WIDTH-gap)/2
            val left=MARGIN
            val right=MARGIN+w+gap
            val firstV=paragraph(first.second,w-20f,11f,INK,true)
            val secondV=second?.let{paragraph(it.second,w-20f,11f,INK,true)}
            val rowHeight=(21f+maxOf(firstV.height.toFloat(),secondV?.height?.toFloat()?:0f)+12f)
                .coerceAtLeast(51f)
            ensure(rowHeight+7f)
            drawBox(right,y,w,rowHeight,first.first,first.second)
            if(second!=null)drawBox(left,y,w,rowHeight,second.first,second.second)
            y+=rowHeight+7f
        }
        private fun drawBox(x:Float,top:Float,w:Float,h:Float,key:String,value:String) {
            val c=canvas!!
            c.drawRoundRect(RectF(x,top,x+w,top+h),8f,8f,fill(PALE))
            c.drawRoundRect(RectF(x,top,x+w,top+h),8f,8f,border())
            text(key,x+9f,top+7f,w-18f,9.5f,MUTED)
            text(value,x+9f,top+22f,w-18f,11f,INK,true)
        }
        private fun tiles() {
            ensure(83f)
            val entries=listOf(
                "کارکنان" to report.totals.employeeCount.toString(),
                "روزهای کارکرد" to report.totals.days.toString(),
                "جمع مشمول (ریال)" to format(report.totals.total)
            )
            val gap=7f
            val w=(BODY_WIDTH-2*gap)/3f
            for((index,entry) in entries.withIndex()) {
                val x=WIDTH-MARGIN-w-index*(w+gap)
                canvas!!.drawRoundRect(RectF(x,y,x+w,y+65f),10f,10f,fill(
                    if(index==2)NAVY else PALE
                ))
                val color=if(index==2)Color.WHITE else INK
                text(entry.first,x+7f,y+10f,w-14f,10f,if(index==2)0xffd9e9ff.toInt() else MUTED)
                text(entry.second,x+7f,y+31f,w-14f,13.2f,color,true)
            }
            y+=80f
        }
        private fun cellText(
            value:String,w:Float,header:Boolean,
            center:Boolean=false
        ):StaticLayout=paragraph(
            value,w-12f,
            if(header)9.5f else if(center)9.5f else 10f,
            if(header)Color.WHITE else INK,header,center
        )
        private val columnWidths=listOf(197f,42f,94f,95f,107f)
        private fun tableLine(values:List<String>,header:Boolean,index:Int=0) {
            val layouts=values.mapIndexed{n,value->
                cellText(value,columnWidths[n],header,n>0)
            }
            val h=(layouts.maxOf{it.height.toFloat()}+16f)
                .coerceAtLeast(if(header)34f else 58f)
            ensure(h)
            val c=canvas!!
            val background=when {
                header -> NAVY
                index%2==0 -> Color.WHITE
                else -> PALE
            }
            c.drawRect(MARGIN,y,WIDTH-MARGIN,y+h,fill(background))
            c.drawRect(MARGIN,y,WIDTH-MARGIN,y+h,border())
            var xRight=WIDTH-MARGIN
            for(n in columnWidths.indices) {
                val w=columnWidths[n]
                val x=xRight-w
                val layout=layouts[n]
                c.save()
                c.translate(x+6f,y+(h-layout.height)/2f)
                layout.draw(c)
                c.restore()
                if(n>0)c.drawLine(xRight,y,xRight,y+h,border())
                xRight=x
            }
            y+=h
        }
        private fun tableHeader()=tableLine(
            listOf("کارمند / مشخصات بیمه","روز","مزد روزانه","مزایا","جمع ماهانه"),true
        )
        private fun renderTable() {
            title("ریزکارکرد و دستمزد کارکنان",32f)
            tableHeader()
            if(report.people.isEmpty()) {
                ensure(44f)
                text("ریزکارکردی برای این دوره ثبت نشده است.",MARGIN+10f,y+13f,BODY_WIDTH-20f)
                y+=46f
            }
            for((i,p) in report.people.withIndex()) {
                val detail=buildString {
                    append("${i+1}. ${p.name.ifBlank{"نام ثبت نشده"}}")
                    append("\nبیمه: ${p.insuranceNumber.ifBlank{"نامشخص"}}")
                    append("  |  کد ملی: ${p.nationalId.ifBlank{"نامشخص"}}")
                    if(p.jobCode.isNotBlank())append("\nکد شغل: ${p.jobCode}")
                }
                val columns=listOf(
                    detail,p.days.toString(),format(p.dailyWage),
                    format(p.benefits),format(InsuranceReportMath.monthlyTotal(p))
                )
                val layouts=columns.mapIndexed{index,v->cellText(v,columnWidths[index],false,index>0)}
                val required=(layouts.maxOf{it.height.toFloat()}+16f).coerceAtLeast(58f)
                if(y+required>CONTENT_BOTTOM) {
                    openPage()
                    title("ریزکارکرد کارکنان (ادامه)",32f)
                    tableHeader()
                }
                tableLine(columns,false,i)
            }
            y+=11f
        }
        private fun summary() {
            ensure(197f)
            title("خلاصه محاسبات",32f)
            val rows=mutableListOf(
                "تعداد کارکنان" to report.totals.employeeCount.toString(),
                "جمع روزهای کارکرد" to report.totals.days.toString(),
                "جمع دستمزد ماهانه" to "${format(report.totals.wages)} ریال",
                "جمع مزایای مشمول ثبت‌شده" to "${format(report.totals.benefits)} ریال",
                "جمع دستمزد و مزایای مشمول" to "${format(report.totals.total)} ریال"
            )
            report.premium?.let { p->
                rows.add("سهم بیمه‌شده (۷٪)" to "${format(p.insured)} ریال")
                rows.add("سهم کارفرما (۲۰٪)" to "${format(p.employer)} ریال")
                rows.add("بیمه بیکاری (۳٪)" to "${format(p.unemployment)} ریال")
                rows.add("جمع کل حق بیمه برآوردی" to "${format(p.total)} ریال")
            } ?: rows.add("حق بیمه نرخ اختصاصی" to "نیازمند بررسی")
            for((i,pair) in rows.withIndex()) {
                ensure(29f)
                canvas!!.drawRect(MARGIN,y,WIDTH-MARGIN,y+27f,
                    fill(if(i%2==0)PALE else Color.WHITE))
                text(pair.first,WIDTH-MARGIN-255f,y+7f,247f,10.7f,
                    if(i==4)NAVY else INK,i==4)
                text(pair.second,MARGIN+9f,y+7f,242f,10.7f,
                    if(i==4)NAVY else INK,i==4)
                y+=29f
            }
            y+=9f
        }
        private fun disclaimer(){
            val notice="حق بیمه با فرض نرخ عادی ۷٪، ۲۰٪ و ۳٪ فقط برآورد می‌شود؛ برای نرخ اختصاصی محاسبه نشده است. معافیت‌ها، سقف و حداقل مزد و تفکیک مزایا نیازمند تأیید هستند. ۴٪ مشاغل سخت در بدهی ماهانه منظور نشده است. این گزارش داخلی جایگزین فایل دیسک قابل ارسال به تأمین اجتماعی نیست."
            val lay=paragraph(notice,BODY_WIDTH-24f,11f,0xff90572c.toInt())
            val h=lay.height.toFloat()+23f
            ensure(h+13f)
            canvas!!.drawRoundRect(RectF(MARGIN,y,WIDTH-MARGIN,y+h),8f,8f,fill(0xfffff4e5.toInt()))
            text(notice,MARGIN+12f,y+11f,BODY_WIDTH-24f,11f,0xff90572c.toInt())
            y+=h+12f
            if(report.warnings.isNotEmpty()) {
                title("موارد نیازمند بررسی",28f)
                for(warning in report.warnings) {
                    val warningHeight=paragraph("• $warning",BODY_WIDTH-14f,10f,0xffa34035.toInt()).height+12f
                    ensure(warningHeight)
                    text("• $warning",MARGIN+7f,y,BODY_WIDTH-14f,10f,0xffa34035.toInt())
                    y+=warningHeight
                }
            }
        }
        fun render() {
            openPage()
            title("مشخصات کارگاه و دوره",32f)
            val h=report.header
            pair("نام کارگاه" to h.workshopName,"کد کارگاه" to h.workshopCode)
            pair("نام کارفرما" to h.employer,"ردیف پیمان" to h.contractRow)
            pair("سال / ماه" to "${h.year}/${h.month.toString().padStart(2,'0')}",
                "شماره لیست" to h.listNumber.toString().padStart(2,'0'))
            pair("نشانی کارگاه" to h.address,
                "زمان تهیه" to SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.US).format(Date()))
            y+=7f
            tiles()
            renderTable()
            summary()
            disclaimer()
            finishPage()
        }
        private fun format(n:Long)=String.format(Locale.US,"%,d",n)
    }
}
