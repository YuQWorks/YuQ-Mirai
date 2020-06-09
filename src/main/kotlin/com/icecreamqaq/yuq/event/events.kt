package com.icecreamqaq.yuq.event

import com.IceCreamQAQ.Yu.event.events.CancelEvent
import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.Message

open class MessageEvent(val message: Message) : Event(), CancelEvent {
    override fun cancelAble() = true
}

open class GroupMessageEvent(message: Message) : MessageEvent(message)
open class PrivateMessageEvent(message: Message) : MessageEvent(message)

open class MessageRecallEvent(val sender: Long, val operator: Long, val messageId: Int) : Event()
open class PrivateRecallEvent(sender: Long, operator: Long, messageId: Int) : MessageRecallEvent(sender, operator, messageId)
open class GroupRecallEvent(val group: Long, sender: Long, operator: Long, messageId: Int) : MessageRecallEvent(sender, operator, messageId)

open class FriendListEvent : Event()
open class NewRequestEvent(val message: String) : FriendListEvent(), CancelEvent {
    override fun cancelAble() = true
    var accept = false
}

open class FriendAddEvent(val friend:Friend):FriendListEvent()
open class FriendDeleteEvent(val friend:Friend):FriendListEvent()

open class BotJoinGroupEvent(val group: Group):FriendListEvent()
open class BotLevelGroupEvent(val group: Group):FriendListEvent()

open class NewFriendRequestEvent(val qq: Long, message: String) : NewRequestEvent(message)
open class GroupInviteEvent(val group: Long, val qq: Long, message: String) : NewRequestEvent(message)

open class GroupMemberEvent(val group: Group) : Event()
open class GroupMemberRequestEvent(group: Group, val qq: Long, val name: String, val message: String) : GroupMemberEvent(group), CancelEvent {
    override fun cancelAble() = true
    var accept = false
}

open class GroupMemberJoinEvent(group: Group, val member: Member) : GroupMemberEvent(group)
open class GroupMemberInviteEvent(group: Group, member: Member, val inviter: Member) : GroupMemberJoinEvent(group, member)

open class GroupMemberLeaveEvent(group: Group, val member: Member) : GroupMemberEvent(group)
open class GroupMemberKickEvent(group: Group, val member: Member, val operator: Member) : GroupMemberEvent(group)