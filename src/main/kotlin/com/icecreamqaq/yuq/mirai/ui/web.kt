package com.icecreamqaq.yuqui

import javafx.concurrent.Worker
import javafx.event.EventHandler
import kotlinx.coroutines.CompletableDeferred
import tornadofx.*

class WebView : View("WebView") {

    val url: String by param()
    val def: CompletableDeferred<String> by param()


    override val root = vbox {
        webview {
//            this.engine.

            this.engine.userAgent =
                "Mozilla/5.0 (Linux; Android 10; M2002J9E Build/QKQ1.191222.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045230 Mobile Safari/537.36 V1_AND_SQ_8.4.5_1468_YYB_D QQ/8.4.5.4745 NetType/4G WebP/0.3.0 Pixel/1080 StatusBarHeight/70 SimpleUISwitch/0 QQTheme/1000 InMagicWin/0"
            this.engine.isJavaScriptEnabled = true
            this.engine.load(url)

//            this.engi

            this.engine.loadWorker.stateProperty().addListener { ov, os, ns ->
                println("ov: ${ov.value}")
                println("os: ${os.name}")
                println("ns: ${ns.name}")

                if (ns == Worker.State.SUCCEEDED) {
                    this.engine.executeScript(
                        "var mqq = {invoke: function(type, mode, ret){ alert(ret.ticket) } };"
                    )
                }
            }


//

//            this.engine.

            this.engine.onAlert = EventHandler { e ->
                    val ticket = e.data
                    def.complete(ticket)
                    close()
            }


//            this.engine.
        }
    }
}