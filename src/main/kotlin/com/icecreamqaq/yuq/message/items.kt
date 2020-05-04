package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.annotation.PathVar
import com.IceCreamQAQ.Yu.entity.Result

interface MessageItem : MessagePlus {
    fun toLocal(source: Any, message: Message): Any
    fun toPath(): String
    fun convertByPathVar(type: PathVar.Type): Any?

    fun toMessage(): Message
}

interface MessagePlus {
    operator fun plus(item: MessageItem): Message
    operator fun plus(item: String): Message
    operator fun plus(item: Message): Message
}

interface Text : MessageItem {
    val text: String
}

interface At : MessageItem {
    val user: Long
}

interface Face : MessageItem {
    val faceId: Int
}

interface Image : MessageItem {
    val id: String
}

interface NoImplItem : MessageItem {
    val source: String
}