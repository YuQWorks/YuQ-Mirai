package com.icecreamqaq.yuq.entity

import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource

interface Contact {
    val id: Long
    val avatar: String
    val name: String

    val yuq: YuQ

    fun sendMessage(message: Message): MessageSource {
        return yuq.sendMessage(convertMessage(message))
    }

    fun convertMessage(message: Message): Message

}

interface Friend : Contact {

    override fun convertMessage(message: Message): Message {
        message.temp = false
        message.qq = id
        return message
    }

}

interface Group : Contact {

    val members: Map<Long, Member>

    operator fun get(qq: Long): Member {
        return members[qq] ?: error("Member $qq Not Found!")
    }

    override fun convertMessage(message: Message): Message {
        message.temp = false
        message.group = id
        return message
    }

}

interface Member : Contact {

    val group: Long

    val nameCard: String
    val title: String

    override fun convertMessage(message: Message): Message {
        message.temp = true
        message.group = group
        message.qq = id
        return message
    }

}