package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.AppLogger
import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.annotation.Default
import com.IceCreamQAQ.Yu.cache.EhcacheHelp
import com.IceCreamQAQ.Yu.controller.router.NewRouter
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.event.EventBus
import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.controller.ContextRouter
import com.icecreamqaq.yuq.controller.ContextSession
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.event.*
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.mf
import com.icecreamqaq.yuq.mif
import com.icecreamqaq.yuq.mirai.entity.MiraiFriend
import com.icecreamqaq.yuq.mirai.entity.MiraiGroup
import com.icecreamqaq.yuq.mirai.entity.MiraiGroupMember
import com.icecreamqaq.yuq.mirai.logger.Network
import com.icecreamqaq.yuq.mirai.message.*
import com.icecreamqaq.yuq.yuq
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.events.GroupMemberEvent
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.TempMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.BotConfiguration
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named
import kotlin.collections.set
import kotlin.coroutines.resume
import net.mamoe.mirai.event.events.BotJoinGroupEvent as MiraiBotJoinGroupEvent
import net.mamoe.mirai.event.events.FriendAddEvent as MiraiFriendAddEvent
import net.mamoe.mirai.event.events.FriendDeleteEvent as MiraiFriendDeleteEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent as MiraiNewFriendRequestEvent
import net.mamoe.mirai.message.GroupMessageEvent as MiraiGroupMessageEvent
import net.mamoe.mirai.message.data.MessageSource as MiraiSource

class MiraiBot : YuQ, ApplicationService {

    private val log = LoggerFactory.getLogger(MiraiBot::class.java)

    @Config("YuQ.Mirai.user.qq")
    private lateinit var qq: String

    @Config("YuQ.Mirai.user.pwd")
    private lateinit var pwd: String

    @Config("YuQ.bot.name")
    private var botName: String? = null

    @Config("YuQ.Mirai.protocol")
    @Default("HD")
    private lateinit var protocol: String

    @Inject
    @field:Named("group")
    private lateinit var group: NewRouter

    @Inject
    @field:Named("priv")
    private lateinit var priv: NewRouter

    @Inject
    private lateinit var contextRouter: ContextRouter

    @Inject
    private lateinit var logger: AppLogger

    @Inject
    private lateinit var eventBus: EventBus

    @Inject
    override lateinit var messageFactory: MiraiMessageFactory

    @Inject
    override lateinit var messageItemFactory: MiraiMessageItemFactory

    @Inject
    @field:Named("ContextSession")
    lateinit var sessionCache: EhcacheHelp<ContextSession>

    @Inject
    private lateinit var context: YuContext

    private lateinit var bot: Bot
    override lateinit var friends: HashMap<Long, MiraiFriend>
    override lateinit var groups: HashMap<Long, MiraiGroup>


//    var DefaultLogger: (identity: String?) -> MiraiLogger = { YuQMiraiLogger }

    override fun init() {
        mif = messageItemFactory
        mf = messageFactory
        yuq = this

        bot = Bot(qq.toLong(), pwd) {
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

        refreshFriends()
        refreshGroups()
    }

    override fun refreshFriends(): Map<Long, Friend> {
        val friends = HashMap<Long, MiraiFriend>(bot.friends.size)
        for (friend in bot.friends) {
            friends[friend.id] = MiraiFriend(friend, this)
        }
        this.friends = friends
        return friends
    }

    override fun refreshGroups(): Map<Long, Group> {
        val groups = HashMap<Long, MiraiGroup>(bot.groups.size)
        for (group in bot.groups) {
            groups[group.id] = MiraiGroup(group, this)
        }
        this.groups = groups
        return groups
    }

    override fun start() {
//        context.injectBean(this)
        startBot()
    }

    override fun stop() {
        bot.close()
    }

    fun startBot() {

        val qqLong = qq.toLong()

        bot.subscribeMessages {
            always {

                val temp = this is TempMessageEvent

                val messageSource = this.message.toString()

                val message = MiraiMessage()

                val miraiSource = this.message[MiraiSource] ?: return@always
                val source = MiraiMessageSource(miraiSource)
                message.source = source
                message.temp = temp

                message.id = miraiSource.id
                message.qq = this.sender.id
                message.group = when (this) {
                    is TempMessageEvent -> this.sender.group.id
                    is MiraiGroupMessageEvent -> this.subject.id
                    else -> null
                }

                message.sourceMessage = this.message

                val pathBody = ArrayList<MessageItem>()
                val messageBody = message.body

                var itemNum = 0
                loop@ for (m in this.message) {
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
                            if (itemNum == 0 && m.target == qqLong) continue@loop
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

                log.info("Receive Message: $message")


                if (
                        when (this) {
                            is TempMessageEvent -> eventBus.post(PrivateMessageEvent(message))
                            is FriendMessageEvent -> eventBus.post(PrivateMessageEvent(message))
                            is MiraiGroupMessageEvent -> eventBus.post(GroupMessageEvent(message))
                            else -> false
                        }
                ) return@always

                if (pathBody.size == 0) return@always

                val actionContext = BotActionContext()
                val sessionId = if (temp) "t_" else "" + message.qq + "_" + message.group

                val session = sessionCache[sessionId] ?: {
                    val session = ContextSession(sessionId)
                    eventBus.post(ContextSessionCreateEvent(session))
                    sessionCache[sessionId] = session
                    session
                }()

//                val t = this@MiraiBot

                actionContext.session = session
                actionContext.message = message

                actionContext["session"] = session

                if (session.suspendCoroutineIt != null) {
                    session.suspendCoroutineIt!!.resume(message)
                } else {
                    if (eventBus.post(ActionContextInvokeEvent.Per(actionContext))) return@always

                    when {
                        session.context != null -> contextRouter.invoke(session.context!!, actionContext)
                        temp || message.group == null -> priv.invoke(actionContext.path[0], actionContext)
                        else -> group.invoke(actionContext.path[0], actionContext)
                    }

                    session.context = actionContext.nextContext?.router

                    eventBus.post(ActionContextInvokeEvent.Post(actionContext))

                    if (session.context != null) {
                        val msg = contextRouter.routers[session.context!!]?.tips?.get(actionContext.nextContext?.status)
                        if (msg != null)
                            sendMessage(actionContext.message!!.newMessage().plus(msg))
                    }
                    sendMessage((actionContext.reMessage ?: return@always))
                }
            }
        }


        bot.subscribeAlways<MiraiNewFriendRequestEvent> {
            val e = NewFriendRequestEvent(this.fromId, this.message)
            if (eventBus.post(e) && e.accept) it.accept()
        }
        bot.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            val e = GroupInviteEvent(this.groupId, this.invitorId, "")
            if (eventBus.post(e) && e.accept) it.accept()
        }
        bot.subscribeAlways<MemberJoinRequestEvent> {
            val e = GroupMemberRequestEvent(groups[this.groupId]!!, this.fromId, this.fromNick, this.message)
            if (eventBus.post(e) && e.accept != null)
                if (e.accept!!) it.accept()
                else it.reject(e.blackList)
        }

        // 好友部分变动监听
        bot.subscribeAlways<MiraiFriendAddEvent> {
            val friend = MiraiFriend(friend, this@MiraiBot)
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
            val group = MiraiGroup(group, this@MiraiBot)
            this@MiraiBot.groups[group.id] = group
            eventBus.post(BotJoinGroupEvent(group))
        }
        bot.subscribeAlways<BotLeaveEvent> {
            val group = this@MiraiBot.groups[group.id] ?: return@subscribeAlways
            this@MiraiBot.groups.remove(group.id)
            eventBus.post(BotLevelGroupEvent(group))
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
            val member = MiraiGroupMember(member, group)
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
            eventBus.post(GroupBanMemberEvent(member, op, this.durationSeconds))
        }
        bot.subscribeAlways<MemberUnmuteEvent> {
            val member = this.getMember() ?: return@subscribeAlways
            val op = this@MiraiBot.groups[this.group.id]?.members?.get(this.operator?.id ?: -1)
                    ?: this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            eventBus.post(GroupUnBanMemberEvent(member, op))
        }
        bot.subscribeAlways<BotMuteEvent> {
            val member = this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            val op = this@MiraiBot.groups[this.group.id]?.get(this.operator.id) ?: return@subscribeAlways
            eventBus.post(GroupBanBotEvent(member, op, this.durationSeconds))
        }
        bot.subscribeAlways<BotUnmuteEvent> {
            val member = this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            val op = this@MiraiBot.groups[this.group.id]?.get(this.operator.id) ?: return@subscribeAlways
            eventBus.post(GroupUnBanBotEvent(member, op))
        }


        bot.subscribeAlways<MessageRecallEvent> {
            eventBus.post(when (this) {
                is MessageRecallEvent.GroupRecall -> GroupRecallEvent(this.group.id, this.authorId, this.operator?.id
                        ?: qqLong, this.messageId)
                is MessageRecallEvent.FriendRecall -> PrivateRecallEvent(this.authorId, this.operator, this.messageId)
            })
        }
    }

    override fun sendMessage(message: Message): MessageSource {
        log.info("SendMessage: $message")
        var mm = buildMessageChain {}

        if (message.reply != null) mm += QuoteReply((message.reply as MiraiMessageSource).source)
        if (message.at) mm += AtImpl(message.qq!!).toLocal(bot, message)

        for (messageItem in message.body) {
            mm += messageItem.toLocal(bot, message) as net.mamoe.mirai.message.data.Message
        }

        val re =
                try {
                    runBlocking {
                        when {
                            message.temp -> bot.groups[message.group!!][message.qq!!].sendMessage(mm)
                            message.group != null -> bot.groups[message.group!!].sendMessage(mm)
                            else -> bot.friends[message.qq ?: error("Message Receiver Not Found!")].sendMessage(mm)
                        }
                    }
                } catch (e: Exception) {
                    log.info("SendMessage Fail!", e)
                    throw  e
                }

        log.info("SendMessage Success!")
        return MiraiMessageSource(re.source)
    }

    override fun recallMessage(messageSource: MessageSource): Int {
        return messageSource.recall()
    }

}