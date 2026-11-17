package ir.saba.insurancedisk

import org.junit.Assert.*
import org.junit.Test

class MonthlyInsuranceRulesTest {
    @Test fun calculates1405AllowancesAndContributions(){
        val e=MonthlyInsuranceEntry(30,5_541_850L,22_000_000L,
            166_667L,5_000_000L,9_000_000L)
        val t=MonthlyInsuranceRules.calculate(1405,7,listOf(e))
        assertEquals(5_000_010L,t.seniority)
        assertEquals(198_255_510L,t.subjectGross)
        assertEquals(9_000_000L,t.nonSubject)
        assertEquals(13_877_886L,t.insured)
        assertEquals(39_651_102L,t.employer)
        assertEquals(5_947_665L,t.unemployment)
        assertEquals(59_476_653L,t.totalPayable)
    }
    @Test fun capsInsuredBasisWithoutChangingGrossSalary(){
        val e=MonthlyInsuranceEntry(30,100_000_000L,0L)
        val t=MonthlyInsuranceRules.calculate(1405,7,listOf(e))
        assertEquals(3_000_000_000L,t.subjectGross)
        assertEquals(1_163_788_500L,t.cappedSubject)
        assertEquals(1_836_211_500L,t.excludedByCap)
        assertTrue(t.warnings.any{it.contains("سقف")})
    }
    @Test fun reliefRequiresDocumentAndReducesEmployerOnly(){
        val entry=MonthlyInsuranceEntry(1,10_000_000L,0L)
        val t=MonthlyInsuranceRules.calculate(1405,1,listOf(entry),
            employerRelief=1_000_000L,reliefReference="تأییدیه شعبه ۱۲")
        assertEquals(2_000_000L,t.employer)
        assertEquals(1_000_000L,t.employerPayable)
        assertEquals(2_000_000L,t.totalPayable)
        assertThrows(IllegalArgumentException::class.java){
            MonthlyInsuranceRules.calculate(1405,1,listOf(entry),employerRelief=1L)
        }
        assertThrows(IllegalArgumentException::class.java){
            MonthlyInsuranceRules.calculate(1405,1,listOf(entry),
                employerRelief=2_000_001L,reliefReference="a")
        }
    }
    @Test fun supportsVerifiedSpecialRates(){
        val t=MonthlyInsuranceRules.calculate(1405,1,listOf(MonthlyInsuranceEntry(1,10000L,0L)),
            insuredRate=700,employerRate=1300,unemploymentRate=300)
        assertEquals(2300L,t.totalPayable)
        assertEquals(2300,MonthlyInsuranceRules.rateBps("۲۳")!!+0)
        assertEquals(1950,MonthlyInsuranceRules.rateBps("۱۹٫۵")!!)
        assertNull(MonthlyInsuranceRules.rateBps("19.555"))
    }
    @Test fun cannotMakeUpRatesForUnsupportedYearsOrZeroWork(){
        assertThrows(IllegalArgumentException::class.java){
            MonthlyInsuranceRules.calculate(1403,1,emptyList())
        }
        assertThrows(IllegalArgumentException::class.java){
            MonthlyInsuranceRules.calculate(1405,1,listOf(MonthlyInsuranceEntry(0,0L,100L)))
        }
        assertEquals(3_463_656L,MonthlyInsuranceRules.minimumDailyWage(1404)!!)
        assertEquals(0L,MonthlyInsuranceRules.calculate(1405,1,emptyList()).totalPayable)
    }
    @Test fun esfand1405HasOnly29Days(){
        assertThrows(IllegalArgumentException::class.java){
            MonthlyInsuranceRules.calculate(1405,12,listOf(MonthlyInsuranceEntry(30,100L,0L)))
        }
    }
    @Test fun rejectsArithmeticOverflow(){
        assertThrows(ArithmeticException::class.java){
            MonthlyInsuranceRules.calculate(1405,1,
                listOf(MonthlyInsuranceEntry(31,Long.MAX_VALUE,0L)))
        }
    }
}
