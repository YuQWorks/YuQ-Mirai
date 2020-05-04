package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageFactory
import com.icecreamqaq.yuq.message.MessageItemFactory

@AutoBind
interface YuQ {

    val messageFactory: MessageFactory
    val messageItemFactory: MessageItemFactory

    fun sendMessage(message: Message): Int

}