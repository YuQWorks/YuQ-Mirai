package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource

class MiraiMessage :Message() {

    override fun plus(text: String): Message {
        body.add(TextImpl(text))
        return this
    }
}

class MiraiMessageSource(val source: net.mamoe.mirai.message.data.MessageSource) : MessageSource {
    override val id: Int
        get() = source.id
}