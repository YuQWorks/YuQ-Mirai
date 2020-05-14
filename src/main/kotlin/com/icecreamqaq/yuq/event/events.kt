package com.icecreamqaq.yuq.event

import com.IceCreamQAQ.Yu.event.events.CancelEvent
import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.message.Message

open class MessageEvent(val message: Message) : Event(), CancelEvent {
    override fun cancelAble() = true
}

open class GroupMessageEvent(message: Message) : MessageEvent(message)

open class PrivateMessageEvent(message: Message) : MessageEvent(message)

open class FriendListEvent : Event()
open class NewRequestEvent : FriendListEvent(), CancelEvent {
    override fun cancelAble() = true
    var accept = false
}

open class NewFriendRequestEvent : NewRequestEvent()
open class GroupInviteEvent : NewRequestEvent()

