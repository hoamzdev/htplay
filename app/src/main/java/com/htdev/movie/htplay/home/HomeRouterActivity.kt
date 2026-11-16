package com.htdev.movie.htplay.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.htdev.movie.htplay.widget.checkNetworkConnected
import com.htdev.movie.htplay.widget.onClickListener

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.AuthenticatedActivity
import com.htdev.movie.htplay.databinding.ActivityHomeRouterBinding
import com.htdev.movie.htplay.util.SharePref
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeRouterActivity : AuthenticatedActivity<ActivityHomeRouterBinding>() {
    private lateinit var navController: NavController
    private var startInDownloads = false

    override fun onCreate(savedInstanceState: Bundle?) {
        startInDownloads = savedInstanceState?.getBoolean(STATE_START_IN_DOWNLOADS)
            ?: (intent.getBooleanExtra(EXTRA_OPEN_DOWNLOADS, false) || !checkNetworkConnected(this))
        super.onCreate(savedInstanceState)
    }

    private val bottomTabs by lazy {
        with(binding.bottomNav) {
            listOf(
                R.id.homeFragment to lnTabHome,
                R.id.favoriteFragment2 to lnTabFavorite,
                R.id.downloadFragment to lnTabDownload,
                R.id.settingsFragment to lnTabUser,
            )
        }
    }

    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            updateTabBottomNav(destination.id)
        }

    override fun createViewBinding(): ActivityHomeRouterBinding {
        return ActivityHomeRouterBinding.inflate(layoutInflater)
    }

    override fun initView() {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHost.navController
        handleNavRouter()
        if (navController.currentDestination == null) {
            val graph = navController.navInflater.inflate(R.navigation.home_nav_router)
            if (startInDownloads) graph.setStartDestination(R.id.downloadFragment)
            navController.graph = graph
        }
        if (checkNetworkConnected(this)) SharePref.init(this).countOpenApp()
    }

    private fun handleNavRouter() {
        navController.addOnDestinationChangedListener(destinationChangedListener)
    }

    override fun handleClickEvent() {
        bottomTabs.forEach { (destinationId, tab) ->
            tab.onClickListener(requiresNetwork = destinationId != R.id.downloadFragment) {
                navigateToTab(destinationId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_DOWNLOADS, false)) {
            navigateToTab(R.id.downloadFragment)
        }
    }

    private fun navigateToTab(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return
        navController.navigate(destinationId, null, navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        })
    }

    private fun updateTabBottomNav(selectedDestinationId: Int) {
        bottomTabs.forEach { (destinationId, tab) ->
            tab.isSelected = destinationId == selectedDestinationId
        }
    }

    override fun backDevice() {
        if (!navController.popBackStack()) super.backDevice()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_START_IN_DOWNLOADS, startInDownloads)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        navController.removeOnDestinationChangedListener(destinationChangedListener)
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_OPEN_DOWNLOADS = "open_downloads"
        private const val STATE_START_IN_DOWNLOADS = "home_start_in_downloads"

        fun createDownloadsIntent(context: Context): Intent =
            Intent(context, HomeRouterActivity::class.java).putExtra(EXTRA_OPEN_DOWNLOADS, true)
    }
}
