package ir.saba.insurancedisk
import org.junit.Assert.*
import org.junit.Test
class EmployeeRulesTest {
    @Test fun insurancePadsAndNormalizesPersianDigits() {
        assertEquals("0000012345",EmployeeRules.normalizeInsuranceNo("۱۲۳۴۵"))
        assertEquals("1234567890",EmployeeRules.normalizeInsuranceNo("١٢٣٤٥٦٧٨٩٠"))
        assertNull(EmployeeRules.normalizeInsuranceNo("۱۲۳۴۵۶۷۸۹۰۱"))
        assertNull(EmployeeRules.normalizeInsuranceNo("0"))
        assertNull(EmployeeRules.normalizeInsuranceNo("12a3"))
    }
    @Test fun nationalIdRejectsInvalidChecksum() {
        assertTrue(EmployeeRules.validNationalId("۱۲۳۴۵۶۷۸۹۱"))
        assertFalse(EmployeeRules.validNationalId("1234567890"))
        assertFalse(EmployeeRules.validNationalId("1111111111"))
    }
    @Test fun solarDateChecksMonthLengths() {
        assertTrue(EmployeeRules.validSolarDate("۱۳۷۰/۰۱/۳۱"))
        assertTrue(EmployeeRules.validSolarDate("1400/12/30"))
        assertFalse(EmployeeRules.validSolarDate("1400/07/31"))
        assertFalse(EmployeeRules.validSolarDate("1400/00/10"))
        assertFalse(EmployeeRules.validSolarDate("1400/12/31"))
        assertFalse(EmployeeRules.validSolarDate("1400/01/00"))
    }
    @Test fun jobCodeSupportsPersianDigits() {
        assertTrue(EmployeeRules.validJobCode("۱۲۳۴۵"))
        assertFalse(EmployeeRules.validJobCode("12A4"))
    }
}
