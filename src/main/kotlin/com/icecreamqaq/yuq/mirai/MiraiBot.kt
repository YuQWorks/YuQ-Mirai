package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.annotation.Cron
import com.IceCreamQAQ.Yu.annotation.Default
import com.IceCreamQAQ.Yu.annotation.JobCenter
import com.IceCreamQAQ.Yu.cache.EhcacheHelp
import com.IceCreamQAQ.Yu.controller.router.NewRouter
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.event.EventBus
import com.IceCreamQAQ.Yu.toJSONString
import com.IceCreamQAQ.Yu.util.Web
import com.alibaba.fastjson.JSON
import com.icecreamqaq.yuq.*
import com.icecreamqaq.yuq.controller.ContextRouter
import com.icecreamqaq.yuq.controller.ContextSession
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.entity.User
import com.icecreamqaq.yuq.event.*
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageFactoryImpl
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.mirai.entity.FriendImpl
import com.icecreamqaq.yuq.mirai.entity.GroupImpl
import com.icecreamqaq.yuq.mirai.entity.GroupMemberImpl
import com.icecreamqaq.yuq.mirai.logger.Network
import com.icecreamqaq.yuq.mirai.message.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.events.GroupMemberEvent
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.TempMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.qqandroid.FPMM
import net.mamoe.mirai.utils.BotConfiguration
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set
import net.mamoe.mirai.event.events.BotJoinGroupEvent as MiraiBotJoinGroupEvent
import net.mamoe.mirai.event.events.FriendAddEvent as MiraiFriendAddEvent
import net.mamoe.mirai.event.events.FriendDeleteEvent as MiraiFriendDeleteEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent as MiraiNewFriendRequestEvent
import net.mamoe.mirai.message.GroupMessageEvent as MiraiGroupMessageEvent
import net.mamoe.mirai.message.data.MessageSource as MiraiSource

open class MiraiBot : YuQ, ApplicationService, User {

    private val log = LoggerFactory.getLogger(MiraiBot::class.java)

    @Config("YuQ.Mirai.user.qq")
    lateinit var qq: String

    @Config("YuQ.Mirai.user.pwd")
    lateinit var pwd: String

    @Config("YuQ.bot.name")
    private var botName: String? = null

    @Config("YuQ.Mirai.protocol")
    @Default("HD")
    lateinit var protocol: String

    @Inject
    @field:Named("group")
    lateinit var group: NewRouter

    @Inject
    @field:Named("priv")
    lateinit var priv: NewRouter

    @Inject
    lateinit var contextRouter: ContextRouter

    @Inject
    lateinit var webImpl: Web

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    override lateinit var messageFactory: MessageFactoryImpl

    @Inject
    override lateinit var messageItemFactory: MiraiMessageItemFactory

    @Inject
    lateinit var rainBot: RainBot

    @Inject
    @field:Named("ContextSession")
    lateinit var sessionCache: EhcacheHelp<ContextSession>

    @Inject
    lateinit var context: YuContext

    lateinit var bot: Bot
    override var botId: Long = 0
    override val botInfo: User = this
    override val cookieEx = Cookie("", 0, HashMap())

    data class Cookie(
            override var skey: String,
            override var gtk: Long = 0,
            override var pskeyMap: Map<String, YuQ.QQCookie.Pskey>
    ) : YuQ.QQCookie

    override lateinit var friends: HashMap<Long, FriendImpl>
    override lateinit var groups: HashMap<Long, GroupImpl>

    lateinit var sKey: String
    lateinit var superKey: String
    var gtk: Long = 0

    val pskeyMap = HashMap<String, YuQ.QQCookie.Pskey>()


//    var DefaultLogger: (identity: String?) -> MiraiLogger = { YuQMiraiLogger }

    override fun init() {
        FPMM.getTime = { System.currentTimeMillis() }
        FPMM.clear()

        mif = messageItemFactory
        mf = messageFactory
        yuq = this
        botId = qq.toLong()
        web = webImpl
        eventBus


        bot = Bot(botId, pwd) {
            fileBasedDeviceInfo()
            networkLoggerSupplier = { Network("Net ${it.id}") }
            botLoggerSupplier = { com.icecreamqaq.yuq.mirai.logger.Bot(("Bot ${it.id}")) }
            if (this@MiraiBot.protocol == "Android") protocol = BotConfiguration.MiraiProtocol.ANDROID_PHONE
            if (this@MiraiBot.protocol == "Watch") protocol = BotConfiguration.MiraiProtocol.ANDROID_WATCH
        }
        runBlocking {
            bot.alsoLogin()
        }
        context.putBean(Bot::class.java, "", bot)

        registerCookie()

        refreshFriends()
        refreshGroups()
    }

    open fun registerCookie() {
        val f = fun(sKey: String): Long {
            var hash = 5381L
            for (element in sKey) {
                hash += (hash shl 5 and 2147483647) + element.toInt() and 2147483647
                hash = hash and 2147483647
            }
            return hash and 2147483647
        }

        for (field in bot::class.java.superclass.declaredFields) {
            if (field.name == "client") {
                field.isAccessible = true
                val client = field[bot]
                for (cf in field.type.declaredFields) {
                    if (cf.name == "wLoginSigInfo") {
                        cf.isAccessible = true
                        val lsi = cf[client]
                        val lsiJS = lsi.toJSONString()
                        val lsiJO = JSON.parseObject(lsiJS)
                        val sKey = String(Base64.getDecoder().decode(lsiJO.getJSONObject("sKey").getString("data")))

                        this.sKey = sKey
                        this.cookieEx.skey = sKey
                        this.gtk = f(sKey)
                        this.cookieEx.gtk = this.gtk
                        this.superKey = String(Base64.getDecoder().decode(lsiJO.getString("superKey")))

                        val psKeys = lsiJO.getJSONObject("psKeyMap")

                        for (k in psKeys.keys) {
                            val value = String(Base64.getDecoder().decode(psKeys.getJSONObject(k).getString("data"))
                                    ?: continue)
                            val pskey = YuQ.QQCookie.Pskey(value, f(value))
                            pskeyMap[k] = pskey
                            webImpl.saveCookie(k, "/", "p_skey", value)
                            webImpl.saveCookie(k, "/", "p_uin", "o$qq")
                        }

                        this.cookieEx.pskeyMap = pskeyMap

                        webImpl.saveCookie("qq.com", "/", "uin", "o$qq")
                        webImpl.saveCookie("qq.com", "/", "skey", sKey)
                    }
                }
            }
        }
    }

    override fun refreshFriends(): Map<Long, Friend> {
        val friends = HashMap<Long, FriendImpl>(bot.friends.size)
        for (friend in bot.friends) {
            friends[friend.id] = FriendImpl(friend)
        }
        this.friends = friends
        return friends
    }

    override fun refreshGroups(): Map<Long, Group> {
        val groups = HashMap<Long, GroupImpl>(bot.groups.size)
        for (group in bot.groups) {
            groups[group.id] = GroupImpl(group)
        }
        this.groups = groups
        return groups
    }

    override fun start() {
        context.injectBean(rainBot)
        startBot()
    }

    override fun stop() {
        bot.close()
    }

    suspend fun MessageChain.toMessage(): Message? {
        val message = Message()

        val miraiSource = this[MiraiSource] ?: return null
        message.id = miraiSource.id
        val source = MiraiMessageSource(miraiSource)
        message.source = source
        message.sourceMessage = this

        val pathBody = ArrayList<MessageItem>()
        val messageBody = message.body

        var itemNum = 0
        loop@ for (m in this) {
            when (m) {
                is MiraiSource -> continue@loop
                is QuoteReply -> message.reply = MiraiMessageSource(m.source)
                is PlainText -> {
                    messageBody.add(TextImpl(m.content))
                    val sm = m.content.trim()
                    if (sm.isEmpty()) continue@loop
                    val sms = sm.replace("\n", " ").split(" ")
                    var loopStart = 0
                    if (itemNum == 0 && botName != null && sms[0] == botName) loopStart = 1
                    for (i in loopStart until sms.size) {
                        pathBody.add(TextImpl(sms[i]))
                        itemNum++
                    }
                }
                is At -> {
                    val item = AtImpl(m.target)
                    messageBody.add(item)
                    if (itemNum == 0 && m.target == botId) continue@loop
                    pathBody.add(item)
                    itemNum++
                }
                is AtAll -> {
                    val item = (AtImpl(-1L))
                    messageBody.add(item)
                    pathBody.add(item)
                    itemNum++
                }
                is Face -> {
                    val item = (FaceImpl(m.id))
                    messageBody.add(item)
                    pathBody.add(item)
                    itemNum++
                }
                is Image -> {
                    val item = (ImageReceive(m.imageId, m.queryUrl()))
                    messageBody.add(item)
                    pathBody.add(item)
                    itemNum++
                }
                is FlashImage -> {
                    val item = (FlashImageImpl(ImageReceive(m.image.imageId, m.image.queryUrl())))
                    messageBody.add(item)
                    pathBody.add(item)
                    itemNum++
                }
                is Voice -> {
                    val item = VoiceRecv(m)
                    messageBody.add(item)
                    pathBody.add(item)
                    itemNum++
                }
                is LightApp -> {
                    val item = JsonImpl(m.content)
                    messageBody.add(item)
                    pathBody.add(item)
                    itemNum++
                }
                is ServiceMessage -> {
                    val item = XmlImpl(m.serviceId, m.content)
                    messageBody.add(item)
                    pathBody.add(item)
                    itemNum++
                }
                else -> {
                    val item = NoImplItemImpl(m)
                    messageBody.add(item)
                    pathBody.add(item)
                    itemNum++
                }
            }
        }

        message.path = pathBody

        return message
    }

    fun startBot() {

//        BotEvent

        bot.subscribeAlways<BotReloginEvent> {
            registerCookie()
        }

        // 好友消息事件
        bot.subscribeAlways<FriendMessageEvent> {
            val message = message.toMessage() ?: return@subscribeAlways
            message.temp = false
            message.qq = this.sender.id

            val friend = yuq.friends[this.sender.id] ?: return@subscribeAlways
            rainBot.receivePrivateMessage(friend, message)
        }

        // 群消息事件
        bot.subscribeAlways<MiraiGroupMessageEvent> {
            val message = message.toMessage() ?: return@subscribeAlways
            message.temp = false
            message.group = this.subject.id
            message.qq = this.sender.id

            val member = yuq.groups[this.subject.id]!![this.sender.id]
            rainBot.receiveGroupMessage(member, message)
        }

        // 临时会话事件
        bot.subscribeAlways<TempMessageEvent> {
            val message = message.toMessage() ?: return@subscribeAlways
            message.temp = true
            message.group = this.group.id
            message.qq = this.sender.id

            val member = yuq.groups[this.sender.group.id]!![this.sender.id]
            rainBot.receivePrivateMessage(member, message)
        }

        // 新好友申请事件
        bot.subscribeAlways<MiraiNewFriendRequestEvent> {
            val e = NewFriendRequestEvent(this.fromId, this.message)
            if (eventBus.post(e)) {
                when (e.accept) {
                    true -> {
                        it.accept()
                        val mf = bot.friends[this.fromId]
                        val f = FriendImpl(mf)
                        this@MiraiBot.friends[f.id] = f
                    }
                    else -> it.reject()
                }
            }
        }
        // 机器人被邀请入群事件
        bot.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            val e = GroupInviteEvent(this.groupId, this.invitorId, "")
            if (eventBus.post(e)) {
                when (e.accept) {
                    true -> {
                        it.accept()
//                        val mg = bot.groups[this.groupId]
//                        val g = GroupImpl(mg)
//                        this@MiraiBot.groups[g.id] = g
                    }
                }
            }
        }
        // 有新成员申请入群事件
        bot.subscribeAlways<MemberJoinRequestEvent> {
            val e = GroupMemberRequestEvent(groups[this.groupId]!!, this.fromId, this.fromNick, this.message)
            if (eventBus.post(e) && e.accept != null)
                if (e.accept!!) {
                    it.accept()
                    val m = this.group[this.fromId]
                    val group = groups[this.group.id] ?: return@subscribeAlways
                    val member = GroupMemberImpl(m, group)
                    group.members[member.id] = member
                    eventBus.post(GroupMemberJoinEvent(group, member))
                } else it.reject(e.blackList)
        }

        // 好友部分变动监听
        bot.subscribeAlways<MiraiFriendAddEvent> {
            val friend = FriendImpl(friend)
            this@MiraiBot.friends[friend.id] = friend
            eventBus.post(FriendAddEvent(friend))
        }
        bot.subscribeAlways<MiraiFriendDeleteEvent> {
            val friend = this@MiraiBot.friends[friend.id] ?: return@subscribeAlways
            this@MiraiBot.friends.remove(friend.id)
            eventBus.post(FriendDeleteEvent(friend))
        }
        bot.subscribeAlways<FriendRemarkChangeEvent> {
//            this@MiraiBot.friends[friend.id]?.name = friend.nick
        }

        // 群部分变动监听
        bot.subscribeAlways<MiraiBotJoinGroupEvent> {
            val group = GroupImpl(group)
            this@MiraiBot.groups[group.id] = group
            eventBus.post(BotJoinGroupEvent(group))
        }
        bot.subscribeAlways<BotLeaveEvent> {
            val group = this@MiraiBot.groups[group.id] ?: return@subscribeAlways
            this@MiraiBot.groups.remove(group.id)
            eventBus.post(
                    if (this is BotLeaveEvent.Kick) BotLevelGroupEvent.Kick(group.get(operator.id))
                    else BotLevelGroupEvent.Level(group)
            )
        }
        bot.subscribeAlways<GroupNameChangeEvent> {
//            this@MiraiBot.groups[group.id]?.name = group.name
        }
        bot.subscribeAlways<MemberPermissionChangeEvent> {
//            this@MiraiBot.groups[group.id]!!.members[member.id]!!.permission = new.level
        }

        // 群成员部分变动监听
        bot.subscribeAlways<MemberJoinEvent> {
            val group = this@MiraiBot.groups[member.group.id] ?: return@subscribeAlways
            val member = GroupMemberImpl(member, group)
            group.members[member.id] = member
            eventBus.post(if (this is MemberJoinEvent.Invite) GroupMemberInviteEvent(group, member, member) else GroupMemberJoinEvent(group, member))
        }
        bot.subscribeAlways<MemberLeaveEvent> {
            val group = this@MiraiBot.groups[member.group.id] ?: return@subscribeAlways
            val member = group[member.id]
            group.members.remove(member.id)
            eventBus.post(
                    if (this is MemberLeaveEvent.Kick) GroupMemberKickEvent(group, member, group.members[operator?.id]
                            ?: group.bot)
                    else GroupMemberLeaveEvent(group, member)
            )
        }
        bot.subscribeAlways<MemberCardChangeEvent> {
//            this@MiraiBot.groups[member.group.id]?.members?.get(member.id)?.nameCard = member.nameCard
        }
        bot.subscribeAlways<MemberSpecialTitleChangeEvent> {
//            this@MiraiBot.groups[member.group.id]?.members?.get(member.id)?.title = member.specialTitle
        }

        fun GroupMemberEvent.getMember() = this@MiraiBot.groups[this.group.id]?.get(this.member.id)

        bot.subscribeAlways<MemberMuteEvent> {
            val member = this.getMember() ?: return@subscribeAlways
            val op = this@MiraiBot.groups[this.group.id]?.members?.get(this.operator?.id ?: -1)
                    ?: this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            eventBus.post(GroupBanMemberEvent(member.group, member, op, this.durationSeconds))
        }
        bot.subscribeAlways<MemberUnmuteEvent> {
            val member = this.getMember() ?: return@subscribeAlways
            val op = this@MiraiBot.groups[this.group.id]?.members?.get(this.operator?.id ?: -1)
                    ?: this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            eventBus.post(GroupUnBanMemberEvent(member.group, member, op))
        }
        bot.subscribeAlways<BotMuteEvent> {
            val member = this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            val op = this@MiraiBot.groups[this.group.id]?.get(this.operator.id) ?: return@subscribeAlways
            eventBus.post(GroupBanBotEvent(member.group, member, op, this.durationSeconds))
        }
        bot.subscribeAlways<BotUnmuteEvent> {
            val member = this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            val op = this@MiraiBot.groups[this.group.id]?.get(this.operator.id) ?: return@subscribeAlways
            eventBus.post(GroupUnBanBotEvent(member.group, member, op))
        }


        bot.subscribeAlways<MessageRecallEvent> {
            eventBus.post(when (this) {
                is MessageRecallEvent.GroupRecall -> {
                    val g = groups[group.id] ?: return@subscribeAlways
                    GroupRecallEvent(g, g.members[this.authorId] ?: g.bot, g.members[this.operator?.id]
                            ?: g.bot, this.messageId)
                }
                is MessageRecallEvent.FriendRecall -> PrivateRecallEvent(
                        friends[this.authorId] ?: return@subscribeAlways,
                        friends[this.operator] ?: return@subscribeAlways,
                        this.messageId)
            })
        }
    }

    override fun sendMessage(message: Message) =
            when {
                message.temp -> {
                    groups[message.group!!]!![message.qq!!]
                }
                message.group != null -> {
                    groups[message.group!!]!!
                }
                else -> {
                    friends[message.qq!!]!!
                }
            }.sendMessage(message)


    override fun recallMessage(messageSource: MessageSource): Int {
        return messageSource.recall()
    }

    override val avatar: String
        get() = bot.selfQQ.avatarUrl
    override val id: Long
        get() = botId
    override val name: String
        get() = bot.nick

    override fun canSendMessage() = false

    override fun isFriend() = false

}

@JobCenter
class MiraiJob {

    @Cron("2m")
    fun cf() {
        FPMM.clear()
    }

}