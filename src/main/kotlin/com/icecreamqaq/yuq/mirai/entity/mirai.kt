package com.icecreamqaq.yuq.mirai.entity

import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.mirai.message.MiraiMessageSource
import com.icecreamqaq.yuq.mirai.message.toLocal
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import net.mamoe.mirai.contact.Contact as MiraiContact
import net.mamoe.mirai.contact.Friend as MiraiFriend
import net.mamoe.mirai.contact.Group as MiraiGroup
import net.mamoe.mirai.contact.Member as MiraiMember

abstract class ContactImpl(val miraiContact: MiraiContact) : Contact {

    private val log = LoggerFactory.getLogger(ContactImpl::class.java)

    override fun sendMessage(message: Message): MiraiMessageSource {
        val ms = message.toString()
        val ts = this.toString()
        log.debug("Send Message To: $ts, $ms")
        val m = MiraiMessageSource(
                runBlocking {
                    miraiContact.sendMessage(message.toLocal(this@ContactImpl))
                }.source
        )
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

    override val avatar: String
        get() = group.avatarUrl

    override val name: String
        get() = group.name

    override fun leave() {
        runBlocking {
            group.quit()
        }
    }

    override fun get(qq: Long): GroupMemberImpl {
        return members[qq] ?: error("Member $qq Not Found!")
    }

    override fun toString(): String {
        return "Group($name($id))"
    }

    override val members: MutableMap<Long, GroupMemberImpl>
    override val bot: Member

    init {
        members = HashMap(group.members.size)
        for (member in group.members) {
            members[member.id] = GroupMemberImpl(member, this)
        }
        bot = GroupMemberImpl(group.botAsMember, this)
    }


}

class GroupMemberImpl(private val member: MiraiMember, override val group: GroupImpl) : ContactImpl(member), Member {

    override val permission
        get() = member.permission.level
    override var nameCard
        get() = member.nameCard
        set(value) {
            member.nameCard = value
        }
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
