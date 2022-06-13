package com.icecreamqaq.yuq.mirai

import com.icecreamqaq.yuq.YuQInternalBotImpl
import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource

internal lateinit var internalBot: YuQInternalBotImpl

internal fun <T> Message.send(contact: Contact, obj: T, send: (T) -> MessageSource) = internalBot.sendMessage(this, contact, obj, send)

