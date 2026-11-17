package ir.saba.insurancedisk

import android.database.sqlite.SQLiteDatabase

internal data class InsuranceReportHeader(
    val listId:Long, val workshopId:Long, val workshopName:String, val workshopCode:String,
    val contractRow:String, val employer:String, val address:String,
    val year:Int, val month:Int, val listNumber:Int, val insuranceRate:Double=0.0
)

internal data class InsuranceReportPerson(
    val employeeId:Long,
    val employeeWorkshopId:Long?,
    val name:String,
    val personnelNumber:String,
    val insuranceNumber:String,
    val nationalId:String,
    val jobTitle:String,
    val jobCode:String,
    val days:Int,
    val dailyWage:Long,
    val benefits:Long
)

internal data class InsuranceReportTotals(
    val employeeCount:Int, val days:Long, val wages:Long, val benefits:Long, val total:Long
)

internal data class InsuranceReportData(
    val header:InsuranceReportHeader,
    val people:List<InsuranceReportPerson>,
    val totals:InsuranceReportTotals,
    val warnings:List<String>,
    val premium:InsurancePremiumTotals?=null
)

internal object InsuranceReportSelection {
    fun canShare(savedListId:Long?,selectedListId:Long?,hasSavedUri:Boolean):Boolean =
        hasSavedUri && savedListId!=null && savedListId==selectedListId
}

internal object InsuranceReportMath {
    fun monthlyWage(person:InsuranceReportPerson):Long =
        Math.multiplyExact(person.days.toLong(),person.dailyWage)

    fun monthlyTotal(person:InsuranceReportPerson):Long =
        Math.addExact(monthlyWage(person),person.benefits)

    fun sum(people:List<InsuranceReportPerson>):InsuranceReportTotals {
        var days=0L
        var wages=0L
        var benefits=0L
        for(person in people) {
            days=Math.addExact(days,person.days.toLong())
            wages=Math.addExact(wages,monthlyWage(person))
            benefits=Math.addExact(benefits,person.benefits)
        }
        return InsuranceReportTotals(people.size,days,wages,benefits,Math.addExact(wages,benefits))
    }

    fun warnings(header:InsuranceReportHeader,people:List<InsuranceReportPerson>):List<String> {
        val problems=mutableListOf<String>()
        if(people.isEmpty())problems.add("برای این لیست هنوز ریزکارکردی ثبت نشده است.")
        if(header.month !in 1..12)problems.add("ماه دوره نامعتبر است.")
        if(!InsurancePremiumMath.isOrdinaryRate(header.insuranceRate))
            problems.add("این کارگاه نرخ اختصاصی دارد؛ تفکیک سهم بیمه نیازمند بررسی است.")
        val maxDays=WorkDetailRules.maxDays(header.month)
        for((index,person) in people.withIndex()) {
            val row=index+1
            if(person.employeeWorkshopId!=header.workshopId) {
                problems.add("ردیف $row: کارمند به کارگاه این لیست متصل نیست.")
            }
            if(person.name.isBlank() || person.insuranceNumber.isBlank()) {
                problems.add("ردیف $row: اطلاعات هویتی یا شماره بیمه ناقص است.")
            }
            if(person.days<0 || person.days>maxDays ||
                person.dailyWage<0L || person.benefits<0L) {
                problems.add("ردیف $row: روز کارکرد یا مبلغ نامعتبر است.")
            }
            InsurancePremiumMath.wageWarning(header.year,person.dailyWage)?.let{
                problems.add("ردیف $row: $it")
            }
        }
        return problems
    }
}

/** Snapshot reader, never mutates user records or their existing database schema. */
internal class InsuranceReportRepository(private val database:SQLiteDatabase) {
    fun load(listId:Long):InsuranceReportData {
        val header=database.rawQuery(
            """SELECT l.id,l.workshop_id,w.name,w.code,w.contract_row,w.employer,w.address,
                      l.year,l.month,l.list_number,w.insurance_rate
               FROM insurance_lists l JOIN workshops w ON w.id=l.workshop_id
               WHERE l.id=?""",arrayOf(listId.toString())
        ).use { c ->
            if(!c.moveToFirst())throw IllegalStateException("لیست یا کارگاه انتخاب‌شده پیدا نشد.")
            InsuranceReportHeader(
                c.getLong(0),c.getLong(1),c.getString(2) ?: "",c.getString(3) ?: "",
                c.getString(4) ?: "",c.getString(5) ?: "",c.getString(6) ?: "",
                c.getInt(7),c.getInt(8),c.getInt(9),c.getDouble(10)
            )
        }
        val people=mutableListOf<InsuranceReportPerson>()
        database.rawQuery(
            """SELECT i.employee_id,e.workshop_id,
                      COALESCE(NULLIF(TRIM(e.first_name || ' ' || e.last_name),''),
                               NULLIF(e.name,''),''),
                      COALESCE(e.personnel_no,''),COALESCE(e.insurance_no,''),
                      COALESCE(e.national_id,''),COALESCE(e.job_title,''),
                      COALESCE(e.job_code,''),i.days,i.daily_wage,i.benefits
               FROM insurance_list_items i
               LEFT JOIN employees e ON e.id=i.employee_id
               WHERE i.list_id=?
               ORDER BY e.last_name,e.first_name,i.id""",arrayOf(listId.toString())
        ).use { c ->
            while(c.moveToNext())people.add(InsuranceReportPerson(
                c.getLong(0),if(c.isNull(1))null else c.getLong(1),
                c.getString(2) ?: "",c.getString(3) ?: "",c.getString(4) ?: "",
                c.getString(5) ?: "",c.getString(6) ?: "",c.getString(7) ?: "",
                c.getInt(8),c.getLong(9),c.getLong(10)
            ))
        }
        return InsuranceReportData(
            header,people,InsuranceReportMath.sum(people),
            InsuranceReportMath.warnings(header,people),
            if(InsurancePremiumMath.isOrdinaryRate(header.insuranceRate))
                InsurancePremiumMath.estimate(people.map { InsuranceReportMath.monthlyTotal(it) })
            else null
        )
    }
}
