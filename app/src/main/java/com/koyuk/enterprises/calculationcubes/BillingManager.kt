package com.koyuk.enterprises.calculationcubes

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient
//import com.koyuk.enterprises.calculationcubes.BillingManager.GlobalVariable.hasPro

class BillingManager(context: Context): PurchasesUpdatedListener{
    private var billingClient: BillingClient = BillingClient.newBuilder(context).setListener(this).build()
    private val calcCubesSku = "com.koyuk.enterprises.calculationcubes.pro"
    var currentPrice = "1.99"
    lateinit var skuDetailsPro: SkuDetails

    object GlobalVariable {
        //var hasPro: Boolean = false
    }

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.
                    getSkuDetails()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun getSkuDetails() {
        val skuList = ArrayList<String>()
        skuList.add(calcCubesSku)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
            if (responseCode == BillingClient.BillingResponse.OK && skuDetailsList != null) {
                for (skuDetails in skuDetailsList) {
                    val sku = skuDetails.sku
                    val price = skuDetails.price
                    if (calcCubesSku == sku) {
                        skuDetailsPro = skuDetails
                        currentPrice = price
                    }
                }
            }
        }
        var purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP).purchasesList
        handlePurchases(purchases)
    }

    public fun upgrade(activity: Activity){
        getSkuDetails()
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetailsPro)
            .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(@BillingClient.BillingResponse responseCode: Int, purchases: List<Purchase>?) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            handlePurchases(purchases)
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }
    private fun handlePurchases(purchases: List<Purchase>){
        for (purchase in purchases) {
            var purchase = purchases[0]
            if(purchase.sku == calcCubesSku){
                //hasPro = true
            }
        }
    }
}