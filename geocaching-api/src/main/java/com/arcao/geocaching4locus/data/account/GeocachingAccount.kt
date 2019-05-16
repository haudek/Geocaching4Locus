package com.arcao.geocaching4locus.data.account

import com.arcao.geocaching4locus.data.api.model.User
import com.arcao.geocaching4locus.data.api.model.enum.MembershipType
import org.threeten.bp.Instant

data class GeocachingAccount(
        private val accountManager: AccountManager,
        var accessToken: String,
        var accessTokenExpiration: Instant = Instant.MIN,
        var refreshToken: String,
        var userName: String? = null,
        var membership: MembershipType = MembershipType.UNKNOWN,
        var avatarUrl: String? = null,
        var bannerUrl: String? = null
) {

    val accessTokenExpired
        get() = Instant.now().isAfter(accessTokenExpiration)

    suspend fun refreshToken() {
        accountManager.refreshAccount(this)
    }


    fun updateUserInfo(user : User) {
        userName = user.username
        membership = user.membership
        avatarUrl = user.avatarUrl
        bannerUrl = user.bannerUrl

        accountManager.saveAccount(this)
    }
}