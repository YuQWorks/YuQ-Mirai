package com.icecreamqaq.yuq.mirai.entity

import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.entity.Member
import kotlinx.coroutines.runBlocking

abstract class MiraiContact(override val yuq: YuQ) : Contact

class MiraiFriend(private val friend :net.mamoe.mirai.contact.Friend, yuq: YuQ):MiraiContact(yuq),Friend {

    override val id = friend.id
    override val avatar
        get() = friend.avatarUrl
    override val name
        get() = friend.nick
}

class MiraiGroup(private val group:net.mamoe.mirai.contact.Group, yuq: YuQ): MiraiContact(yuq), Group {
    override val id= group.id

    override val avatar: String
        get() = group.avatarUrl

    override val name: String
        get() = group.name

    override val members: MutableMap<Long, MiraiGroupMember>
    override val bot: Member

    init {
        members = HashMap(group.members.size)
        for (member in group.members) {
            members[member.id] = MiraiGroupMember(member, this)
        }
        bot = MiraiGroupMember(group.botAsMember,this)
    }
}

class MiraiGroupMember(private val member: net.mamoe.mirai.contact.Member, override val group: Group):MiraiContact(group.yuq),Member {

    override val permission
        get() = member.permission.level
    override val nameCard
        get() = member.nameCard
    override val title
        get() = member.specialTitle

    override val ban: Int
        get() {
            return member.muteTimeRemaining
        }

    override fun ban(time: Int) {
        runBlocking {
            member.mute(time)
        }
    }

    override fun unBan() {
        runBlocking {
            member.unmute()
        }
    }

    override fun kick(message: String) {
        runBlocking {
            member.kick(message)
        }
    }

    override val id
        get() = member.id
    override val avatar
        get() = member.avatarUrl
    override val name
        get() = member.nick
}
