package woowacourse.movie.presentation.views.main.fragments.history.contract.presenter

import woowacourse.movie.domain.model.repository.HistoryRepository
import woowacourse.movie.domain.model.reservation.DomainReservation
import woowacourse.movie.presentation.mapper.toDomain
import woowacourse.movie.presentation.mapper.toPresentation
import woowacourse.movie.presentation.model.Reservation
import woowacourse.movie.presentation.views.main.fragments.history.contract.HistoryContract

class HistoryPresenter(
    view: HistoryContract.View,
    private val historyRepository: HistoryRepository,
) : HistoryContract.Presenter(view) {
    private val loadedHistories = mutableSetOf<DomainReservation>()

    override fun addHistory(item: Reservation) {
        loadedHistories.add(item.toDomain())
        view.showMoreHistory(item)
    }

    override fun loadHistories(): List<Reservation> {
        val newHistories = historyRepository.getAll()

        loadedHistories.addAll(newHistories)
        return newHistories.map { it.toPresentation() }
    }
}
