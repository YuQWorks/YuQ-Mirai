package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.DefaultStarter
import com.IceCreamQAQ.Yu.loader.AppClassloader
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

class YuQMiraiStart {


    companion object {
        private val log = LoggerFactory.getLogger(YuQMiraiStart::class.java)

        @JvmStatic
        fun start() {
            val startTime = System.currentTimeMillis()

            val classloader = AppClassloader(YuQMiraiStart::class.java.classLoader)
            val blackList = ArrayList<String>(1)
//            blackList.add("net.mamoe.mirai.")
            AppClassloader.registerBackList(blackList)
//            blackList.add("okhttp3.")
//            classloader.registerBackList(blackList)

            val yuClass = classloader.loadClass("com.IceCreamQAQ.Yu.DefaultApp")
            val start: Method? = yuClass.getMethod("start")

            val yu = yuClass.newInstance()
            start!!.invoke(yu)

            val overTime = System.currentTimeMillis()

            log.info("Done! ${(overTime - startTime).toDouble() / 1000}s.")

            println(" __  __     ____ \n" +
                    " \\ \\/ /_ __/ __ \\\n" +
                    "  \\  / // / /_/ /\n" +
                    "  /_/\\_,_/\\___\\_\\\n")
            println("感谢您使用 YuQ 进行开发，在您使用中如果遇到任何问题，可以到 Github，Gitee 提出 issue，您也可以添加 YuQ 的开发交流群（696129128）进行交流。")
        }

        @JvmStatic
        fun start(args: Array<String>) {


            DefaultStarter.init(args)
            start()


        }

    }

}