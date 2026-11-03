package com.htdev.movie.htplay.home

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.AuthenticatedActivity
import com.htdev.movie.htplay.databinding.ActivityHomeRouterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeRouterActivity : AuthenticatedActivity<ActivityHomeRouterBinding>() {
    private lateinit var navController: NavController

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
    }

    private fun handleNavRouter() {
        navController.addOnDestinationChangedListener(destinationChangedListener)
    }

    override fun handleClickEvent() {
        bottomTabs.forEach { (destinationId, tab) ->
            tab.setOnClickListener {
                if (navController.currentDestination?.id != destinationId) {
                    navController.navigate(destinationId, null, navOptions {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    })
                }
            }
        }
    }

    private fun updateTabBottomNav(selectedDestinationId: Int) {
        bottomTabs.forEach { (destinationId, tab) ->
            tab.isSelected = destinationId == selectedDestinationId
        }
    }

    override fun backDevice() {
        if (!navController.popBackStack()) super.backDevice()
    }

    override fun onDestroy() {
        navController.removeOnDestinationChangedListener(destinationChangedListener)
        super.onDestroy()
    }
}
