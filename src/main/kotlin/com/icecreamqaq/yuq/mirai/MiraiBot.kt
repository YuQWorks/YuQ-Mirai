package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.AppLogger
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.controller.router.RouterPlus
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.event.EventBus
import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.event.GroupInviteEvent
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.event.GroupMessageEvent
import com.icecreamqaq.yuq.event.PrivateMessageEvent
import com.icecreamqaq.yuq.mirai.controller.MiraiBotActionContext
import com.icecreamqaq.yuq.mirai.message.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.sourceId

import javax.inject.Inject
import javax.inject.Named

class MiraiBot : YuQ {

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
    private lateinit var logger: AppLogger

    @Inject
    private lateinit var eventBus: EventBus

    @Inject
    override lateinit var messageFactory: MiraiMessageFactory

    @Inject
    override lateinit var messageItemFactory: MiraiMessageItemFactory


    @Inject
    private lateinit var context: YuContext

    private lateinit var bot: Bot

    fun init() {
        bot = Bot(qq.toLong(), pwd)
        context.putBean(Bot::class.java, "", bot)
    }

    suspend fun startBot() {

        val qqLong = qq.toLong()
        bot.alsoLogin()


        bot.subscribeMessages {
            always {

                val privateMessage = this.sender == this.subject
                val messageSource = this.message.toString()

                logger.logDebug(
                        "MiraiBot",
                        "Receive Message, Sender: ${this.sender.id}(${this.subject.id}), MessageBody: $messageSource."
                )

                val message = MiraiMessage()

                message.id = this.message[MessageSource].id
                message.qq = this.sender.id
                if (!privateMessage) message.group = this.subject.id

                message.sourceMessage = messageSource

                val messageBody = message.body

                var itemNum = 0
                loop@ for (m in this.message) {
                    when (m) {
                        is MessageSource -> continue@loop
                        is PlainText -> {
                            val sm = m.stringValue.trim()
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
                        else -> {
                            messageBody.add(NoImplItemImpl(m.toString()))
                            itemNum++
                        }
                    }
                }

                if (
                        if (privateMessage) eventBus.post(PrivateMessageEvent(message))
                        else eventBus.post(GroupMessageEvent(message))
                ) return@always

                val actionContext = MiraiBotActionContext()

                actionContext.message = message

                if (privateMessage) priv.invoke(actionContext.path[0], actionContext)
                else group.invoke(actionContext.path[0], actionContext)

                sendMessage((actionContext.result ?: return@always) as Message)

            }
        }


        bot.subscribeAlways<NewFriendRequestEvent> {
            val e = com.icecreamqaq.yuq.event.NewFriendRequestEvent()
            if (eventBus.post(e) && e.accept) it.accept()
        }
        bot.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            val e = GroupInviteEvent()
            if (eventBus.post(e) && e.accept) it.accept()
        }

//        bot.subscribeAlways<> {
//            it.accept()
//        }
    }

    override fun sendMessage(message: Message): Int {
        var mm: MessageChain = buildMessageChain {}

        for (messageItem in message.body) {
            mm += messageItem.toLocal(bot, message) as net.mamoe.mirai.message.data.Message
        }

        val re = runBlocking {
            if (message.group != null) bot.groups[message.group!!].sendMessage(mm)
            else bot.friends[message.qq!!].sendMessage(mm)
        }

        return re.sourceId
    }


}