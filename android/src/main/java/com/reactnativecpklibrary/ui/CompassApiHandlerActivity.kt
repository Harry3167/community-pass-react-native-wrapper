package com.reactnativecpklibrary.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.mastercard.compass.model.consent.ConsentResponse
import com.reactnativecpklibrary.CompassKernelUIController
import com.reactnativecpklibrary.R
import com.reactnativecpklibrary.ui.util.CompassIntentResponse
import com.reactnativecpklibrary.ui.util.CompassResultContract
import com.reactnativecpklibrary.util.ErrorCode.UNKNOWN
import com.reactnativecpklibrary.util.Key.DATA
import com.reactnativecpklibrary.util.Key.ERROR_CODE
import com.reactnativecpklibrary.util.Key.ERROR_MESSAGE
import com.reactnativecpklibrary.util.Key.RELIANT_APP_GUID

abstract class CompassApiHandlerActivity<T : Any> : CompassKernelUIController.CompassKernelActivity() {

    companion object {
        protected const val TAG = "CompassApiIntentHandlerActivity"
    }

    protected val compassApiActivityResult = registerForActivityResult(CompassResultContract<T>()){
        when(it){
            is CompassIntentResponse.Success -> successFinishActivity(it.data)
            is CompassIntentResponse.Error -> errorFoundFinishActivity(it.code, it.message)
        }
    }

    protected fun getNonIntentCompassApiResults(value: T?) {
        when(value){
            is ConsentResponse -> successFinishActivity(value)
            is String -> successFinishActivity(value)
            else -> errorFoundFinishActivity(0, "Unknown error")
        }
    }

    lateinit var reliantAppGuid: String

    abstract suspend fun callCompassApi()

    private fun successFinishActivity(data: T){
        val intent = Intent().apply {
            when (data) {
              is ConsentResponse -> putExtra(DATA, data)
              is String -> putExtra(DATA, data)
              is Parcelable -> putExtra(DATA, data)
            }
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun errorFoundFinishActivity(errorCode: Int?, errorMessage: String?) {
        val intent = Intent().apply {
            putExtra(ERROR_CODE, errorCode ?: UNKNOWN)
            putExtra(ERROR_MESSAGE, errorMessage ?: "unknown error")
        }
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Transparent)

        reliantAppGuid = intent.getStringExtra(RELIANT_APP_GUID)!!
        connectKernelService()
    }

    private fun connectKernelService() {
        connectKernelService(reliantAppGuid) { isSuccess, errorCode, errorMessage ->
            when (isSuccess) {
                true -> {
                    Log.d(TAG, "Connected to Kernel successfully")
                    startCompassCoroutine()
                }
                false -> {
                    Log.e(
                        TAG,
                        "Could not connect to Kernel. Code: $errorCode. Message: $errorMessage"
                    )
                    errorFoundFinishActivity(errorCode, errorMessage)
                }
            }
        }
    }

    private fun startCompassCoroutine() = lifecycleScope.launchWhenCreated {
        callCompassApi()
    }

}
