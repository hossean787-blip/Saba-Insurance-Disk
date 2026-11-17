package ir.saba.insurancedisk

import org.junit.Assert.*
import org.junit.Test

class WorkDetailExtendedTest {
    @Test fun separatesSubjectAndNonSubjectAndAddsDailySeniority(){
        val r=WorkDetailRules.calculateExtended("۳۰","۵۵۴۱۸۵۰","۲۲۰۰۰۰۰۰","۱۶۶۶۶۷","۵۰۰۰۰۰۰","۱۰۰۰۰۰۰",7)!!
        assertEquals(5_000_010L,r.seniorityMonthly)
        assertEquals(198_255_510L,r.subjectTotal)
        assertEquals(199_255_510L,r.grossTotal)
    }
    @Test fun legacyCalculatorRemainsCompatible(){
        assertEquals(1000L,WorkDetailRules.calculate("1","700","300",1)!!.total)
    }
    @Test fun refusesNegativeNonnumericAndOverflow(){
        assertNull(WorkDetailRules.calculateExtended("1","100","0","-1","0","0",1))
        assertNull(WorkDetailRules.calculateExtended("1","100","0","0","x","0",1))
        assertNull(WorkDetailRules.calculateExtended("31","100","0","0","0","0",7))
        assertNull(WorkDetailRules.calculateExtended("1","1","0","0","0","9223372036854775807",1))
    }
}
