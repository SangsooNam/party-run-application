package online.partyrun.partyrunapplication.di.network

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import online.partyrun.partyrunapplication.data.model.SignInTokenResponse
import online.partyrun.partyrunapplication.network.service.SignInApiService
import online.partyrun.partyrunapplication.presentation.auth.AuthActivity
import online.partyrun.partyrunapplication.presentation.auth.signin.GoogleAuthUiClient
import online.partyrun.partyrunapplication.utils.Constants.SERVER_BASE_URL
import online.partyrun.partyrunapplication.utils.extension.setIntentActivity
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authenticator
 * requestCode가 401 코드가 포함된 응답을 반환받았을 때 authenticate() 호출
 */
@Singleton
class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val context: Context
): Authenticator {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = context,
            oneTapClient = Identity.getSignInClient(context)
        )
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        Timber.tag("Authenticate()").e("newAccessToken")
        val refreshToken = runBlocking(Dispatchers.IO) {
            tokenManager.getRefreshToken().first()
        }

        return runBlocking(Dispatchers.IO) {
            val newAccessToken = getNewAccessToken(refreshToken)
            /* Refresh Token 만료 시 */
            if (!newAccessToken.isSuccessful || newAccessToken.body() == null) {
                googleAuthUiClient.signOutGoogleAuth() // Google 로그아웃
                tokenManager.deleteAccessToken()
                context.setIntentActivity(
                    AuthActivity::class.java,
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                ) {
                    putString("fromMain", "sign_in")
                }
            }
            /* 정상적으로 Access Token을 받아온 경우 */
            newAccessToken.body()?.let {
                tokenManager.saveAccessToken(it.accessToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${it.accessToken}")
                    .build()
            }
        }
    }

    /* 리턴타입에 BaseResponse를 붙이지 않는 경우 */
    private suspend fun getNewAccessToken(refreshToken: String?): retrofit2.Response<SignInTokenResponse> {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            this.setLevel(HttpLoggingInterceptor.Level.BODY)
        }
        val okHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(SERVER_BASE_URL) /* SERVER BASE URL */
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        val service = retrofit.create(SignInApiService::class.java)
        return service.replaceToken("Bearer $refreshToken")
    }
}
