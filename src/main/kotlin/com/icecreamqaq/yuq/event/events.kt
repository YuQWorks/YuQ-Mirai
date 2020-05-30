package com.icecreamqaq.yuq.event

import com.IceCreamQAQ.Yu.event.events.CancelEvent
import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.message.Message

open class MessageEvent(val message: Message) : Event(), CancelEvent {
    override fun cancelAble() = true
}

open class GroupMessageEvent(message: Message) : MessageEvent(message)
open class PrivateMessageEvent(message: Message) : MessageEvent(message)

open class MessageRecallEvent(val sender:Long,val operator:Long,val messageId:Int) : Event()
open class PrivateRecallEvent(sender: Long,operator: Long,messageId: Int) : MessageRecallEvent(sender, operator, messageId)
open class GroupRecallEvent(val group:Long,sender: Long,operator: Long,messageId: Int) : MessageRecallEvent(sender, operator, messageId)

open class FriendListEvent : Event()
open class NewRequestEvent(val message:String) : FriendListEvent(), CancelEvent {
    override fun cancelAble() = true
    var accept = false
}

open class NewFriendRequestEvent(val qq:Long,message:String) : NewRequestEvent(message)
open class GroupInviteEvent(val group: Long,val qq:Long,message:String) : NewRequestEvent(message)

