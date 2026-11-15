package ir.saba.insurancedisk

import org.junit.Assert.*
import org.junit.Test

class InsuranceReportMathTest {
    private fun header(month:Int=7)=InsuranceReportHeader(
        1L,10L,"کارگاه نمونه","9005467378","054","کارفرما","اراک",
        1405,month,1
    )
    private fun person(
        id:Long=2L,workshopId:Long?=10L,
        days:Int=30,wage:Long=5_541_850L,benefits:Long=0L
    )=InsuranceReportPerson(
        id,workshopId,"علی رضایی","000001","0012345678","0012345678",
        "حسابدار","1234",days,wage,benefits
    )

    @Test fun computesDailyToMonthlyWageExactly(){
        val p=person()
        assertEquals(166_255_500L,InsuranceReportMath.monthlyWage(p))
        assertEquals(166_255_500L,InsuranceReportMath.monthlyTotal(p))
    }

    @Test fun aggregatesWagesBenefitsAndDaysWithoutDoubleCounting(){
        val list=listOf(person(),person(3L,days=20,wage=1_000_000L,benefits=120_000L))
        val result=InsuranceReportMath.sum(list)
        assertEquals(2,result.employeeCount)
        assertEquals(50L,result.days)
        assertEquals(186_255_500L,result.wages)
        assertEquals(120_000L,result.benefits)
        assertEquals(186_375_500L,result.total)
    }

    @Test fun emptyListIsZeroAndWarns(){
        val totals=InsuranceReportMath.sum(emptyList())
        assertEquals(0,totals.employeeCount)
        assertEquals(0L,totals.total)
        assertTrue(InsuranceReportMath.warnings(header(),emptyList()).isNotEmpty())
    }

    @Test fun flagsWrongWorkshopAndImpossibleWorkdays(){
        val problems=InsuranceReportMath.warnings(
            header(7),listOf(person(workshopId=11L,days=31))
        )
        assertTrue(problems.any{it.contains("کارگاه")})
        assertTrue(problems.any{it.contains("روز کارکرد")})
    }

    @Test fun flagsMissingLegacyEmployeeRecord(){
        val record=person(workshopId=null).copy(name="",insuranceNumber="")
        val problems=InsuranceReportMath.warnings(header(),listOf(record))
        assertTrue(problems.size>=2)
    }

    @Test fun checksArithmeticOverflowRatherThanWrapping(){
        assertThrows(ArithmeticException::class.java){
            InsuranceReportMath.monthlyTotal(person(days=2,wage=Long.MAX_VALUE))
        }
        assertThrows(ArithmeticException::class.java){
            InsuranceReportMath.sum(listOf(
                person(days=1,wage=Long.MAX_VALUE),
                person(3L,days=1,wage=100L)
            ))
        }
    }

    @Test fun cannotShareFileBelongingToAnotherList() {
        assertTrue(InsuranceReportSelection.canShare(12L,12L,true))
        assertFalse(InsuranceReportSelection.canShare(12L,13L,true))
        assertFalse(InsuranceReportSelection.canShare(12L,null,true))
        assertFalse(InsuranceReportSelection.canShare(null,12L,true))
        assertFalse(InsuranceReportSelection.canShare(12L,12L,false))
    }

    @Test fun filenameUsesListPeriodAndSafeWorkshopCode(){
        val data=InsuranceReportData(
            header(),emptyList(),InsuranceReportMath.sum(emptyList()),emptyList()
        )
        assertEquals(
            "SURA-Insurance-9005467378-1405-07-01.pdf",
            InsurancePdfWriter.fileName(data)
        )
    }
}
