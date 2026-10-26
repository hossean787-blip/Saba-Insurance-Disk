package ir.saba.insurancedisk
import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.widget.*
class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(32,48,32,24); setBackgroundColor(Color.rgb(14,25,45)) }
  root.addView(TextView(this).apply { text="لیست دیسک صبا"; textSize=27f; setTextColor(Color.WHITE) })
  root.addView(TextView(this).apply { text="مدیریت لیست بیمه | نسخه آزمایشی"; textSize=16f; setTextColor(Color.WHITE) })
  listOf("کارگاه‌ها","کارکنان","لیست ماهانه","کنترل خطا","خروجی DBF").forEach { name -> root.addView(Button(this).apply { text=name; setOnClickListener { Toast.makeText(this@MainActivity,"در دست توسعه",Toast.LENGTH_SHORT).show() } }) }
  setContentView(root)
 }
}