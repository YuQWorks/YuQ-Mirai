package com.icecreamqaq.yuq.mirai.entity

import com.IceCreamQAQ.Yu.toJSONObject
import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.error.SendMessageFailedByCancel
import com.icecreamqaq.yuq.event.SendMessageEvent
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.mirai.localEventBus
import com.icecreamqaq.yuq.mirai.message.AtImpl
import com.icecreamqaq.yuq.mirai.message.MiraiMessageSource
import com.icecreamqaq.yuq.mirai.message.toLocal
import com.icecreamqaq.yuq.postWithQQKey
import com.icecreamqaq.yuq.web
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import net.mamoe.mirai.contact.Contact as MiraiContact
import net.mamoe.mirai.contact.Friend as MiraiFriend
import net.mamoe.mirai.contact.Group as MiraiGroup
import net.mamoe.mirai.contact.Member as MiraiMember

abstract class ContactImpl(val miraiContact: MiraiContact) : Contact {

    private val log = LoggerFactory.getLogger(ContactImpl::class.java)

    override fun sendMessage(message: Message): MiraiMessageSource {
        val ms = message.toLogString()
        val ts = this.toLogString()
        log.debug("Send Message To: $ts, $ms")
        if (localEventBus.post(SendMessageEvent.Per(this, message))) throw SendMessageFailedByCancel()
        val m = MiraiMessageSource(
                runBlocking {
                    miraiContact.sendMessage(message.toLocal(this@ContactImpl))
                }.source
        )
        localEventBus.post(SendMessageEvent.Post(this, message, m))
        log.info("$ts <- $ms")
        return m
    }
}

class FriendImpl(private val friend: MiraiFriend) : ContactImpl(friend), Friend {

    override val id = friend.id
    override val avatar
        get() = friend.avatarUrl
    override val name
        get() = friend.nick

    override fun delete() {
        TODO("Not yet implemented")
    }

    override fun toString() = "Friend($name($id))"

}

class GroupImpl(private val group: MiraiGroup) : ContactImpl(group), Group {
    override val id = group.id
    override var maxCount: Int = 0

    override val avatar: String
        get() = group.avatarUrl

    override val name: String
        get() = group.name

    init {
        maxCount = web.postWithQQKey("https://qun.qq.com/cgi-bin/qun_mgr/search_group_members",
                mapOf(
                        "gc" to id.toString(),
                        "st" to 0.toString(),
                        "end" to 15.toString(),
                        "sort" to "0",
                        "bkn" to "{gtk}"
                ) as MutableMap<String, String>
        ).toJSONObject().getIntValue("max_count")
    }

    override fun leave() {
        runBlocking {
            group.quit()
        }
    }

    override fun get(qq: Long): GroupMemberImpl {
        return members[qq] ?: if (qq == bot.id) bot else error("Member $qq Not Found!")
    }

    override fun isFriend() = false

    override fun toString(): String {
        return "Group($name($id))"
    }

    override fun banAll() {
        group.settings.isMuteAll = true
    }

    override fun unBanAll() {
        group.settings.isMuteAll = false
    }

    override val members: MutableMap<Long, GroupMemberImpl>
    override val bot: GroupMemberImpl

    init {
        members = HashMap(group.members.size)
        for (member in group.members) {
            members[member.id] = GroupMemberImpl(member, this)
        }
        bot = GroupMemberImpl(group.botAsMember, this)
    }


}

open class GroupMemberImpl(private val member: MiraiMember, override val group: GroupImpl) : ContactImpl(member), Member {

    override val permission
        get() = member.permission.level
    override var nameCard
        get() = member.nameCard
        set(value) {
            member.nameCard = value
        }
    override val title
        get() = member.specialTitle

    override fun at() = AtImpl(id)

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

    override fun toString(): String {
        return "Member($nameCard($id)[${group.name}(${group.id}])"
    }

    override val id
        get() = member.id
    override val avatar
        get() = member.avatarUrl
    override val name
        get() = member.nick


}

class AnonymousMemberImpl(member: MiraiMember, group: GroupImpl) : GroupMemberImpl(member, group), AnonymousMember {

    override fun canSendMessage() = false
    override fun isFriend() = false

}
