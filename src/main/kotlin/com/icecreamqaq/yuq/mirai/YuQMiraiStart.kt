package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.DefaultStarter
import com.IceCreamQAQ.Yu.hook.*
import com.IceCreamQAQ.Yu.loader.AppClassloader
import com.IceCreamQAQ.Yu.toJSONObject
import com.icecreamqaq.yuq.YuQStarter
import javafx.application.Application.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.IMirai
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.full.companionObjectInstance

//@Deprecated(
//        message = "建议使用 YuQ 提供的启动类，YuQStarter",
//        level = DeprecationLevel.WARNING,
//        replaceWith = ReplaceWith("YuQStarter", "com.icecreamqaq.yuq.YuQStarter")
//)

lateinit var classloader: AppClassloader

class YuQMiraiStart {


    companion object {
        private val log = LoggerFactory.getLogger(YuQMiraiStart::class.java)

        @JvmStatic
        fun start() {
//            AppClassloader.registerBackList(arrayListOf("net.mamoe.", "javafx."))


            val startTime = System.currentTimeMillis()
            val classloader = AppClassloader(YuQStarter::class.java.classLoader)
            Thread.currentThread().contextClassLoader = classloader

            val yuClass = classloader.loadClass("com.IceCreamQAQ.Yu.DefaultApp")
            val start: Method? = yuClass.getMethod("start")

            val yu = yuClass.newInstance()
            start!!.invoke(yu)

            val overTime = System.currentTimeMillis()

            log.info("Done! ${(overTime - startTime).toDouble() / 1000}s.")

            println(
                " __  __     ____ \n" +
                        " \\ \\/ /_ __/ __ \\\n" +
                        "  \\  / // / /_/ /\n" +
                        "  /_/\\_,_/\\___\\_\\\n"
            )
            println("感谢您使用 YuQ 进行开发，在您使用中如果遇到任何问题，可以到 Github，Gitee 提出 issue，您也可以添加 YuQ 的开发交流群（787049553）进行交流。")
        }

        @JvmStatic
        fun start(args: Array<String>) {
            DefaultStarter.init(args)
            start()
        }

    }

}

class HookMiraiService : HookRunnable {
    override fun init(info: HookInfo) {

    }

    override fun preRun(method: HookMethod): Boolean {
//        method.result = Class.forName("net.mamoe.mirai.internal.MiraiImpl")
//            .run { kotlin.companionObjectInstance as? IMirai ?: newInstance() }
        return false
    }

    override fun postRun(method: HookMethod) {

    }

    override fun onError(method: HookMethod): Boolean {
        return false
    }
}

const val capHost = "https://tencap.icecreamapi.com"

class HookSliderCaptcha : HookRunnable {
    override fun init(info: HookInfo) {

    }

    override fun preRun(method: HookMethod): Boolean {
        val client = OkHttpClient()

        fun post(url: String, paras: Map<String, String>) =
            client.newCall(
                Request.Builder()
                    .url(url)
                    .post(
                        FormBody.Builder()
                            .apply { paras.forEach { add(it.key, it.value) } }
                            .build()
                    ).build()
            ).execute().body!!.string()

        val url = method.paras[2]!! as String
        val ticket = runBlocking {
            val ticket: String
            val pid = "${UUID.randomUUID()}.${UUID.randomUUID()}"

            post(
                "$capHost/createCap",
                mapOf("pid" to pid, "url" to url)
            ).toJSONObject()
                .getInteger("code")
                .let {
                    if (it != 0) error("滑块会话创建失败！")
                }

            println("[SliderCaptcha] 需要滑动验证码, 请将下面提供的 Pid 输入进滑块助手，并点击提交按钮，然后手动完成滑块验证。")
            println("[SliderCaptcha] @see https://www.yuque.com/icecreamqaq/api/use-ten-cap")
            println("[SliderCaptcha] Pid: $pid")

            delay(6000)

            while (true) {
                val result = post(
                    "$capHost/checkCap",
                    mapOf("pid" to pid)
                ).toJSONObject()

                val cpc = result.getInteger("code")
                if (cpc != 0) {
                    delay(2000)
                    continue
                }

                ticket = result.getString("result")
                break
            }
            ticket
        }
//        RainUI.webListener(url)
        method.result = ticket
        return true
    }

    override fun postRun(method: HookMethod) {

    }

    override fun onError(method: HookMethod) = false

}