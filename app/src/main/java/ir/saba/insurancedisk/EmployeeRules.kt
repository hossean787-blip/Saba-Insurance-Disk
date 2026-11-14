package ir.saba.insurancedisk

/** Pure validation rules used by employee editor and JVM tests. */
internal object EmployeeRules {
    fun normalizeDigits(value:String):String = buildString(value.length) {
        for(ch in value) append(when(ch) {
            in '۰'..'۹' -> ('0'.code + (ch.code-'۰'.code)).toChar()
            in '٠'..'٩' -> ('0'.code + (ch.code-'٠'.code)).toChar()
            else -> ch
        })
    }
    fun normalizeInsuranceNo(raw:String):String? {
        val digits=normalizeDigits(raw.trim())
        if(!digits.matches(Regex("[0-9]{1,10}")) || digits.all { it=='0' })return null
        return digits.padStart(10,'0')
    }
    fun validNationalId(raw:String):Boolean {
        val value=normalizeDigits(raw.trim())
        if(!value.matches(Regex("[0-9]{10}")) || value.toSet().size==1)return false
        val sum=(0..8).sumOf { (value[it]-'0')*(10-it) }
        val rem=sum%11
        val check=value[9]-'0'
        return check == if(rem<2)rem else 11-rem
    }
    fun validSolarDate(raw:String):Boolean {
        val date=normalizeDigits(raw.trim())
        if(!date.matches(Regex("[0-9]{4}/[0-9]{2}/[0-9]{2}")))return false
        val parts=date.split("/")
        val year=parts[0].toInt()
        val month=parts[1].toInt()
        val day=parts[2].toInt()
        val maxDay=when(month) {
            in 1..6 -> 31
            in 7..12 -> 30
            else -> 0
        }
        return year in 1200..1499 && day in 1..maxDay
    }
    fun validJobCode(raw:String):Boolean =
        normalizeDigits(raw.trim()).matches(Regex("[0-9]+"))
}
