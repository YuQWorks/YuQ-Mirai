package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.event.EventBus
import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource

internal lateinit var localEventBus: EventBus
internal lateinit var miraiBot: MiraiBot

internal fun <T> Message.send(contact: Contact, obj: T, send: (T) -> MessageSource) = miraiBot.rainBot.sendMessage(this, contact, obj, send)

