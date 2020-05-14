package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.ActionContext
import com.IceCreamQAQ.Yu.controller.router.DefaultRouter
import com.IceCreamQAQ.Yu.controller.router.RouterPlus
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.loader.LoadItem_
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class ContextRouter(level: Int) : DefaultRouter(level) {

    override fun invoke(path: String, context: ActionContext): Boolean {
        val router = this.routers[path] ?: return false
        return router.invoke("", context)
    }
}

class BotContextControllerLoader : BotControllerLoader() {

    @Inject
    private lateinit var context: YuContext

    override fun load(items: Map<String, LoadItem_>) {
        val router = ContextRouter(0)
        for (item in items.values) {
            val instance = context[item.type] ?: continue
            controllerToRouter_(instance, router)
        }
        context.putBean(RouterPlus::class.java, "context", router)
    }

}

data class ContextSession(val id: String, private val saves: MutableMap<String, Any> = ConcurrentHashMap()) {

    var context: String? = null

    operator fun get(name: String) = saves[name]
    operator fun set(name: String, value: Any) {
        saves[name] = value
    }

}