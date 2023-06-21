package online.partyrun.partyrunapplication.di

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import online.partyrun.partyrunapplication.core.common.network.TokenExpirationNotifier

@Module
@InstallIn(SingletonComponent::class)
object TokenExpirationModule {

    @Provides
    fun provideTokenExpirationNotifier(
        @ApplicationContext context: Context
    ): TokenExpirationNotifier =
        DefaultTokenExpirationNotifier(context)
}
