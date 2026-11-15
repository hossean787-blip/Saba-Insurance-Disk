package ir.saba.insurancedisk

/** Amount parsing and arithmetic independent from Android, for repeatable JVM testing. */
internal object WorkDetailRules {
    data class Calculation(val days:Int,val dailyWage:Long,val benefits:Long,val total:Long)

    fun parseNonnegative(raw:String):Long? {
        val value=EmployeeRules.normalizeDigits(raw.trim()
            .replace(",", "").replace("٬", ""))
        return if(value.matches(Regex("[0-9]+")))value.toLongOrNull() else null
    }

    /** Maximum workable calendar days. Esfand 30 is allowed for leap-year lists. */
    fun maxDays(month:Int)=when(month) {
        in 1..6 -> 31
        in 7..12 -> 30
        else -> 0
    }

    fun calculate(days:String,dailyWage:String,benefits:String,month:Int):Calculation? {
        val d=parseNonnegative(days) ?:return null
        val wage=parseNonnegative(dailyWage) ?:return null
        val allowance=parseNonnegative(benefits) ?:return null
        if(month !in 1..12 || d>maxDays(month) || (d>0 && wage==0L))return null
        return try {
            val total=Math.addExact(Math.multiplyExact(d,wage),allowance)
            Calculation(d.toInt(),wage,allowance,total)
        }catch(ex:ArithmeticException){null}
    }
}
