package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.AppLogger
import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.cache.EhcacheHelp
import com.IceCreamQAQ.Yu.controller.router.RouterPlus
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
import com.icecreamqaq.yuq.mirai.entity.MiraiFriend
import com.icecreamqaq.yuq.mirai.entity.MiraiGroup
import com.icecreamqaq.yuq.mirai.entity.MiraiGroupMember
import com.icecreamqaq.yuq.mirai.message.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent as MiraiNewFriendRequestEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.GroupMessageEvent as MiraiGroupMessageEvent
import net.mamoe.mirai.message.TempMessageEvent
import net.mamoe.mirai.message.data.MessageSource as MiraiSource
import net.mamoe.mirai.message.data.*

import javax.inject.Inject
import javax.inject.Named

class MiraiBot : YuQ, ApplicationService {

    @Config("YuQ.Mirai.user.qq")
    private lateinit var qq: String

    @Config("YuQ.Mirai.user.pwd")
    private lateinit var pwd: String

    @Config("YuQ.bot.name")
    private var botName: String? = null

    @Inject
    @field:Named("group")
    private lateinit var group: RouterPlus

    @Inject
    @field:Named("priv")
    private lateinit var priv: RouterPlus

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

    override fun init() {
        bot = Bot(qq.toLong(), pwd){
            fileBasedDeviceInfo()
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

    }

    fun startBot() {

        val qqLong = qq.toLong()

        bot.subscribeMessages {
            always {

                val temp = this is TempMessageEvent

                val messageSource = this.message.toString()

                logger.logDebug(
                        "MiraiBot",
                        "Receive Message, Sender: ${this.sender.id}(${this.subject.id}), MessageBody: $messageSource."
                )

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
                        is Face ->{
                            val item=(FaceImpl(m.id))
                            messageBody.add(item)
                            pathBody.add(item)
                            itemNum++
                        }
                        is OnlineImage -> {
                            val item =(ImageReceive(m.imageId, m.originUrl))
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
                    sessionCache[sessionId] = session
                    session
                }()

                actionContext.session = session
                actionContext.message = message

                when {
                    session.context != null -> contextRouter.invoke(session.context!!, actionContext)
                    temp || message.group == null -> priv.invoke(actionContext.path[0], actionContext)
                    else -> group.invoke(actionContext.path[0], actionContext)
                }

                session.context = actionContext.nextContext?.router

                if (session.context != null) {
                    val msg = contextRouter.routers[session.context!!]?.tips?.get(actionContext.nextContext?.status)
                    if (msg != null)
                        sendMessage(actionContext.message!!.newMessage().plus(msg))
                }
                sendMessage((actionContext.result ?: return@always) as Message)
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
            if (eventBus.post(e) && e.accept) it.accept()
        }

        // 好友部分变动监听
        bot.subscribeAlways<FriendAddEvent> {
            this@MiraiBot.friends[friend.id] = MiraiFriend(friend, this@MiraiBot)
        }
        bot.subscribeAlways<FriendDeleteEvent> {
            this@MiraiBot.friends.remove(friend.id)
        }
        bot.subscribeAlways<FriendRemarkChangeEvent> {
//            this@MiraiBot.friends[friend.id]?.name = friend.nick
        }

        // 群部分变动监听
        bot.subscribeAlways<BotJoinGroupEvent> {
            this@MiraiBot.groups[group.id] = MiraiGroup(group, this@MiraiBot)
        }
        bot.subscribeAlways<BotLeaveEvent> {
            this@MiraiBot.groups.remove(group.id)
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
        }
        bot.subscribeAlways<MemberLeaveEvent> {
            this@MiraiBot.groups[member.group.id]?.members?.remove(member.id)
        }
        bot.subscribeAlways<MemberCardChangeEvent> {
//            this@MiraiBot.groups[member.group.id]?.members?.get(member.id)?.nameCard = member.nameCard
        }
        bot.subscribeAlways<MemberSpecialTitleChangeEvent> {
//            this@MiraiBot.groups[member.group.id]?.members?.get(member.id)?.title = member.specialTitle
        }


        bot.subscribeAlways<MessageRecallEvent> {
            eventBus.post(when (this) {
                is MessageRecallEvent.GroupRecall -> GroupRecallEvent(this.group.id, this.authorId, this.operator?.id ?: qqLong, this.messageId)
                is MessageRecallEvent.FriendRecall -> PrivateRecallEvent(this.authorId, this.operator, this.messageId)
            })
        }
    }

    override fun sendMessage(message: Message): MessageSource {
        var mm: MessageChain = buildMessageChain {}

        if (message.reply != null) mm += QuoteReply((message.reply as MiraiMessageSource).source)
        if (message.at) mm += AtImpl(message.qq!!).toLocal(bot, message)

        for (messageItem in message.body) {
            mm += messageItem.toLocal(bot, message) as net.mamoe.mirai.message.data.Message
        }

        val re = runBlocking {
            when {
                message.temp -> bot.groups[message.group!!][message.qq!!].sendMessage(mm)
                message.group != null -> bot.groups[message.group!!].sendMessage(mm)
                else -> bot.friends[message.qq!!].sendMessage(mm)
            }
        }

        return MiraiMessageSource(re.source)
    }

    override fun recallMessage(messageSource: MessageSource): Int {
        return messageSource.recall()
    }


}