package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageFactory
import com.icecreamqaq.yuq.message.MessageItemFactory
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.mirai.entity.MiraiFriend
import com.icecreamqaq.yuq.mirai.entity.MiraiGroup
import com.icecreamqaq.yuq.mirai.entity.MiraiGroupMember

@AutoBind
interface YuQ {

    val messageFactory: MessageFactory
    val messageItemFactory: MessageItemFactory

    val friends:Map<Long,Friend>
    val groups:Map<Long,Group>

    fun refreshFriends(): Map<Long, Friend>
    fun refreshGroups(): Map<Long, Group>

    fun sendMessage(message: Message): MessageSource

    fun recallMessage(messageSource: MessageSource):Int

}