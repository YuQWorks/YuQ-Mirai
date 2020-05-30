package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageFactory

class MiraiMessageFactory : MessageFactory {
    override fun newMessage(): Message {
        return MiraiMessage()
    }

    override fun newGroup(group: Long): Message {
        val message = newMessage()
        message.group = group
        return message
    }

    override fun newPrivate(qq: Long): Message {
        val message = MiraiMessage()
        message.qq = qq
        return message
    }

    override fun newTemp(qq: Long, group: Long): Message {
        val message = MiraiMessage()
        message.temp = true
        message.qq = qq
        message.group = group
        return message
    }
}