package com.htdev.movie.htplay.language

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuthException
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.data.exception.AppException
import com.htdev.movie.htplay.util.getLocalizedErrorMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun loginMovieDetailsAndPasswordResetInflateInEverySupportedLanguage() {
        val expectations = listOf(
            listOf("en", "Log in to your account", "About", "Reset password"),
            listOf("vi", "Đăng nhập vào tài khoản", "Giới thiệu", "Đặt lại mật khẩu"),
            listOf("fr", "Connectez-vous à votre compte", "Présentation", "Réinitialiser le mot de passe"),
            listOf("de", "Melde dich bei deinem Konto an", "Informationen", "Passwort zurücksetzen"),
            listOf("it", "Accedi al tuo account", "Informazioni", "Reimposta la password"),
            listOf("zh", "登录您的账户", "简介", "重置密码"),
            listOf("ja", "アカウントにログイン", "作品情報", "パスワードを再設定"),
            listOf("ko", "계정에 로그인", "작품 정보", "비밀번호 재설정"),
        )
        instrumentation.runOnMainSync {
            expectations.forEach { (code, loginTitle, aboutLabel, resetLabel) ->
                val localized = ContextThemeWrapper(
                    LanguageHelper.localizedContext(context, code), R.style.Theme_Movie
                )
                val inflater = LayoutInflater.from(localized)
                val login = inflater.inflate(R.layout.activity_login, null, false)
                assertEquals(code, loginTitle, login.findViewById<TextView>(R.id.tvlogin).text.toString())

                val details = inflater.inflate(R.layout.item_detail_header, null, false)
                assertEquals(code, aboutLabel, details.findViewById<TextView>(R.id.tv_about).text.toString())
                val reset = inflater.inflate(R.layout.activity_forgot_pass_wactivity, null, false)
                assertEquals(code, resetLabel, reset.findViewById<TextView>(R.id.btn_reset).text.toString())

                assertTrue(localized.getString(R.string.detail_episode_number, 125).contains("125"))
                assertTrue(localized.getString(R.string.detail_more_from_country, "COUNTRY").contains("COUNTRY"))
            }
        }
    }

    @Test fun authenticationErrorsUseTheCurrentLanguageInsteadOfRawSdkMessages() {
        val vietnamese = LanguageHelper.localizedContext(context, "vi")
        assertEquals("Lỗi mạng. Vui lòng kiểm tra kết nối.", vietnamese.getLocalizedErrorMessage(AppException.NetworkException()))
        assertEquals(
            "Tài khoản này đã bị vô hiệu hóa.",
            vietnamese.getLocalizedErrorMessage(FirebaseAuthException("ERROR_USER_DISABLED", "SDK message"))
        )

        val errors = listOf(
            AppException.EmailInvalidException(),
            AppException.EmailAlreadyExistsException(),
            AppException.PasswordWrongException(),
            AppException.NetworkException(),
            FirebaseAuthException("ERROR_WEAK_PASSWORD", "SDK message"),
            IllegalStateException("SDK message"),
        )
        for (code in listOf("vi", "fr", "de", "it", "zh", "ja", "ko")) {
            val localized = LanguageHelper.localizedContext(context, code)
            errors.forEach { error ->
                val translated = localized.getLocalizedErrorMessage(error)
                val english = LanguageHelper.localizedContext(context, "en").getLocalizedErrorMessage(error)
                assertNotEquals(code, english, translated)
                assertFalse(translated.contains("SDK message"))
            }
        }
    }
}
