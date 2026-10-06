package com.example.data.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "SalimBilling"
        const val PRODUCT_ID_PRO = "salim_pro_upgrade"
        private const val PREFS_NAME = "salim_billing_prefs"
        private const val KEY_IS_PRO = "is_pro_cached"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _billingMessage = MutableStateFlow<String?>(null)
    val billingMessage: StateFlow<String?> = _billingMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    fun startConnection(onConnected: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            queryProductDetails()
            queryPurchases()
            onConnected?.invoke()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup successful")
                    queryProductDetails()
                    queryPurchases()
                    onConnected?.invoke()
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    fun queryProductDetails() {
        if (!billingClient.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PRO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = queryProductDetailsResult.firstOrNull { it.productId == PRODUCT_ID_PRO }
                _productDetails.value = details
                Log.d(TAG, "Found product details: ${details?.name} - ${details?.oneTimePurchaseOfferDetails?.formattedPrice}")
            } else {
                Log.w(TAG, "Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    fun queryPurchases(onComplete: ((Boolean) -> Unit)? = null) {
        if (!billingClient.isReady) {
            startConnection { queryPurchases(onComplete) }
            return
        }

        _isLoading.value = true
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            _isLoading.value = false
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPro = purchases.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_PRO) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                setProStatus(hasPro)

                // Acknowledge any unacknowledged purchases
                purchases.forEach { purchase ->
                    if (purchase.products.contains(PRODUCT_ID_PRO) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        !purchase.isAcknowledged
                    ) {
                        acknowledgePurchase(purchase)
                    }
                }

                onComplete?.invoke(hasPro)
            } else {
                Log.w(TAG, "Query purchases failed: ${billingResult.debugMessage}")
                onComplete?.invoke(false)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity): Boolean {
        val details = _productDetails.value
        if (details == null) {
            _billingMessage.value = "Product details not available yet. Please check connection."
            queryProductDetails()
            return false
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    for (purchase in purchases) {
                        if (purchase.products.contains(PRODUCT_ID_PRO) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        ) {
                            setProStatus(true)
                            if (!purchase.isAcknowledged) {
                                acknowledgePurchase(purchase)
                            }
                            _billingMessage.value = "Pro features unlocked!"
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingMessage.value = null
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                setProStatus(true)
                _billingMessage.value = "Purchase restored: You already own Salim Pro!"
            }
            else -> {
                _billingMessage.value = "Purchase error: ${billingResult.debugMessage}"
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
            } else {
                Log.w(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
            }
        }
    }

    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        if (!billingClient.isReady) {
            startConnection {
                restorePurchases(onResult)
            }
            return
        }

        _isLoading.value = true
        queryPurchases { hasPro ->
            _isLoading.value = false
            if (hasPro) {
                onResult(true, "Salim Pro successfully restored.")
            } else {
                onResult(false, "No previous purchases found for this account.")
            }
        }
    }

    fun clearMessage() {
        _billingMessage.value = null
    }

    private fun setProStatus(isPro: Boolean) {
        _isPro.value = isPro
        prefs.edit().putBoolean(KEY_IS_PRO, isPro).apply()
    }
}
