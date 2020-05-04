package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.message.Message

class MiraiMessage :Message() {

    override fun plus(text: String): Message {
        body.add(TextImpl(text))
        return this
    }
}