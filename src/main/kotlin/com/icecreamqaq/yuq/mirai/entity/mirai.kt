package com.icecreamqaq.yuq.mirai.entity

import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.entity.Member

abstract class MiraiContact(override val yuq: YuQ) : Contact

class MiraiFriend(override val id: Long, override val avatar: String, override var name: String, yuq: YuQ):MiraiContact(yuq),Friend
class MiraiGroup(override val id: Long, override val avatar: String, override var name: String, override val members: MutableMap<Long, MiraiGroupMember>, yuq: YuQ): MiraiContact(yuq), Group
class MiraiGroupMember(override val id: Long, override val avatar: String, override val name: String, override val group: Long, override var nameCard: String, override var title: String, yuq: YuQ):MiraiContact(yuq),Member
