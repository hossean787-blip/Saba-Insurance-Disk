package ir.saba.insurancedisk

import org.junit.Assert.*
import org.junit.Test

class WorkDetailRulesTest {
    @Test fun acceptsValidMonthlyWorkAndPersianNumbers() {
        val result=WorkDetailRules.calculate("۳۰","۵۵۴۱۸۵۰","۱۲۰۰۰",7)
        assertNotNull(result)
        assertEquals(30,result!!.days)
        assertEquals(5_541_850L,result.dailyWage)
        assertEquals(166_267_500L,result.total)
    }

    @Test fun calculatesBenefitWithoutDaysOrWage() {
        val r=WorkDetailRules.calculate("0","0","125000",1)
        assertEquals(125000L,r!!.total)
    }

    @Test fun rejectsInvalidMonthAndDayCount() {
        assertNull(WorkDetailRules.calculate("31","10","0",7))
        assertNotNull(WorkDetailRules.calculate("31","10","0",6))
        assertNull(WorkDetailRules.calculate("30","10","0",13))
        assertNull(WorkDetailRules.calculate("99999999999999","10","0",1))
    }

    @Test fun rejectsNonNumericNegativeAndOverflow() {
        assertNull(WorkDetailRules.calculate("-1","100","0",1))
        assertNull(WorkDetailRules.calculate("1","12a","0",1))
        assertNull(WorkDetailRules.calculate("1","0","0",1))
        assertNull(WorkDetailRules.calculate("31","9223372036854775807","1",1))
        assertNull(WorkDetailRules.calculate("1","9223372036854775807","1",1))
    }

    @Test fun placeholderIndicesNeverSelectTheWrongEmployee() {
        val employees=listOf(101L,202L,303L)
        assertNull(WorkDetailRules.fromPlaceholder(employees,0))
        assertNull(WorkDetailRules.fromPlaceholder(employees,-1))
        assertEquals(101L,WorkDetailRules.fromPlaceholder(employees,1))
        assertEquals(202L,WorkDetailRules.fromPlaceholder(employees,2))
        assertEquals(303L,WorkDetailRules.fromPlaceholder(employees,3))
        assertNull(WorkDetailRules.fromPlaceholder(employees,4))
    }
    @Test fun employeeSearchIncludesPersonnelNumber() {
        assertTrue(WorkDetailRules.matchesSearch("علی رضایی","0012003456","000025",3L,"۲۵"))
        assertTrue(WorkDetailRules.matchesSearch("علی رضایی","0012003456","000025",3L,"رضایی"))
        assertTrue(WorkDetailRules.matchesSearch("علی رضایی","0012003456","000025",3L,""))
        assertFalse(WorkDetailRules.matchesSearch("علی رضایی","0012003456","000025",3L,"99999"))
    }

    @Test fun acceptsDigitSeparators() {
        val r=WorkDetailRules.calculate("۱","۱٬۰۰۰٬۰۰۰","۲۰,۰۰۰",1)
        assertEquals(1_020_000L,r!!.total)
    }
}
