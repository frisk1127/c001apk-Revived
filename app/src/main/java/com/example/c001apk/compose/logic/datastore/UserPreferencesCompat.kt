package com.example.c001apk.compose.logic.datastore

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.example.c001apk.compose.FollowType
import com.example.c001apk.compose.ThemeMode
import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.UserPreferences
import com.example.c001apk.compose.UiStyle
import com.example.c001apk.compose.constant.Constants.API_VERSION
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.constant.Constants.VERSION_CODE
import com.example.c001apk.compose.constant.Constants.VERSION_NAME
import com.example.c001apk.compose.logic.model.HapticStrength

data class UserPreferencesCompat(
    val themeMode: ThemeMode,
    val materialYou: Boolean,
    val pureBlack: Boolean,
    val fontScale: Float,
    val contentScale: Float,
    val szlmId: String,
    val imageQuality: Int,
    val imageFilter: Boolean,
    val openInBrowser: Boolean,
    val showSquare: Boolean,
    val recordHistory: Boolean,
    val showEmoji: Boolean,
    val checkUpdate: Boolean,
    val checkCount: Boolean,
    val versionName: String,
    val apiVersion: String,
    val versionCode: String,
    val manufacturer: String,
    val brand: String,
    val model: String,
    val buildNumber: String,
    val sdkInt: String,
    val androidVersion: String,
    val userAgent: String,
    val xAppDevice: String,
    val xAppToken: String,
    val isLogin: Boolean,
    val userAvatar: String,
    val username: String,
    val level: String,
    val experience: String,
    val nextLevelExperience: String,
    val uid: String,
    val token: String,
    val followType: FollowType,
    val recentIds: String,
    val checkCountPeriod: Int,
    val installTime: String,
    val themeType: ThemeType,
    val seedColor: String,
    val paletteStyle: Int,
    val hapticFeedback: Boolean,
    val hapticStrength: HapticStrength,
    val customHaptics: Boolean,
    val reverseHapticEffects: Boolean,
    val uiStyle: UiStyle = UiStyle.MD3,
    val blurEnabled: Boolean = true,
    val blurScope: com.example.c001apk.compose.BlurScope = com.example.c001apk.compose.BlurScope.GLOBAL,
    val accounts: List<com.example.c001apk.compose.SavedAccount> = emptyList(),
    val sessionGeneration: Long = 0,
    val szlmLastAttempt: Long = 0,
    val pendingDeviceIdLogin: Long = 0,
    val autoCheckClientUpdate: Boolean = true,
    val lastClientUpdateCheck: Long = 0,
) {
    constructor(original: UserPreferences) : this(
        themeMode = original.themeMode,
        materialYou = original.materialYou,
        pureBlack = original.pureBlack,
        fontScale = original.fontScale,
        contentScale = original.contentScale,
        szlmId = original.szlmId,
        imageQuality = original.imageQuality,
        imageFilter = original.imageFilter,
        openInBrowser = original.openInBrowser,
        showSquare = original.showSquare,
        recordHistory = original.recordHistory,
        showEmoji = original.showEmoji,
        checkUpdate = original.checkUpdate,
        checkCount = original.checkCount,
        versionName = original.versionName,
        apiVersion = original.apiVersion,
        versionCode = original.versionCode,
        manufacturer = original.manufacturer,
        brand = original.brand,
        model = original.model,
        buildNumber = original.buildNumber,
        sdkInt = original.sdkInt,
        androidVersion = original.androidVersion,
        userAgent = original.userAgent,
        xAppDevice = original.xAppDevice,
        xAppToken = original.xAppToken,
        isLogin = original.isLogin,
        userAvatar = original.userAvatar,
        username = original.username,
        level = original.level,
        experience = original.experience,
        nextLevelExperience = original.nextLevelExperience,
        uid = original.uid,
        token = original.token,
        followType = original.followType,
        recentIds = original.recentIds,
        checkCountPeriod = original.checkCountPeriod,
        installTime = original.installTime,
        themeType = original.themeType,
        seedColor = original.seedColor,
        paletteStyle = original.paletteStyle,
        hapticFeedback = !original.disableHapticFeedback,
        hapticStrength = HapticStrength.fromProtoValue(original.hapticStrength),
        customHaptics = original.customHaptics,
        reverseHapticEffects = original.reverseHapticEffects,
        uiStyle = original.uiStyle.takeUnless { it == UiStyle.UNRECOGNIZED } ?: UiStyle.MD3,
        blurEnabled = if (original.hasBlurEnabled()) original.blurEnabled else true,
        blurScope = original.blurScope.takeUnless { it == com.example.c001apk.compose.BlurScope.UNRECOGNIZED } ?: com.example.c001apk.compose.BlurScope.GLOBAL,
        accounts = original.accountsList,
        sessionGeneration = original.sessionGeneration,
        szlmLastAttempt = original.szlmLastAttempt,
        pendingDeviceIdLogin = original.pendingDeviceIdLogin,
        autoCheckClientUpdate = if (original.hasAutoCheckClientUpdate()) original.autoCheckClientUpdate else true,
        lastClientUpdateCheck = original.lastClientUpdateCheck,
    )

    @Composable
    fun isDarkMode() = when (themeMode) {
        ThemeMode.ALWAYS_OFF -> false
        ThemeMode.ALWAYS_ON -> true
        else -> isSystemInDarkTheme()
    }

    fun toProto(): UserPreferences = UserPreferences.newBuilder()
        .setThemeMode(themeMode)
        .setMaterialYou(materialYou)
        .setPureBlack(pureBlack)
        .setFontScale(fontScale)
        .setContentScale(contentScale)
        .setSzlmId(szlmId)
        .setImageQuality(imageQuality)
        .setImageFilter(imageFilter)
        .setOpenInBrowser(openInBrowser)
        .setShowSquare(showSquare)
        .setRecordHistory(recordHistory)
        .setShowEmoji(showEmoji)
        .setCheckUpdate(checkUpdate)
        .setCheckCount(checkCount)
        .setVersionName(versionName)
        .setApiVersion(apiVersion)
        .setVersionCode(versionCode)
        .setManufacturer(manufacturer)
        .setBrand(brand)
        .setModel(model)
        .setBuildNumber(buildNumber)
        .setSdkInt(sdkInt)
        .setAndroidVersion(androidVersion)
        .setUserAgent(userAgent)
        .setXAppDevice(xAppDevice)
        .setXAppToken(xAppToken)
        .setIsLogin(isLogin)
        .setUserAvatar(userAvatar)
        .setUsername(username)
        .setLevel(level)
        .setExperience(experience)
        .setNextLevelExperience(nextLevelExperience)
        .setUid(uid)
        .setToken(token)
        .setFollowType(followType)
        .setRecentIds(recentIds)
        .setCheckCountPeriod(checkCountPeriod)
        .setInstallTime(installTime)
        .setThemeType(themeType)
        .setSeedColor(seedColor)
        .setPaletteStyle(paletteStyle)
        .setDisableHapticFeedback(!hapticFeedback)
        .setHapticStrength(hapticStrength.protoValue)
        .setCustomHaptics(customHaptics)
        .setReverseHapticEffects(reverseHapticEffects)
        .setUiStyle(uiStyle)
        .setBlurEnabled(blurEnabled)
        .setBlurScope(blurScope)
        .addAllAccounts(accounts)
        .setSessionGeneration(sessionGeneration)
        .setSzlmLastAttempt(szlmLastAttempt)
        .setPendingDeviceIdLogin(pendingDeviceIdLogin)
        .setAutoCheckClientUpdate(autoCheckClientUpdate)
        .setLastClientUpdateCheck(lastClientUpdateCheck)
        .build()

    companion object {
        fun default() = UserPreferencesCompat(
            themeMode = ThemeMode.FOLLOW_SYSTEM,
            materialYou = true,
            pureBlack = false,
            fontScale = 1.00f,
            contentScale = 1.00f,
            szlmId = EMPTY_STRING,
            imageQuality = 0,
            imageFilter = true,
            openInBrowser = false,
            showSquare = true,
            recordHistory = true,
            showEmoji = true,
            checkUpdate = true,
            checkCount = true,
            versionName = VERSION_NAME,
            apiVersion = API_VERSION,
            versionCode = VERSION_CODE,
            manufacturer = EMPTY_STRING,
            brand = EMPTY_STRING,
            model = EMPTY_STRING,
            buildNumber = EMPTY_STRING,
            sdkInt = EMPTY_STRING,
            androidVersion = EMPTY_STRING,
            userAgent = EMPTY_STRING,
            xAppDevice = EMPTY_STRING,
            xAppToken = EMPTY_STRING,
            isLogin = false,
            userAvatar = EMPTY_STRING,
            username = EMPTY_STRING,
            level = EMPTY_STRING,
            experience = EMPTY_STRING,
            nextLevelExperience = EMPTY_STRING,
            uid = EMPTY_STRING,
            token = EMPTY_STRING,
            followType = FollowType.ALL,
            recentIds = EMPTY_STRING,
            checkCountPeriod = 5,
            installTime = EMPTY_STRING,
            themeType = ThemeType.Default,
            seedColor = EMPTY_STRING,
            paletteStyle = 0,
            hapticFeedback = true,
            hapticStrength = HapticStrength.Medium,
            customHaptics = false,
            reverseHapticEffects = false,
        )
    }
}
