package com.divafinance.app

import androidx.compose.ui.window.ComposeUIViewController
import com.divafinance.app.di.initKoin
import com.divafinance.app.llm.FoundationModelBridge
import com.divafinance.app.llm.IosPlatformBridges

/**
 * [foundationModel] is the Swift implementation of Apple's on-device language model, or null
 * on any iOS without Apple Intelligence. It is stashed before [initKoin] because
 * `platformModule()` is a no-argument `expect fun` and this is the only moment the Swift side
 * can hand anything over.
 */
fun MainViewController(foundationModel: FoundationModelBridge? = null) = ComposeUIViewController(
    configure = {
        IosPlatformBridges.foundationModel = foundationModel
        initKoin()
    }
) {
    App()
}
