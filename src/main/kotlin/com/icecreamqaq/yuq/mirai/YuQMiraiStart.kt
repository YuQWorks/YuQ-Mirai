package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.DefaultStarter
import com.IceCreamQAQ.Yu.loader.AppClassloader
import com.icecreamqaq.yuq.YuQStarter
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

//@Deprecated(
//        message = "建议使用 YuQ 提供的启动类，YuQStarter",
//        level = DeprecationLevel.WARNING,
//        replaceWith = ReplaceWith("YuQStarter", "com.icecreamqaq.yuq.YuQStarter")
//)
class YuQMiraiStart {


    companion object {
//        private val log = LoggerFactory.getLogger(YuQMiraiStart::class.java)

        @JvmStatic
        fun start() {
//            AppClassloader.registerBackList(arrayListOf("net.mamoe.", "javafx."))
            YuQStarter.start()
        }

        @JvmStatic
        fun start(args: Array<String>) {
            try {
                Class.forName("javafx.application.Application")
            } catch (ex: ClassNotFoundException) {
                System.setProperty("YuQ.Mirai.LoginUI", "-1")
            } catch (ex: NoClassDefFoundError) {
                System.setProperty("YuQ.Mirai.LoginUI", "-1")
            }
            if ("linux" == System.getProperty("os.name")?.toLowerCase()) System.setProperty("YuQ.Mirai.LoginUI", "-1")
            for (arg in args) {
                if (arg == "-NoUI") System.setProperty("YuQ.Mirai.LoginUI", "-1")
                if (arg == "-WithUI") System.setProperty("YuQ.Mirai.LoginUI", "0")
            }
//            AppClassloader.registerBackList(arrayListOf("net.mamoe.", "javafx."))
            YuQStarter.start(args)
        }

    }

}