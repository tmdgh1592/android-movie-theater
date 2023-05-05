package woowacourse.movie.presentation.views.main.fragments.home.recyclerview

import android.view.ViewGroup
import woowacourse.movie.R
import woowacourse.movie.presentation.base.BaseRecyclerView
import woowacourse.movie.presentation.model.movieitem.Ad
import woowacourse.movie.presentation.model.movieitem.ListItem
import woowacourse.movie.presentation.model.movieitem.Movie
import woowacourse.movie.presentation.views.main.fragments.home.type.MovieViewType

class MovieListAdapter(
    private val adInterval: Int = DEFAULT_AD_INTERVAL,
    private val adTypes: List<ListItem>,
    onItemClick: (ListItem) -> Unit = {},
) : BaseRecyclerView.Adapter<ListItem>(onItemClick) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BaseRecyclerView.BaseViewHolder = when (MovieViewType.get(viewType)) {
        MovieViewType.MOVIE -> {
            val movieView = parent.inflate(R.layout.item_movie)
            MovieViewHolder(movieView, onItemViewClick)
        }

        MovieViewType.AD -> {
            val adView = parent.inflate(R.layout.item_native_ad)
            NativeAdViewHolder(adView, onItemViewClick)
        }
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is Movie -> MovieViewType.MOVIE.type
        is Ad -> MovieViewType.AD.type
        else -> throw IllegalArgumentException(INVALID_ITEM_TYPE)
    }

    fun appendAll(newItems: List<ListItem>) {
        var newAdSize = 0
        newItems.forEach { newMovie ->
            newAdSize += appendAd()
            items.add(newMovie)
        }
        notifyItemRangeChanged(items.size, newItems.size + newAdSize)
    }

    private fun appendAd(): Int {
        if ((items.size + 1) % (adInterval + 1) == 0) {
            if (items.add(adTypes.random())) return APPENDED_SIZE
        }
        return NO_APPENDED_SIZE
    }

    companion object {
        private const val DEFAULT_AD_INTERVAL = 3
        private const val APPENDED_SIZE = 1
        private const val NO_APPENDED_SIZE = 0

        private const val INVALID_ITEM_TYPE = "Invalid type"
    }
}
