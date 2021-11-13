package xyz.aprildown.timer.flavor.google

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.querySkuDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.tools.arch.Event
import xyz.aprildown.tools.arch.call
import xyz.aprildown.tools.helper.HandlerHelper
import xyz.aprildown.tools.helper.safeSharedPreference

/**
 * I decide not to share this class since it's not expensive according to the billing samples.
 */
internal class BillingSupervisor(
    context: Context,
    private val requireSkuDetails: Boolean = false,
    private val requestProState: Boolean = false,
    private val requestBackupSubState: Boolean = false,
    private val consumeInAppPurchases: Boolean = false,
) : PurchasesUpdatedListener, BillingClientStateListener, CoroutineScope by MainScope() {

    sealed class Error {
        object SubscriptionNotSupported : Error() {
            override fun toString(): String {
                return "SubscriptionNotSupported"
            }
        }

        data class Message(val code: Int, val content: String) : Error()
    }

    private val applicationContext = context.applicationContext
    private val billingClient: BillingClient = BillingClient.newBuilder(applicationContext)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    private val sharedPreferences = applicationContext.safeSharedPreference

    /** [requireSkuDetails] */
    private val _proSkuDetails: MutableLiveData<SkuDetails> = MutableLiveData()
    val proSkuDetails: LiveData<SkuDetails> = _proSkuDetails

    /** [requireSkuDetails] */
    private val _goProEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val goProEvent: LiveData<Event<Unit>> = _goProEvent

    /** request any state */
    private val _proState: MutableLiveData<Boolean> = MutableLiveData()
    val proState: LiveData<Boolean> = _proState.distinctUntilChanged()

    /** [requireSkuDetails] */
    private val _backupSubSkuDetails: MutableLiveData<SkuDetails> = MutableLiveData()
    val backupSubSkuDetails: LiveData<SkuDetails> = _backupSubSkuDetails

    /** [requireSkuDetails] */
    private val _backupSubEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val backupSubEvent: LiveData<Event<Unit>> = _backupSubEvent

    /** request any state */
    private val _backupSubState: MutableLiveData<Boolean> = MutableLiveData()
    val backupSubState: LiveData<Boolean> = _backupSubState.distinctUntilChanged()

    /** Any */
    private val _error: MutableLiveData<Event<Error>> = MutableLiveData()
    val error: LiveData<Event<Error>> = _error

    private var recentSubSku: String? = null

    fun supervise() {
        connect()
    }

    fun withLifecycleOwner(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    endConnection()
                }
            }
        )
    }

    // PurchasesUpdatedListener

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        HandlerHelper.runOnUiThread {
            when (val code = billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val set = purchases?.toSet() ?: emptySet()

                    if (findSubscription(set, OLD_SUB)) {
                        _proState.value = true
                        _backupSubState.value = true
                        _goProEvent.call()
                        _backupSubEvent.call()
                    } else {
                        val hasNewPro = findProState(set)
                        _proState.value = hasNewPro || (_proState.value ?: false)
                        if (hasNewPro) {
                            _goProEvent.call()
                        }

                        val hasNewSub = findSubscription(set, BACKUP_SUB)
                        _backupSubState.value = hasNewSub || (_backupSubState.value ?: false)
                        if (hasNewSub) {
                            _backupSubEvent.call()
                        }
                    }

                    updateIapIndicator()
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    launch { queryPurchases() }
                }
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                    connect()
                }
                else -> {
                    if (code != BillingClient.BillingResponseCode.USER_CANCELED) {
                        emitError(billingResult)
                    }
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, skuDetails: SkuDetails) {
        billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        )
    }

    private fun connect() {
        if (!billingClient.isReady) {
            billingClient.startConnection(this)
        }
    }

    // region BillingClientStateListener

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        HandlerHelper.runOnUiThread {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    launch {
                        if (requireSkuDetails) {
                            querySkuDetails()
                        }
                        queryPurchases()
                    }
                }
                else -> {
                    emitError(billingResult)
                }
            }
        }
    }

    /**
     * This method is called when the app has inadvertently disconnected from the [BillingClient].
     * An attempt should be made to reconnect using a retry policy. Note the distinction between
     * [endConnection][BillingClient.endConnection] and disconnected:
     * - disconnected means it's okay to try reconnecting.
     * - endConnection means the [billingClient] must be re-instantiated and then start
     *   a new connection because a [BillingClient] instance is invalid after endConnection has
     *   been called.
     **/
    override fun onBillingServiceDisconnected() {
        connect()
    }

    // endregion BillingClientStateListener

    private suspend fun querySkuDetails() {
        val inAppResult = billingClient.querySkuDetails(
            SkuDetailsParams.newBuilder()
                .setSkusList(listOf(PRO))
                .setType(BillingClient.SkuType.INAPP)
                .build()
        )
        when (inAppResult.billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val skuDetails = inAppResult.skuDetailsList?.find { it.sku == PRO }
                if (skuDetails != null) {
                    _proSkuDetails.value = skuDetails
                }
            }
            else -> {
                emitError(inAppResult.billingResult)
            }
        }

        val subResult = billingClient.querySkuDetails(
            SkuDetailsParams.newBuilder()
                .setSkusList(listOf(BACKUP_SUB))
                .setType(BillingClient.SkuType.SUBS)
                .build()
        )
        when (subResult.billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val skuDetails = subResult.skuDetailsList?.find { it.sku == BACKUP_SUB }
                if (skuDetails != null) {
                    _backupSubSkuDetails.value = skuDetails
                }
            }
            else -> {
                emitError(subResult.billingResult)
            }
        }
    }

    private suspend fun queryPurchases() {
        if (sharedPreferences.getBoolean(PREF_HAD_OLD_SUBSCRIPTION, false)) {
            _proState.value = true
            _backupSubState.value = true
            updateIapIndicator()
            return
        }

        if (findSubscription(
                billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS).purchasesList.toSet(),
                OLD_SUB
            )
        ) {
            _proState.value = true
            _backupSubState.value = true
            updateIapIndicator()
            return
        }

        val hasRequestedHistorySub = sharedPreferences.contains(PREF_HAD_OLD_SUBSCRIPTION)
        if (hasRequestedHistorySub) {
            // Doesn't have old subscription.
            queryNewestPurchases()
            updateIapIndicator()
        } else {
            val result = billingClient.queryPurchaseHistory(BillingClient.SkuType.SUBS)

            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasHistorySub = processHistorySubscription(
                    result.purchaseHistoryRecordList?.toSet() ?: emptySet()
                )
                sharedPreferences.edit {
                    putBoolean(PREF_HAD_OLD_SUBSCRIPTION, hasHistorySub)
                }
                if (hasHistorySub) {
                    _proState.value = true
                    _backupSubState.value = true
                } else {
                    queryNewestPurchases()
                }
            } else {
                queryNewestPurchases()
            }
            updateIapIndicator()
        }
    }

    private suspend fun queryNewestPurchases() {
        if (requestProState) {
            _proState.value = findProState(
                billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP)
                    .purchasesList.toSet()
            )
        }
        if (requestBackupSubState) {
            if (isSubscriptionSupported()) {
                _backupSubState.value = findSubscription(
                    billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS)
                        .purchasesList.toSet(),
                    BACKUP_SUB, OLD_SUB
                )
            } else {
                emitError(Error.SubscriptionNotSupported)
            }
        }
    }

    private fun findProState(purchases: Set<Purchase>): Boolean {
        var hasActivePurchase = false
        purchases.forEach { purchase ->
            if (PRO in purchase.skus || OLD_PRO in purchase.skus) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        billingClient.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                        ) {}
                    }
                    if (consumeInAppPurchases) {
                        billingClient.consumeAsync(
                            ConsumeParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                        ) { _, _ ->
                            Timber.tag(TAG).i("Consumed ${purchase.skus.joinToString()}")
                        }
                    }
                    hasActivePurchase = true
                }
            }
        }

        return hasActivePurchase
    }

    private fun findSubscription(purchases: Set<Purchase>, vararg skus: String): Boolean {
        var hasActivePurchase = false
        purchases.forEach { purchase ->
            val targetSku = purchase.skus.find { it in skus }
            if (targetSku != null) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        billingClient.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                        ) {}
                    }
                    hasActivePurchase = true
                    recentSubSku = targetSku
                }
            }
        }
        return hasActivePurchase
    }

    private fun processHistorySubscription(purchases: Set<PurchaseHistoryRecord>): Boolean {
        var hasPurchase = false
        purchases.forEach { purchase ->
            if (OLD_SUB in purchase.skus) {
                hasPurchase = true
                recentSubSku = OLD_SUB
            }
        }
        return hasPurchase
    }

    private fun isSubscriptionSupported(): Boolean {
        val billingResult =
            billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        var succeeded = false
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> connect()
            BillingClient.BillingResponseCode.OK -> succeeded = true
        }
        return succeeded
    }

    fun endConnection() {
        billingClient.endConnection()
        cancel()
    }

    fun getManageSubscriptionLink(): String {
        val sku = recentSubSku
        return if (sku != null) {
            getAppSubscriptionLink(applicationContext.packageName, sku)
        } else {
            SUBSCRIPTION_LINK
        }
    }

    private fun emitError(error: Error) {
        _error.value = Event(error)
        Timber.tag(TAG).i(error.toString())
    }

    private fun emitError(billingResult: BillingResult) {
        emitError(
            Error.Message(
                billingResult.responseCode,
                billingResult.debugMessage
            )
        )
    }

    private fun updateIapIndicator() {
        sharedPreferences.edit {
            if (requestProState) {
                putBoolean(Constants.PREF_HAS_PRO, _proState.value == true)
            }

            if (requestBackupSubState) {
                putBoolean(Constants.PREF_HAS_BACKUP_SUB, _backupSubState.value == true)
            }
        }
    }

    companion object {

        private const val OLD_PRO = "pro"
        private const val OLD_SUB = "subscription"

        private const val PRO = "pro2"

        private const val BACKUP_SUB = "backup"
        // private const val BACKUP_SUB = "subscription_test"

        private const val TAG = "BILLING"
        private const val PREF_HAD_OLD_SUBSCRIPTION = "pref_had_subscription"

        private const val SUBSCRIPTION_LINK = "https://play.google.com/store/account/subscriptions"
        private fun getAppSubscriptionLink(packageName: String, sku: String): String {
            return "https://play.google.com/store/account/subscriptions?sku=$sku&package=$packageName"
        }
    }
}
