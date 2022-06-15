package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.annotation.Event
import com.IceCreamQAQ.Yu.annotation.EventListener
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.event.EventBus
import com.IceCreamQAQ.Yu.event.events.AppStartEvent
import com.IceCreamQAQ.Yu.event.events.AppStopEvent
import com.alibaba.fastjson.JSONObject
import com.icecreamqaq.yuq.*
import com.icecreamqaq.yuq.event.YuQApplicationClosingEvent
import com.icecreamqaq.yuq.event.YuQApplicationStartedEvent
import com.icecreamqaq.yuq.message.MessageItemFactory
import javax.inject.Inject

@EventListener
class MiraiYuQImpl : YuQ, YuQVersion {
    override val bots: ArrayList<MiraiBot> = ArrayList()

    data class Account(
        val qq: Long,
        val pwd: String
    ) {
        var protocol: String = "HD"
        var name: String? = null
    }

    @Config("YuQ.Mirai.Account")
    private lateinit var users: JSONObject

    @Inject
    private lateinit var eventBus: EventBus

    @Inject
    override lateinit var messageItemFactory: MessageItemFactory


    override fun createBot(id: String, pwd: String, botName: String?, extData: String?) =
        Account(id.toLong(), pwd).run {
            name = botName
            extData?.let { protocol = it }
            MiraiBot(this)
        }

//    @Inject
//    private lateinit var internalBot: YuQInternalBotImpl

    @Inject
    private lateinit var context: YuContext

    @Event
    fun onStart(e: AppStartEvent) {

        com.icecreamqaq.yuq.eventBus = eventBus
        yuq = this
        mif = messageItemFactory

        internalBot = context[YuQInternalBotImpl::class.java]!!

        users.keys.forEach { key ->
            bots.add(MiraiBot(users.getObject(key, Account::class.java)))
        }

        YuQApplicationStartedEvent().post()
        bots.forEach { it.login() }
    }

    @Event
    fun onEnd(e: AppStopEvent) {
        YuQApplicationClosingEvent().post()
        bots.forEach { it.close() }
    }


    override fun runtimeName() = "YuQ-Mirai"
    override fun runtimeVersion() = "0.1.0.0-DEV25+MultiBot-DEV2"
}