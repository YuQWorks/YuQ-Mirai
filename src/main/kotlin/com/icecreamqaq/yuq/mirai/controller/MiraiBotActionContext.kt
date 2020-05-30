package com.icecreamqaq.yuq.mirai.controller

import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.mirai.message.MiraiMessage
import com.icecreamqaq.yuq.mirai.message.TextImpl

@Deprecated(
        message = "因为可以使用 Message 直接构建新的 Message 对象，所以这个可以废弃了"
)
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