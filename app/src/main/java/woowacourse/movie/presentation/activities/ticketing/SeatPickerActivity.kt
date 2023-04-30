package woowacourse.movie.presentation.activities.ticketing

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import woowacourse.movie.R
import woowacourse.movie.domain.model.discount.policy.MovieDayDiscountPolicy
import woowacourse.movie.domain.model.discount.policy.MovieTimeDiscountPolicy
import woowacourse.movie.domain.model.movie.DomainTicketPrice
import woowacourse.movie.domain.model.seat.DomainPickedSeats
import woowacourse.movie.domain.model.seat.DomainSeat
import woowacourse.movie.presentation.activities.main.alarm.PushAlarmManager
import woowacourse.movie.presentation.activities.main.fragments.home.HomeFragment
import woowacourse.movie.presentation.activities.ticketingresult.TicketingResultActivity
import woowacourse.movie.presentation.extensions.createAlertDialog
import woowacourse.movie.presentation.extensions.getParcelableCompat
import woowacourse.movie.presentation.extensions.message
import woowacourse.movie.presentation.extensions.negativeButton
import woowacourse.movie.presentation.extensions.positiveButton
import woowacourse.movie.presentation.extensions.showBackButton
import woowacourse.movie.presentation.extensions.showToast
import woowacourse.movie.presentation.extensions.title
import woowacourse.movie.presentation.mapper.toDomain
import woowacourse.movie.presentation.mapper.toPresentation
import woowacourse.movie.presentation.model.MovieDate
import woowacourse.movie.presentation.model.MovieTime
import woowacourse.movie.presentation.model.PickedSeats
import woowacourse.movie.presentation.model.Reservation
import woowacourse.movie.presentation.model.Seat
import woowacourse.movie.presentation.model.SeatColumn
import woowacourse.movie.presentation.model.SeatRow
import woowacourse.movie.presentation.model.Ticket
import woowacourse.movie.presentation.model.TicketPrice
import woowacourse.movie.presentation.model.movieitem.Movie
import woowacourse.movie.presentation.receiver.ReservationPushReceiver

class SeatPickerActivity : AppCompatActivity(), View.OnClickListener {
    private var pickedSeats = DomainPickedSeats()
    private val seatRowSize: Int = 5
    private val seatColSize: Int = 4

    private val movieDate by lazy {
        intent.getParcelableCompat<MovieDate>(TicketingActivity.MOVIE_DATE_KEY)!!.toDomain()
    }
    private val movieTime by lazy {
        intent.getParcelableCompat<MovieTime>(TicketingActivity.MOVIE_TIME_KEY)!!.toDomain()
    }
    private val ticket by lazy { intent.getParcelableCompat<Ticket>(TicketingActivity.TICKET_KEY)!! }
    private val movie by lazy { intent.getParcelableCompat<Movie>(HomeFragment.MOVIE_KEY)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreState(savedInstanceState)
        setContentView(R.layout.activity_seat_picker)
        initView()
    }

    private fun restoreState(instanceState: Bundle?) {
        instanceState?.getParcelableCompat<PickedSeats>(PICKED_SEATS_KEY)?.let { restoredSeats ->
            pickedSeats = restoredSeats.toDomain()
        }
    }

    private fun initView() {
        showBackButton()
        showMovieTitle()
        initViewClickListener()
        updateDoneBtnEnabled(!canPick())
        updateTotalPriceView(calculateTotalPrice().toPresentation())
        initSeatTable(seatRowSize, seatColSize)
    }

    private fun showMovieTitle() {
        findViewById<TextView>(R.id.movie_title_tv).text = movie.title
    }

    private fun initViewClickListener() {
        findViewById<TextView>(R.id.done_btn).setOnClickListener(this)
    }

    private fun updateDoneBtnEnabled(isEnabled: Boolean) {
        findViewById<TextView>(R.id.done_btn).isEnabled = isEnabled
    }

    private fun canPick(): Boolean =
        pickedSeats.canPick(ticket.toDomain())

    private fun updateTotalPriceView(ticketPrice: TicketPrice) {
        findViewById<TextView>(R.id.total_price_tv).text =
            getString(R.string.movie_pay_price, ticketPrice.amount)
    }

    private fun calculateTotalPrice(): DomainTicketPrice = pickedSeats.calculateTotalPrice(
        MovieDayDiscountPolicy(movieDate),
        MovieTimeDiscountPolicy(movieTime),
    )

    private fun initSeatTable(rowSize: Int, colSize: Int) {
        SeatRow.make(rowSize).forEach { seatRow ->
            findViewById<TableLayout>(R.id.seat_table).addView(makeSeatTableRow(seatRow, colSize))
        }
    }

    private fun makeSeatTableRow(row: SeatRow, colSize: Int): TableRow = TableRow(this).apply {
        SeatColumn.make(colSize).forEach { col ->
            addView(makeSeatView(row, col))
        }
    }

    private fun makeSeatView(row: SeatRow, col: SeatColumn): View {
        val seat = Seat(row, col).toDomain()
        return seat.toPresentation().makeView(this, isPicked(seat)) {
            when {
                isPicked(seat) -> unpick(this, seat)
                canPick() -> pick(this, seat)
                else -> showToast(getString(R.string.exceed_pickable_seat))
            }
        }
    }

    private fun unpick(seatView: View, seat: DomainSeat) {
        pickedSeats = pickedSeats.remove(seat)
        updateToggledSeatResultView(seatView, false)
    }

    private fun pick(seatView: View, seat: DomainSeat) {
        pickedSeats = pickedSeats.add(seat)
        updateToggledSeatResultView(seatView, true)
    }

    private fun updateToggledSeatResultView(seatView: View, isPicked: Boolean) {
        seatView.findViewById<TextView>(R.id.seat_number_tv).isSelected = isPicked
        updateTotalPriceView(calculateTotalPrice().toPresentation())
        updateDoneBtnEnabled(!canPick())
    }

    private fun isPicked(seat: DomainSeat) = pickedSeats.isPicked(seat)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(PICKED_SEATS_KEY, pickedSeats.toPresentation())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.done_btn -> showTicketingConfirmDialog()
        }
    }

    private fun showTicketingConfirmDialog() {
        createAlertDialog(false) {
            title(getString(R.string.ticketing_confirm_title))
            message(getString(R.string.ticketing_confirm_message))
            positiveButton(getString(R.string.ticketing_confirm_positive_btn)) {
                val reservation = makeReservation()
                registerPushBroadcast(reservation)
                startTicketingResultActivity(reservation)
            }
            negativeButton(getString(R.string.ticketing_confirm_negative_btn)) { it.dismiss() }
        }.show()
    }

    private fun makeReservation(): Reservation = movie.toDomain().reserve(
        reservedDate = movieDate,
        reservedTime = movieTime,
        ticket = ticket.toDomain(),
        seats = pickedSeats,
        ticketPrice = calculateTotalPrice(),
    ).toPresentation()

    private fun startTicketingResultActivity(reservation: Reservation) {
        startActivity(TicketingResultActivity.makeIntent(this, reservation))
        finish()
    }

    private fun registerPushBroadcast(reservation: Reservation) {
        val alarmIntent = Intent(this, ReservationPushReceiver::class.java)
        val alarmManager = PushAlarmManager(this)

        alarmManager.set(
            intent = alarmIntent,
            pushData = reservation,
            time = reservation.reservedTime.minusMinutes(REMINDER_TIME_MINUTES_AGO),
        )
    }

    companion object {
        internal const val PICKED_SEATS_KEY = "picked_seats"
        internal const val REMINDER_TIME_MINUTES_AGO = 30L
    }
}
