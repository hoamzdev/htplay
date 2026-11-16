package com.htdev.movie.htplay.base

import android.content.Intent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.htdev.movie.htplay.auth.SessionViewModel
import com.htdev.movie.htplay.auth.login.LoginActivity
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.common.SessionExpiredException
import com.htdev.movie.htplay.nointernet.NoInternetActivity
import com.htdev.movie.htplay.util.getLocalizedErrorMessage
import com.htdev.movie.htplay.widget.checkNetworkConnected
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class AuthenticatedActivity<VB : ViewBinding> : BaseActivity<VB>() {
    private val sessionViewModel: SessionViewModel by viewModels()
    private var sessionCheck: Job? = null

    override fun onResume() {
        super.onResume()
        if (!checkNetworkConnected(this)) {
            // A cached login can use local downloads without Firebase's network reload.
            if (!sessionViewModel.isLoggedInLocally()) {
                startActivity(NoInternetActivity.createStartupIntent(this).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            return
        }
        sessionCheck = lifecycleScope.launch {
            val result = sessionViewModel.validateSession()
            if (result is Result.Error) {
                if (result.throwable is SessionExpiredException) {
                    startActivity(Intent(this@AuthenticatedActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                } else {
                    // A failed network check does not prove the account was deleted.
                    showToastApp(getLocalizedErrorMessage(result.throwable))
                }
            }
        }
    }

    override fun onPause() {
        sessionCheck?.cancel()
        super.onPause()
    }
}
