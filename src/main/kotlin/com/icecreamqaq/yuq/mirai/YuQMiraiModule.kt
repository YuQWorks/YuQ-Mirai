package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.hook.HookItem
import com.IceCreamQAQ.Yu.hook.YuHook
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
        YuHook.put(
            HookItem(
                "net.mamoe.mirai.Mirai",
                "findMiraiInstance",
                "com.icecreamqaq.yuq.mirai.HookMiraiService"
            )
        )
        YuHook.put(
            HookItem(
                "net.mamoe.mirai.utils.StandardCharImageLoginSolver",
                "onSolveSliderCaptcha",
                "com.icecreamqaq.yuq.mirai.HookSliderCaptcha"
            )
        )
        context.putBean(YuQInternalFun::class.java, "", YuQInternalFunMiraiImpl())
    }
}