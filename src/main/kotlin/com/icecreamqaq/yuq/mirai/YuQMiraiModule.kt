package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.loader.AppClassloader
import com.IceCreamQAQ.Yu.module.Module
import com.icecreamqaq.yuq.mirai.util.YuQInternalFunMiraiImpl
import com.icecreamqaq.yuq.util.YuQInternalFun
import javax.inject.Inject
import javax.inject.Named

class YuQMiraiModule : Module {

    @Inject
    private lateinit var context: YuContext

    override fun onLoad() {
        AppClassloader.registerBackList(arrayListOf("net.mamoe.", "javafx."))
        context.putBean(YuQInternalFun::class.java, "", YuQInternalFunMiraiImpl())
    }
}