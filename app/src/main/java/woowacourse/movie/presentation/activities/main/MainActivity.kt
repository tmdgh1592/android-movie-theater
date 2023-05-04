package woowacourse.movie.presentation.activities.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import woowacourse.movie.R
import woowacourse.movie.presentation.activities.main.contract.MainContract
import woowacourse.movie.presentation.activities.main.contract.presenter.MainPresenter
import woowacourse.movie.presentation.activities.main.fragments.history.HistoryFragment
import woowacourse.movie.presentation.activities.main.fragments.home.HomeFragment
import woowacourse.movie.presentation.activities.main.fragments.setting.SettingFragment
import woowacourse.movie.presentation.extensions.checkPermissions
import woowacourse.movie.presentation.extensions.getParcelableCompat
import woowacourse.movie.presentation.extensions.showFragmentByTag
import woowacourse.movie.presentation.extensions.showToast
import woowacourse.movie.presentation.model.Reservation
import woowacourse.movie.presentation.model.mainstate.MainState

class MainActivity : AppCompatActivity(), MainContract.View {
    override val presenter: MainContract.Presenter = MainPresenter()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) showToast(getString(R.string.permission_allowed))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter.attach(this)
    }

    override fun initView() {
        requestNotificationPermission()
        initBottomNavigationView()
        showHomeScreen()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelable(MAIN_SCREEN_STATE_KEY, presenter.getState())
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        super.onRestoreInstanceState(bundle)
        bundle.getParcelableCompat<MainState>(MAIN_SCREEN_STATE_KEY)
            ?.let { presenter.setState(it) }
    }

    private fun initBottomNavigationView() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.selectedItemId = R.id.home
        bottomNavigationView.setOnItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.history -> presenter.onShowHistoryScreen()
                R.id.home -> presenter.onShowHomeScreen()
                R.id.setting -> presenter.onShowSettingScreen()
            }
            return@setOnItemSelectedListener true
        }
    }

    override fun showHistoryScreen() {
        showScreen<HistoryFragment>(HistoryFragment.TAG)
    }

    override fun showHomeScreen() {
        showScreen<HomeFragment>(HomeFragment.TAG)
    }

    override fun showSettingScreen() {
        showScreen<SettingFragment>(SettingFragment.TAG)
    }

    private inline fun <reified T : Fragment> showScreen(tag: String) {
        showFragmentByTag<T>(R.id.fragment_container_view, tag)
    }

    private fun requestNotificationPermission() {
        if (checkPermissions(Manifest.permission.POST_NOTIFICATIONS)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (presenter.wasShownHistory()) {
            val historyFragment = supportFragmentManager
                .findFragmentByTag(HistoryFragment.TAG) as HistoryFragment
            historyFragment.addHistory(intent?.getParcelableCompat(RESERVATION_KEY))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detach()
    }

    companion object {
        private const val RESERVATION_KEY = "reservation_key"
        private const val MAIN_SCREEN_STATE_KEY = "main_screen_state_key"

        fun getIntent(context: Context, reservation: Reservation): Intent =
            Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(RESERVATION_KEY, reservation)
    }
}
