package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.DefaultControllerLoader
import com.IceCreamQAQ.Yu.controller.DefaultControllerLoaderImpl
import com.IceCreamQAQ.Yu.controller.router.DefaultActionInvoker
import com.IceCreamQAQ.Yu.controller.router.MethodInvoker
import com.IceCreamQAQ.Yu.loader.LoadItem_
import java.lang.reflect.Method

class BotControllerLoader : DefaultControllerLoaderImpl() {

    override fun createMethodInvoker_(obj: Any, method: Method): MethodInvoker {
        return BotReflectMethodInvoker(method, obj)
    }

    override fun createActionInvoker_(level: Int): DefaultActionInvoker {
        return BotActionInvoker(level)
    }


}