package ir.saba.insurancedisk

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.RippleDrawable
import android.provider.Settings
import android.view.animation.DecelerateInterpolator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * SURA's own native Android design system.
 * The visual direction is indigo/charcoal; no third-party UI or assets are reused.
 */
internal object SuraUi {
    val charcoal: Int=Color.rgb(44,62,76)        // #2C3E4C
    val primary: Int=Color.rgb(57,68,188)       // #3944BC
    val primaryDark: Int=Color.rgb(41,51,145)
    val title: Int=Color.rgb(38,48,69)
    val muted: Int=Color.rgb(109,119,142)
    val canvas: Int=Color.rgb(246,247,253)
    val line: Int=Color.rgb(227,230,243)
    val pastel: Int=Color.rgb(238,239,255)
    val calm: Int=Color.rgb(118,126,219)
    val white: Int=Color.WHITE
    val danger: Int=Color.rgb(186,73,77)
    val good: Int=Color.rgb(36,129,106)

    fun dp(activity:Activity,value:Int):Int =
        (value*activity.resources.displayMetrics.density+0.5f).toInt()

    /** Narrow screens show full-width Persian fields instead of squeezed columns. */
    fun isCompact(activity:Activity):Boolean =
        activity.resources.configuration.screenWidthDp < 360

    /** Native visual touch response without replacing existing click listeners. */
    fun touch(activity:Activity,target:View,corner:Int=13) {
        target.isClickable=true
        target.isFocusable=true
        target.minimumHeight=dp(activity,48)
        target.foreground=RippleDrawable(
            ColorStateList.valueOf(Color.argb(46,57,68,188)),null,
            surface(activity,Color.WHITE,corner,Color.TRANSPARENT)
        )
    }

    private fun animationsEnabled(activity:Activity):Boolean = try {
        Settings.Global.getFloat(
            activity.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f
        )>0f
    }catch(_:Exception){true}

    /** Honor disabled system animations and do not delay screen navigation. */
    fun animateEntrance(activity:Activity,target:View) {
        if(!animationsEnabled(activity))return
        target.alpha=0f
        target.translationY=dp(activity,10).toFloat()
        target.post {
            if(target.isAttachedToWindow)target.animate()
                .alpha(1f).translationY(0f)
                .setDuration(210L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun surface(activity:Activity,color:Int=white,radius:Int=18,stroke:Int=line):GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius=dp(activity,radius).toFloat()
            if(stroke!=Color.TRANSPARENT)setStroke(dp(activity,1),stroke)
        }

    fun gradient(activity:Activity,radius:Int=22):GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(charcoal,primaryDark,primary)
        ).apply { cornerRadius=dp(activity,radius).toFloat() }

    fun label(
        activity:Activity,value:String,size:Float=14f,
        color:Int=title,bold:Boolean=false
    )=TextView(activity).apply {
        text=value
        textSize=size
        setTextColor(color)
        gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
        textDirection=View.TEXT_DIRECTION_FIRST_STRONG
        if(bold)setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD)
        includeFontPadding=true
    }

    fun section(activity:Activity,value:String):TextView =
        label(activity,value,17f,title,true).apply {
            setPadding(0,dp(activity,15),0,dp(activity,10))
        }

    fun banner(activity:Activity,titleText:String,subtitle:String,icon:String):View {
        val outer=LinearLayout(activity).apply {
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(activity,17),dp(activity,19),dp(activity,17),dp(activity,19))
            background=gradient(activity,21)
            elevation=dp(activity,4).toFloat()
        }
        animateEntrance(activity,outer)
        val text=LinearLayout(activity).apply {
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
        }
        text.addView(label(activity,titleText,21f,white,true))
        text.addView(label(activity,subtitle,11.5f,Color.rgb(220,226,255)).apply {
            setPadding(0,dp(activity,6),0,0)
        })
        outer.addView(text,LinearLayout.LayoutParams(0,-2,1f))
        val iconBackground=LinearLayout(activity).apply {
            gravity=Gravity.CENTER
            background=surface(activity,Color.rgb(75,82,187),17,Color.TRANSPARENT)
        }
        iconBackground.addView(glyph(activity,icon,Color.WHITE,26))
        outer.addView(iconBackground,LinearLayout.LayoutParams(
            dp(activity,57),dp(activity,57)
        ).apply {leftMargin=dp(activity,5)})
        return outer
    }

    fun glyph(activity:Activity,kind:String,tint:Int=primary,dimension:Int=22):ImageView =
        ImageView(activity).apply {
            setImageDrawable(LineIcon(kind,tint))
            layoutParams=LinearLayout.LayoutParams(dp(activity,dimension),dp(activity,dimension))
            contentDescription=kind
        }

    /**
     * Fixed bottom navigation: scroll content remains scrollable independently.
     * Existing pages keep their working forms and SQL-backed workflows.
     */
    fun mount(activity:Activity,scroll:ScrollView,current:String):View {
        val shell=LinearLayout(activity).apply {
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(canvas)
        }
        shell.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val navigation=bottom(activity,current)
        shell.addView(navigation,LinearLayout.LayoutParams(-1,dp(activity,72)))
        activity.window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        // Centralized insets prevent a double navigation-bar gap.
        scroll.setOnApplyWindowInsetsListener { view,insets ->
            val top=if(android.os.Build.VERSION.SDK_INT>=30)
                insets.getInsets(WindowInsets.Type.statusBars()).top
                else insets.systemWindowInsetTop
            view.setPadding(0,top,0,dp(activity,12))
            insets
        }
        shell.setOnApplyWindowInsetsListener { view,insets ->
            val bottom=if(android.os.Build.VERSION.SDK_INT>=30)
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                else insets.systemWindowInsetBottom
            if(android.os.Build.VERSION.SDK_INT>=30)
                navigation.visibility=if(insets.isVisible(WindowInsets.Type.ime()))
                    View.GONE else View.VISIBLE
            view.setPadding(0,0,0,bottom)
            insets
        }
        if(android.os.Build.VERSION.SDK_INT<30) {
            shell.viewTreeObserver.addOnGlobalLayoutListener {
                val rect=Rect()
                shell.getWindowVisibleDisplayFrame(rect)
                val covered=activity.resources.displayMetrics.heightPixels-rect.bottom
                navigation.visibility=if(covered>dp(activity,180))View.GONE else View.VISIBLE
            }
        }
        activity.window.navigationBarColor=Color.WHITE
        activity.window.statusBarColor=charcoal
        activity.window.decorView.systemUiVisibility =
            activity.window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        return shell
    }

    private data class Nav(
        val key:String,val name:String,val symbol:String,
        val target:Class<out Activity>
    )
    private val nav= listOf(
        Nav("home","خانه","home",MainActivity::class.java),
        Nav("workshops","کارگاه","building",WorkshopActivity::class.java),
        Nav("employees","کارکنان","users",EmployeeActivity::class.java),
        Nav("lists","لیست‌ها","list",InsuranceListActivity::class.java),
        Nav("reports","گزارش","report",InsuranceReportActivity::class.java)
    )

    private fun bottom(activity:Activity,current:String):View {
        val bar=LinearLayout(activity).apply {
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(activity,5),dp(activity,7),dp(activity,5),dp(activity,7))
            background=surface(activity,white,0,Color.TRANSPARENT)
            elevation=dp(activity,13).toFloat()
        }
        for(tab in nav) {
            val active=tab.key==current || (current=="details" && tab.key=="lists")
            val wrapper=LinearLayout(activity).apply {
                orientation=LinearLayout.VERTICAL
                gravity=Gravity.CENTER
                isClickable=true
                isFocusable=true
                setPadding(dp(activity,2),dp(activity,3),dp(activity,2),dp(activity,3))
                contentDescription=tab.name
                if(android.os.Build.VERSION.SDK_INT>=30)
                    stateDescription=if(active)"انتخاب‌شده" else "انتخاب‌نشده"
                touch(activity,this,14)
                setOnClickListener {
                    if(activity.javaClass==tab.target)return@setOnClickListener
                    val intent=Intent(activity,tab.target)
                    if(tab.key=="home")intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    else if(tab.key!="reports")
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    if(tab.key=="reports")intent.putExtra("pdf",true)
                    activity.startActivity(intent)
                }
            }
            val bubble=LinearLayout(activity).apply {
                gravity=Gravity.CENTER
                background=surface(activity,if(active)pastel else Color.TRANSPARENT,
                    13,Color.TRANSPARENT)
            }
            bubble.addView(glyph(activity,tab.symbol,if(active)primary else muted,21))
            wrapper.addView(bubble,LinearLayout.LayoutParams(dp(activity,52),dp(activity,34)))
            wrapper.addView(label(activity,tab.name,10.5f,
                if(active)primary else muted,active).apply{
                gravity=Gravity.CENTER
                maxLines=1
                ellipsize=android.text.TextUtils.TruncateAt.END
                setAutoSizeTextTypeUniformWithConfiguration(
                    9,11,1,android.util.TypedValue.COMPLEX_UNIT_SP
                )
                setPadding(0,dp(activity,3),0,0)
            })
            bar.addView(wrapper,LinearLayout.LayoutParams(0,-1,1f))
        }
        return bar
    }

    /** Crisp stroke icons independent of device emoji fonts. */
    private class LineIcon(private val kind:String,private val tint:Int):Drawable() {
        private val pen=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=tint
            strokeWidth=2.05f
            strokeCap=Paint.Cap.ROUND
            strokeJoin=Paint.Join.ROUND
            style=Paint.Style.STROKE
        }
        override fun draw(c:Canvas) {
            val b=bounds
            c.save()
            c.translate(b.left.toFloat(),b.top.toFloat())
            c.scale(b.width()/24f,b.height()/24f)
            fun line(x:Float,y:Float,xx:Float,yy:Float)=c.drawLine(x,y,xx,yy,pen)
            fun rect(l:Float,t:Float,r:Float,bt:Float,round:Float=2f)=
                c.drawRoundRect(l,t,r,bt,round,round,pen)
            fun circle(x:Float,y:Float,r:Float)=c.drawCircle(x,y,r,pen)
            when(kind) {
                "home" -> {
                    val p=Path().apply{
                        moveTo(3f,11f);lineTo(12f,3f);lineTo(21f,11f)
                    }
                    c.drawPath(p,pen)
                    line(5f,10f,5f,20f);line(19f,10f,19f,20f)
                    line(5f,20f,19f,20f);rect(10f,14f,14f,20f,0f)
                }
                "building" -> {
                    rect(5f,3f,19f,21f)
                    for(x in listOf(8f,13f))for(y in listOf(7f,11f,15f))
                        rect(x,y,x+2f,y+2f,0.3f)
                    rect(10f,18f,14f,21f,0f)
                }
                "users" -> {
                    circle(9f,8f,3f);circle(17f,9f,2f)
                    val p=Path().apply {
                        moveTo(3f,20f);lineTo(3f,17f);quadTo(3f,13f,9f,13f)
                        quadTo(15f,13f,15f,17f);lineTo(15f,20f)
                    }
                    c.drawPath(p,pen);line(3f,20f,15f,20f)
                    line(17f,14f,19f,14f);line(19f,14f,21f,17f);line(21f,17f,21f,20f)
                }
                "list" -> {
                    rect(4f,3f,20f,21f)
                    for(y in listOf(8f,12f,16f)){
                        line(10f,y,17f,y);circle(7f,y,0.5f)
                    }
                }
                "details" -> {
                    rect(3f,4f,21f,20f)
                    line(7f,10f,17f,10f)
                    line(7f,14f,13f,14f)
                    line(7f,17f,16f,17f)
                }
                "report" -> {
                    val p=Path().apply {
                        moveTo(6f,3f);lineTo(15f,3f);lineTo(20f,8f)
                        lineTo(20f,21f);lineTo(6f,21f);close()
                    };c.drawPath(p,pen)
                    line(15f,3f,15f,8f);line(15f,8f,20f,8f)
                    line(9f,13f,17f,13f);line(9f,17f,16f,17f)
                }
                "eye" -> {
                    val p=Path().apply{
                        moveTo(2f,12f);quadTo(12f,0f,22f,12f)
                        quadTo(12f,24f,2f,12f)
                    }
                    c.drawPath(p,pen);circle(12f,12f,3f)
                }
                "gear" -> {
                    circle(12f,12f,7.8f);circle(12f,12f,2.8f)
                    for(i in 0..7){
                        val a=i*Math.PI/4
                        val dx=kotlin.math.cos(a).toFloat()
                        val dy=kotlin.math.sin(a).toFloat()
                        line(12f+8f*dx,12f+8f*dy,12f+10.5f*dx,12f+10.5f*dy)
                    }
                }
                "shield" -> {
                    val p=Path().apply {
                        moveTo(12f,2f);lineTo(20f,5f);lineTo(20f,12f)
                        quadTo(20f,18f,12f,22f)
                        quadTo(4f,18f,4f,12f);lineTo(4f,5f);close()
                    };c.drawPath(p,pen)
                    line(8f,12f,11f,15f);line(11f,15f,16f,9f)
                }
                else -> {
                    rect(4f,4f,20f,20f,4f)
                    line(8f,12f,16f,12f)
                }
            }
            c.restore()
        }
        override fun setAlpha(alpha:Int){pen.alpha=alpha;invalidateSelf()}
        override fun setColorFilter(filter:android.graphics.ColorFilter?){
            pen.colorFilter=filter;invalidateSelf()
        }
        @Deprecated("Android graphics compatibility.")
        override fun getOpacity()=PixelFormat.TRANSLUCENT
    }
}
