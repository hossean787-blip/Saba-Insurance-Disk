package ir.saba.insurancedisk

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Calculation for insured subject wages, statutory period cap, and explicitly
 * verified employer-side relief. Does not determine legal relief eligibility.
 * All money is in RIALS, and rates are hundredths of a percent (700 = 7%).
 */
internal data class MonthlyInsuranceEntry(
    val days:Int,val dailyWage:Long,val benefits:Long,
    val seniorityDaily:Long=0L,val maritalAllowance:Long=0L,
    val nonSubjectBenefits:Long=0L
)
internal data class MonthlyInsuranceTotals(
    val people:Int,
    val days:Long,
    val wages:Long,
    val seniority:Long,
    val marital:Long,
    val otherSubject:Long,
    val nonSubject:Long,
    val subjectGross:Long,
    val cappedSubject:Long,
    val excludedByCap:Long,
    val insured:Long,
    val employer:Long,
    val unemployment:Long,
    val approvedEmployerRelief:Long,
    val employerPayable:Long,
    val totalPayable:Long,
    val warnings:List<String>
)
internal object MonthlyInsuranceRules {
    fun minimumDailyWage(year:Int):Long?=when(year) {
        1404 -> 3_463_656L
        1405 -> 5_541_850L
        else -> null
    }
    fun rateBps(raw:String):Int? {
        val normalized=EmployeeRules.normalizeDigits(raw.trim()).replace('٫','.')
        if(!normalized.matches(Regex("[0-9]{1,3}(\\.[0-9]{1,2})?")))return null
        return try{
            val bps=BigDecimal(normalized).movePointRight(2).intValueExact()
            bps.takeIf{it in 0..10000}
        }catch(e:ArithmeticException){null}
    }
    fun formatRate(bps:Int):String=BigDecimal(bps).movePointLeft(2).stripTrailingZeros().toPlainString()

    private fun roundRate(base:Long,bps:Int):Long {
        require(base>=0L && bps in 0..10000) {"مبلغ یا نرخ نامعتبر است"}
        return BigInteger.valueOf(base)
            .multiply(BigInteger.valueOf(bps.toLong()))
            .add(BigInteger.valueOf(5000L))
            .divide(BigInteger.valueOf(10000L)).longValueExact()
    }
    fun validateRates(insured:Int,employer:Int,unemployment:Int):Boolean =
        insured in 0..10000 && employer in 0..10000 &&
            unemployment in 0..10000 && insured+employer+unemployment in 1..10000

    /** No minimum-wage inflation: under-minimum pay is flagged, never silently changed. */
    fun calculate(
        year:Int,month:Int,entries:List<MonthlyInsuranceEntry>,
        insuredRate:Int=700,employerRate:Int=2000,unemploymentRate:Int=300,
        employerRelief:Long=0L,reliefReference:String=""
    ):MonthlyInsuranceTotals {
        val minimum=minimumDailyWage(year)
            ?:throw IllegalArgumentException("قواعد دستمزد این سال ثبت نشده است؛ دوره ۱۴۰۴ یا ۱۴۰۵ را انتخاب کنید")
        require(month in 1..12) {"ماه نامعتبر است"}
        require(validateRates(insuredRate,employerRate,unemploymentRate)){
            "نرخ‌های سهم بیمه نامعتبر هستند"
        }
        require(employerRelief>=0L) {"مبلغ معافیت نباید منفی باشد"}
        require(employerRelief==0L || reliefReference.trim().isNotEmpty()){
            "برای معافیت تأییدشده، شماره مستند الزامی است"
        }
        val maxDays=WorkDetailRules.maxDays(month)
        val maxDaily=Math.multiplyExact(minimum,7L)
        var days=0L;var wages=0L;var seniority=0L;var marital=0L
        var benefits=0L;var nonSubject=0L;var gross=0L;var capped=0L
        var insured=0L;var employer=0L;var unemployment=0L
        val warnings=mutableListOf<String>()
        entries.forEachIndexed{index,entry->
            val row=index+1
            require(entry.days in 0..maxDays &&
                entry.dailyWage>=0L && entry.benefits>=0L &&
                entry.seniorityDaily>=0L && entry.maritalAllowance>=0L &&
                entry.nonSubjectBenefits>=0L) {"ردیف $row: روز یا مبلغ نامعتبر است"}
            val wage=Math.multiplyExact(entry.days.toLong(),entry.dailyWage)
            val senior=Math.multiplyExact(entry.days.toLong(),entry.seniorityDaily)
            val subject=Math.addExact(Math.addExact(wage,senior),
                Math.addExact(entry.benefits,entry.maritalAllowance))
            if(entry.days==0 && subject>0L) {
                throw IllegalArgumentException("ردیف $row: مزایای مشمول با کارکرد صفر نیازمند بررسی است")
            }
            val ceiling=Math.multiplyExact(entry.days.toLong(),maxDaily)
            val basis=minOf(subject,ceiling)
            if(subject>ceiling)warnings.add("ردیف $row: بخشی از مزد مشمول به سقف هفت‌برابر محدود شد")
            if(entry.days>0 && entry.dailyWage<minimum)
                warnings.add("ردیف $row: مزد روزانه از حداقل مصوب سال $year کمتر است؛ وضعیت قانونی را بررسی کنید")
            days=Math.addExact(days,entry.days.toLong())
            wages=Math.addExact(wages,wage)
            seniority=Math.addExact(seniority,senior)
            marital=Math.addExact(marital,entry.maritalAllowance)
            benefits=Math.addExact(benefits,entry.benefits)
            nonSubject=Math.addExact(nonSubject,entry.nonSubjectBenefits)
            gross=Math.addExact(gross,subject)
            capped=Math.addExact(capped,basis)
            insured=Math.addExact(insured,roundRate(basis,insuredRate))
            employer=Math.addExact(employer,roundRate(basis,employerRate))
            unemployment=Math.addExact(unemployment,roundRate(basis,unemploymentRate))
        }
        require(employerRelief<=employer) {
            "معافیت ثبت‌شده نمی‌تواند از کل سهم کارفرما بیشتر باشد"
        }
        val payable=Math.subtractExact(employer,employerRelief)
        val due=Math.addExact(Math.addExact(insured,payable),unemployment)
        return MonthlyInsuranceTotals(entries.size,days,wages,seniority,marital,benefits,
            nonSubject,gross,capped,Math.subtractExact(gross,capped),insured,employer,
            unemployment,employerRelief,payable,due,warnings)
    }
}
