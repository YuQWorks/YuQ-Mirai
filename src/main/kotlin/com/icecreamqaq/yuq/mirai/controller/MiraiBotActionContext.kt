package com.icecreamqaq.yuq.mirai.controller

import com.IceCreamQAQ.Yu.entity.Result
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.mirai.message.MiraiMessage
import com.icecreamqaq.yuq.mirai.message.TextImpl

class MiraiBotActionContext : BotActionContext() {

    override fun buildResult(obj: Any): Message? {
        return when (obj) {
            is String -> {
                val message = MiraiMessage()
                val mb = message.body
                mb.add(TextImpl(obj))
                message
            }
            is MessageItem -> {
                val message = MiraiMessage()
                val mb = message.body
                mb.add(obj)
                message
            }
            is Message -> obj
            else -> buildResult(obj.toString())
        }
    }

}