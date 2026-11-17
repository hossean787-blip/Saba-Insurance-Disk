package ir.saba.insurancedisk

import org.junit.Assert.*
import org.junit.Test

class InsurancePremiumMathTest {
    @Test fun ordinaryRateDefaultsToThirtyPercentOnly(){
        assertTrue(InsurancePremiumMath.isOrdinaryRate(0.0))
        assertTrue(InsurancePremiumMath.isOrdinaryRate(30.0))
        assertFalse(InsurancePremiumMath.isOrdinaryRate(27.0))
        assertFalse(InsurancePremiumMath.isOrdinaryRate(23.0))
    }
    @Test fun calculatesStandardSharesInRials(){
        val p=InsurancePremiumMath.estimate(listOf(1_000_000L))
        assertEquals(1_000_000L,p.base)
        assertEquals(70_000L,p.insured)
        assertEquals(200_000L,p.employer)
        assertEquals(30_000L,p.unemployment)
        assertEquals(230_000L,p.employerWithUnemployment)
        assertEquals(300_000L,p.total)
    }
    @Test fun roundsEachEmployeeThenAggregates(){
        val p=InsurancePremiumMath.estimate(listOf(108L,108L))
        assertEquals(216L,p.base)
        assertEquals(16L,p.insured)
        assertEquals(44L,p.employer)
        assertEquals(6L,p.unemployment)
        assertEquals(66L,p.total)
    }
    @Test fun zerosNegativeAndOverflow(){
        assertEquals(0L,InsurancePremiumMath.estimate(emptyList()).total)
        assertThrows(IllegalArgumentException::class.java){
            InsurancePremiumMath.estimate(listOf(-1L))
        }
        assertThrows(ArithmeticException::class.java){
            InsurancePremiumMath.estimate(listOf(Long.MAX_VALUE,1L))
        }
    }
    @Test fun periodSpecificWageWarnings(){
        assertNull(InsurancePremiumMath.wageWarning(1405,5_541_850L))
        assertNotNull(InsurancePremiumMath.wageWarning(1405,5_541_849L))
        assertNotNull(InsurancePremiumMath.wageWarning(1405,38_792_951L))
        assertNull(InsurancePremiumMath.wageWarning(1404,5_000_000L))
    }
}
