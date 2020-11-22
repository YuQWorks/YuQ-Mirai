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
            YuQStarter.start()
        }

        @JvmStatic
        fun start(args: Array<String>) {
            YuQStarter.start(args)
        }

    }

}