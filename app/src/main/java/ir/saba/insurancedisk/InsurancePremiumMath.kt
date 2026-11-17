package ir.saba.insurancedisk

import java.math.BigInteger

/**
 * Provisional insurance-share calculation for ordinary 30% employment coverage.
 * Inputs contain only wages and benefits already recorded as insurance-subject.
 * Exemptions, special rates and annual wage ceilings need separate verification.
 * The 4% hazardous-occupation retirement liability is not a monthly charge.
 */
internal data class InsurancePremiumTotals(
    val base:Long,
    val insured:Long,
    val employer:Long,
    val unemployment:Long
) {
    val employerWithUnemployment:Long get()=Math.addExact(employer,unemployment)
    val total:Long get()=Math.addExact(insured,employerWithUnemployment)
}

internal object InsurancePremiumMath {
    const val MIN_DAILY_WAGE_1405=5_541_850L
    const val MAX_DAILY_WAGE_1405=7L*MIN_DAILY_WAGE_1405

    /** Zero is the unset optional workshop rate, meaning ordinary 30% here. */
    fun isOrdinaryRate(rate:Double):Boolean=rate==0.0 || rate==30.0

    private fun portion(base:Long,percentage:Int):Long {
        require(base>=0L) {"مزد مشمول نمی‌تواند منفی باشد"}
        return BigInteger.valueOf(base)
            .multiply(BigInteger.valueOf(percentage.toLong()))
            .add(BigInteger.valueOf(50L))
            .divide(BigInteger.valueOf(100L)).longValueExact()
    }

    /** Round individual contributions to whole rials, then sum exactly. */
    fun estimate(employeeBases:Iterable<Long>):InsurancePremiumTotals {
        var base=0L;var insured=0L;var employer=0L;var unemployment=0L
        for(employeeBase in employeeBases){
            base=Math.addExact(base,employeeBase)
            insured=Math.addExact(insured,portion(employeeBase,7))
            employer=Math.addExact(employer,portion(employeeBase,20))
            unemployment=Math.addExact(unemployment,portion(employeeBase,3))
        }
        return InsurancePremiumTotals(base,insured,employer,unemployment)
    }

    fun wageWarning(year:Int,dailyWage:Long):String?{
        if(year!=1405 || dailyWage==0L)return null
        return when {
            dailyWage<MIN_DAILY_WAGE_1405 ->
                "مزد روزانه کمتر از حداقل سال ۱۴۰۵ است؛ وضعیت استثنا را بررسی کنید."
            dailyWage>MAX_DAILY_WAGE_1405 ->
                "مزد روزانه از سقف روزانه سال ۱۴۰۵ بیشتر است؛ مبنای حق بیمه را بررسی کنید."
            else -> null
        }
    }
}
