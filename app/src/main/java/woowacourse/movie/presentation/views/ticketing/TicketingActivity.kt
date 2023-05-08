package woowacourse.movie.presentation.views.ticketing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import woowacourse.movie.R
import woowacourse.movie.databinding.ActivityTicketingBinding
import woowacourse.movie.presentation.extensions.getParcelableCompat
import woowacourse.movie.presentation.extensions.showBackButton
import woowacourse.movie.presentation.extensions.showToast
import woowacourse.movie.presentation.model.MovieDate
import woowacourse.movie.presentation.model.MovieTime
import woowacourse.movie.presentation.model.TicketingState
import woowacourse.movie.presentation.model.movieitem.ListItem
import woowacourse.movie.presentation.model.movieitem.Movie
import woowacourse.movie.presentation.model.theater.Theater
import woowacourse.movie.presentation.views.seatpicker.SeatPickerActivity
import woowacourse.movie.presentation.views.ticketing.contract.TicketingContract
import woowacourse.movie.presentation.views.ticketing.listener.OnSpinnerItemSelectedListener
import woowacourse.movie.presentation.views.ticketing.presenter.TicketingPresenter

class TicketingActivity : AppCompatActivity(), TicketingContract.View, View.OnClickListener {
    override val presenter: TicketingContract.Presenter by lazy {
        TicketingPresenter(
            view = this,
            state = TicketingState(
                movie = intent.getParcelableCompat(MOVIE_KEY)!!,
                theater = intent.getParcelableCompat(THEATER_KEY)!!
            )
        )
    }
    private lateinit var binding: ActivityTicketingBinding

    private val movieDateAdapter: ArrayAdapter<String> by lazy {
        ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
    }
    private val movieTimeAdapter: ArrayAdapter<String> by lazy {
        ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_ticketing)
        binding.presenter = presenter
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelable(TICKETING_STATE_KEY, presenter.getState())
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        super.onRestoreInstanceState(bundle)
        val ticketingState = bundle.getParcelableCompat<TicketingState>(TICKETING_STATE_KEY)
        ticketingState?.let { presenter.setState(it) }
    }

    override fun initView(movie: Movie, movieDates: List<MovieDate>) {
        showBackButton()
        showMovieIntroduce(movie)
        initSpinnerConfig()
        updateMovieDates(movieDates)
        initViewClickListener()
    }

    private fun showMovieIntroduce(movie: Movie) {
        with(movie) {
            binding.posterIv.setImageResource(thumbnail)
            binding.titleTv.text = title
            binding.dateTv.text = getString(
                R.string.movie_release_date,
                formattedStartDate,
                formattedEndDate,
            )
            binding.runningTimeTv.text =
                getString(R.string.movie_running_time, runningTime)
            binding.introduceTv.text = introduce
        }
    }

    private fun initSpinnerConfig() {
        initSpinnerAdapter()
        initSpinnerListener()
    }

    private fun initSpinnerAdapter() {
        binding.movieDateSpinner.adapter = movieDateAdapter.also { it.setNotifyOnChange(true) }
        binding.movieTimeSpinner.adapter = movieTimeAdapter.also { it.setNotifyOnChange(true) }
    }

    private fun initSpinnerListener() {
        initMovieTimeSpinnerListener()
        initMovieDateSpinnerListener()
    }

    private fun updateMovieDates(movieDates: List<MovieDate>) {
        val movieDateTexts =
            movieDates.map { getString(R.string.book_date, it.year, it.month, it.day) }
        movieDateAdapter.clear()
        movieDateAdapter.addAll(movieDateTexts)
    }

    private fun initViewClickListener() {
        binding.minusBtn.setOnClickListener(this@TicketingActivity)
        binding.plusBtn.setOnClickListener(this@TicketingActivity)
        binding.ticketingBtn.setOnClickListener(this@TicketingActivity)
    }

    private fun initMovieTimeSpinnerListener() {
        binding.movieTimeSpinner.onItemSelectedListener =
            OnSpinnerItemSelectedListener { presenter.onSelectMovieTime(it) }
    }

    private fun initMovieDateSpinnerListener() {
        binding.movieDateSpinner.onItemSelectedListener =
            OnSpinnerItemSelectedListener { presenter.onSelectMovieDate(it) }
    }

    override fun showTicketingState(ticketCount: Int, movieDatePos: Int, movieTimePos: Int) {
        updateCount(ticketCount)
        updateSpinnersState(movieDatePos = movieDatePos, movieTimePos = movieTimePos)
    }

    override fun updateCount(value: Int) {
        binding.ticketCountTv.text = value.toString()
    }

    override fun updateSpinnersState(movieDatePos: Int, movieTimePos: Int) {
        binding.movieDateSpinner.setSelection(movieDatePos)
        binding.movieTimeSpinner.setSelection(movieTimePos)
    }

    override fun updateRunningTimes(runningTimes: List<MovieTime>) {
        val runningTimeTexts: List<String> =
            runningTimes.map { getString(R.string.book_time, it.hour, it.min) }
        movieTimeAdapter.clear()
        movieTimeAdapter.addAll(runningTimeTexts)
    }

    override fun showSeatPickerScreen(ticketingState: TicketingState) {
        val seatPickerIntent = SeatPickerActivity.getIntent(this, ticketingState)
        startActivity(seatPickerIntent)
        finish()
    }

    override fun showUnSelectDateTimeAlertMessage() {
        showToast(getString(R.string.select_date_and_time))
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.minus_btn -> presenter.minusCount()
            R.id.plus_btn -> presenter.plusCount()
            R.id.ticketing_btn -> presenter.onClickTicketingButton()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val MOVIE_KEY = "movie_key"
        private const val THEATER_KEY = "theater_key"
        private const val TICKETING_STATE_KEY = "ticketing_state_key"

        fun getIntent(context: Context, movie: ListItem, theater: Theater): Intent =
            Intent(context, TicketingActivity::class.java)
                .putExtra(MOVIE_KEY, movie)
                .putExtra(THEATER_KEY, theater)
    }
}
