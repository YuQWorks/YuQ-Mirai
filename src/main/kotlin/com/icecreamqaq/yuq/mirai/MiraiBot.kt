package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.AppLogger
import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.cache.EhcacheHelp
import com.IceCreamQAQ.Yu.controller.router.RouterPlus
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.event.EventBus
import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.controller.ContextSession
import com.icecreamqaq.yuq.event.GroupInviteEvent
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.mirai.controller.MiraiBotActionContext
import com.icecreamqaq.yuq.mirai.message.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.event.subscribeMessages
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
    @field:Named("context")
    private lateinit var contextRouter: RouterPlus

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

    override fun init() {
        bot = Bot(qq.toLong(), pwd)
        runBlocking {
            bot.alsoLogin()
        }
        context.putBean(Bot::class.java, "", bot)
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

                val temp = this.sender == this.subject
                val messageSource = this.message.toString()

                logger.logDebug(
                        "MiraiBot",
                        "Receive Message, Sender: ${this.sender.id}(${this.subject.id}), MessageBody: $messageSource."
                )

                val message = MiraiMessage()

                val miraiSource = this.message[MiraiSource] ?: return@always
                val source = MiraiMessageSource(miraiSource)
                message.source = source

                message.id = miraiSource.id
                message.qq = this.sender.id
                if (!temp) message.group = this.subject.id

                message.sourceMessage = messageSource

                val messageBody = message.body

                var itemNum = 0
                loop@ for (m in this.message) {
                    when (m) {
                        is MiraiSource -> continue@loop
                        is QuoteReply -> message.reply = MiraiMessageSource(m.source)
                        is PlainText -> {
                            val sm = m.content.trim()
                            if (sm.isEmpty()) continue@loop
                            val sms = sm.split(" ")
                            var loopStart = 0
                            if (itemNum == 0 && botName != null && sms[0] == botName) loopStart = 1
                            for (i in loopStart until sms.size) {
                                messageBody.add(TextImpl(sms[i]))
                                itemNum++
                            }
                        }
                        is At -> {
                            if (itemNum == 0 && m.target == qqLong) continue@loop
                            messageBody.add(AtImpl(m.target))
                            itemNum++
                        }
                        is OnlineImage -> {
                            messageBody.add(ImageReceive(m.imageId, m.originUrl))
                            itemNum++
                        }
                        else -> {
                            messageBody.add(NoImplItemImpl(m.toString()))
                            itemNum++
                        }
                    }
                }

                if (
                        if (temp) eventBus.post(PrivateMessageEvent(message))
                        else eventBus.post(GroupMessageEvent(message))
                ) return@always

                val actionContext = MiraiBotActionContext()
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
                    temp -> priv.invoke(actionContext.path[0], actionContext)
                    else -> group.invoke(actionContext.path[0], actionContext)
                }

                session.context = actionContext.nextContext

                sendMessage((actionContext.result ?: return@always) as Message)
            }
        }


        bot.subscribeAlways<NewFriendRequestEvent> {
            val e = com.icecreamqaq.yuq.event.NewFriendRequestEvent()
            if (eventBus.post(e) && e.accept) it.accept()
        }
        bot.subscribeAlways<MemberJoinRequestEvent> {
            val e = GroupInviteEvent()
            if (eventBus.post(e) && e.accept) it.accept()
        }
    }

    override fun sendMessage(message: Message): MessageSource {
        var mm: MessageChain = buildMessageChain {}

        if (message.reply != null) mm += QuoteReply((message.reply as MiraiMessageSource).source)

        for (messageItem in message.body) {
            mm += messageItem.toLocal(bot, message) as net.mamoe.mirai.message.data.Message
        }

        val re = runBlocking {
            if (message.group != null) bot.groups[message.group!!].sendMessage(mm)
            else bot.friends[message.qq!!].sendMessage(mm)
        }

        return MiraiMessageSource(re.source)
    }

    override fun recallMessage(messageSource: MessageSource): Int {
        return messageSource.recall()
    }


}