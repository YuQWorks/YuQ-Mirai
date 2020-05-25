package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.DefaultStarter
import com.IceCreamQAQ.Yu.loader.AppClassloader
import java.lang.reflect.Method

class YuQMiraiStart {

    companion object{

        @JvmStatic
        fun start(){
            val classloader = AppClassloader(YuQMiraiStart::class.java.classLoader)
            val blackList = ArrayList<String>(1)
            blackList.add("net.mamoe.mirai.")
//            blackList.add("okhttp3.")
            classloader.registerBackList(blackList)

            val yuClass = classloader.loadClass("com.IceCreamQAQ.Yu.DefaultApp")
            val start: Method? = yuClass.getMethod("start")

            val yu = yuClass.newInstance()
            start!!.invoke(yu)
        }

        @JvmStatic
        fun start(args:Array<String>){
            DefaultStarter.init(args)
            start()
        }

    }

}