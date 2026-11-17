package ir.saba.insurancedisk

/** Amount parsing and arithmetic independent from Android, for repeatable JVM testing. */
internal object WorkDetailRules {
    data class Calculation(val days:Int,val dailyWage:Long,val benefits:Long,val total:Long)
    data class ExtendedCalculation(
        val days:Int,val dailyWage:Long,val benefits:Long,
        val seniorityDaily:Long,val maritalAllowance:Long,
        val nonSubjectBenefits:Long,val subjectTotal:Long,val grossTotal:Long
    ){
        val seniorityMonthly:Long get()=Math.multiplyExact(days.toLong(),seniorityDaily)
        val subjectAllowances:Long get()=Math.addExact(
            Math.addExact(benefits,seniorityMonthly),maritalAllowance)
    }

    fun <T> fromPlaceholder(items:List<T>,selectedIndex:Int):T? =
        if(selectedIndex<=0)null else items.getOrNull(selectedIndex-1)

    fun matchesSearch(
        name:String,insuranceNo:String,personnelNo:String,employeeId:Long,query:String
    ):Boolean {
        val needle=EmployeeRules.normalizeDigits(query.trim())
        val haystack=EmployeeRules.normalizeDigits(
            name + " " + insuranceNo + " " + personnelNo + " " + employeeId
        )
        return needle.isEmpty() || haystack.contains(needle,ignoreCase=true)
    }

    fun parseNonnegative(raw:String):Long? {
        val value=EmployeeRules.normalizeDigits(raw.trim()
            .replace(",", "").replace("٬", ""))
        return if(value.matches(Regex("[0-9]+")))value.toLongOrNull() else null
    }

    /** Maximum workable calendar days. Esfand 30 is allowed for leap-year lists. */
    fun maxDays(month:Int,year:Int?=null)=when {
        month in 1..6 -> 31
        month in 7..11 -> 30
        month==12 && (year==1404 || year==1405) -> 29
        month==12 -> 30
        else -> 0
    }

    fun calculateExtended(
        days:String,dailyWage:String,benefits:String,
        seniorityDaily:String,maritalAllowance:String,nonSubjectBenefits:String,
        month:Int,year:Int?=null
    ):ExtendedCalculation? {
        val base=calculate(days,dailyWage,benefits,month,year) ?:return null
        val seniority=parseNonnegative(seniorityDaily) ?:return null
        val marital=parseNonnegative(maritalAllowance) ?:return null
        val nonSubject=parseNonnegative(nonSubjectBenefits) ?:return null
        return try{
            val subject=Math.addExact(
                Math.addExact(base.total,Math.multiplyExact(base.days.toLong(),seniority)),
                marital)
            val gross=Math.addExact(subject,nonSubject)
            ExtendedCalculation(base.days,base.dailyWage,base.benefits,
                seniority,marital,nonSubject,subject,gross)
        }catch(e:ArithmeticException){null}
    }

    fun calculate(days:String,dailyWage:String,benefits:String,
        month:Int,year:Int?=null):Calculation? {
        val d=parseNonnegative(days) ?:return null
        val wage=parseNonnegative(dailyWage) ?:return null
        val allowance=parseNonnegative(benefits) ?:return null
        if(month !in 1..12 || d>maxDays(month,year) || (d>0 && wage==0L))return null
        return try {
            val total=Math.addExact(Math.multiplyExact(d,wage),allowance)
            Calculation(d.toInt(),wage,allowance,total)
        }catch(ex:ArithmeticException){null}
    }
}
