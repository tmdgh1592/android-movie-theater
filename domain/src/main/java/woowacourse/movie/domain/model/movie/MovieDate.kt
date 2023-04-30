package woowacourse.movie.domain.model.movie

import woowacourse.movie.domain.model.discount.discountable.Discountable
import java.time.DayOfWeek
import java.time.LocalDate

typealias DomainMovieDate = MovieDate

data class MovieDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Discountable {
    constructor(date: LocalDate) : this(date.year, date.monthValue, date.dayOfMonth)

    fun isWeekend(): Boolean {
        val dayOfWeek = LocalDate.of(year, month, day).dayOfWeek
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
    }

    fun isToday(): Boolean {
        val today = LocalDate.now()
        return today.compareTo(LocalDate.of(year, month, day)) == 0
    }

    fun checkInRange(startDate: LocalDate, endDate: LocalDate) {
        require(toLocalDate() in startDate..endDate) {
            OUT_OF_RESERVATION_RANGE_ERROR_MESSAGE
        }
    }

    private fun toLocalDate(): LocalDate = LocalDate.of(year, month, day)

    override fun isDiscountable(): Boolean = day in DISCOUNT_DAYS

    companion object {
        private val DISCOUNT_DAYS = listOf(10, 20, 30)
        private const val OUT_OF_RESERVATION_RANGE_ERROR_MESSAGE = "예매 가능한 날짜가 아닙니다."

        private infix fun LocalDate.max(other: LocalDate): LocalDate =
            if (this > other) this else other

        @JvmOverloads
        fun releaseDates(
            from: LocalDate,
            to: LocalDate,
            today: LocalDate = LocalDate.now(),
        ): List<MovieDate> {
            if (today > to) return emptyList()
            return generateSequence(today max from) { it.plusDays(1) }
                .takeWhile { it <= to }
                .map { MovieDate(it) }
                .toList()
        }
    }
}
