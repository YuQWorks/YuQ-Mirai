package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.annotation.Default
import com.IceCreamQAQ.Yu.cache.EhcacheHelp
import com.IceCreamQAQ.Yu.controller.Router
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.event.EventBus
import com.IceCreamQAQ.Yu.toJSONObject
import com.IceCreamQAQ.Yu.toJSONString
import com.IceCreamQAQ.Yu.util.Web
import com.alibaba.fastjson.JSON
import com.icecreamqaq.yuq.*
import com.icecreamqaq.yuq.controller.ContextRouter
import com.icecreamqaq.yuq.controller.ContextSession
import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.event.*
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.mirai.entity.FriendImpl
import com.icecreamqaq.yuq.mirai.entity.GroupImpl
import com.icecreamqaq.yuq.mirai.entity.GroupMemberImpl
import com.icecreamqaq.yuq.mirai.logger.Network
import com.icecreamqaq.yuq.mirai.message.*
import com.icecreamqaq.yuqui.MyApp
import com.icecreamqaq.yuqui.WebView
import com.sun.javafx.application.PlatformImpl
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.contact.Friend as MiraiFriend
import net.mamoe.mirai.contact.Group as MiraiGroup
import net.mamoe.mirai.contact.NormalMember as MiraiMember
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.events.GroupMemberEvent
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.LoginSolver
import org.slf4j.LoggerFactory
import tornadofx.FX
import tornadofx.find
import tornadofx.launch
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.event.*
import java.awt.image.BufferedImage
import java.net.URI
import java.util.*
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.inject.Named
import javax.swing.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set
import net.mamoe.mirai.event.events.BotJoinGroupEvent as MiraiBotJoinGroupEvent
import net.mamoe.mirai.event.events.FriendAddEvent as MiraiFriendAddEvent
import net.mamoe.mirai.event.events.FriendDeleteEvent as MiraiFriendDeleteEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent as MiraiNewFriendRequestEvent
import net.mamoe.mirai.event.events.GroupMessageEvent as MiraiGroupMessageEvent
import net.mamoe.mirai.message.data.MessageSource as MiraiSource

open class MiraiBot : YuQ, ApplicationService, User, RainVersion {

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
    lateinit var group: Router

    @Inject
    @field:Named("priv")
    lateinit var priv: Router

    @Inject
    lateinit var contextRouter: ContextRouter

    @Inject
    override lateinit var web: Web

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    override lateinit var messageItemFactory: MiraiMessageItemFactory
//    override val web: Web

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
//        FPMM.getTime = { System.currentTimeMillis() }
//        FPMM.clear()

        mif = messageItemFactory
//        mf = messageFactory
        yuq = this
        botId = qq.toLong()
        com.icecreamqaq.yuq.web = web
        localEventBus = eventBus
        com.icecreamqaq.yuq.eventBus = eventBus
        com.icecreamqaq.yuq.mirai.miraiBot = this


        bot = BotFactory.newBot(botId, pwd) {
            fileBasedDeviceInfo()
            networkLoggerSupplier = { Network("Net ${it.id}") }
            botLoggerSupplier = { com.icecreamqaq.yuq.mirai.logger.Bot(("Bot ${it.id}")) }
            if (this@MiraiBot.protocol == "Android") protocol = BotConfiguration.MiraiProtocol.ANDROID_PHONE
            if (this@MiraiBot.protocol == "Watch") protocol = BotConfiguration.MiraiProtocol.ANDROID_WATCH
            if (this@MiraiBot.protocol == "HD") protocol = BotConfiguration.MiraiProtocol.ANDROID_PAD
            if ("-1" != System.getProperty("YuQ.Mirai.LoginUI")) loginSolver = MiraiUILoginSolver
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

        for (method in bot::class.java.declaredMethods) {
            if (method.name == "getClient") {

                method.isAccessible = true
                val client = method.invoke(bot)
                for (cm in client::class.java.declaredMethods) {
                    if (cm.name == "getWLoginSigInfo") {
                        cm.isAccessible = true
                        val lsi = cm.invoke(client)
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
                            val value = String(
                                Base64.getDecoder().decode(psKeys.getJSONObject(k).getString("data"))
                                    ?: continue
                            )
                            val pskey = YuQ.QQCookie.Pskey(value, f(value))
                            pskeyMap[k] = pskey
                            web.saveCookie(k, "/", "p_skey", value)
                            web.saveCookie(k, "/", "p_uin", "o$qq")
                        }

                        this.cookieEx.pskeyMap = pskeyMap

                        web.saveCookie("qq.com", "/", "uin", "o$qq")
                        web.saveCookie("qq.com", "/", "skey", sKey)
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

    private fun getOrNew(f: MiraiFriend) = friends[f.id] ?: {
        val a = FriendImpl(f)
        friends[a.id] = a
        a
    }()

    private fun getOrNew(g: MiraiGroup) = groups[g.id] ?: {
        val a = GroupImpl(g)
        groups[a.id] = a
        a
    }()

    private fun getOrNew(m: MiraiMember) = getOrNew(m.group).run {
        getOrNull(m.id) ?: {
            val member = GroupMemberImpl(m, this)
            members[member.id] = member
            if (member.permission == 1) admins.add(member)
            member
        }()
    }

    override fun refreshGroups(): Map<Long, Group> {
        val groups = HashMap<Long, GroupImpl>(bot.groups.size)
        for (group in bot.groups) {
            try {
                groups[group.id] = GroupImpl(group)
            } catch (e: Exception) {
                log.error("Load Group ${group.id} Error!", e)
            }

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
        message.id = miraiSource.ids[0]
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
        val eventChannel = bot.eventChannel

        eventChannel.subscribeAlways<BotReloginEvent> {
            registerCookie()
        }

        // 好友消息事件
        eventChannel.subscribeAlways<FriendMessageEvent> {
            val message = message.toMessage() ?: return@subscribeAlways
//            message.temp = false
//            message.qq = this.sender.id

            val friend = getOrNew(this.sender)
            rainBot.receiveFriendMessage(friend, message)
        }

        // 群消息事件
        eventChannel.subscribeAlways<MiraiGroupMessageEvent> {
            val message = message.toMessage() ?: return@subscribeAlways
//            message.temp = false
//            message.group = this.subject.id
//            message.qq = this.sender.id

            val group = getOrNew(this.sender.group)
//            val member = when (this.sender.id) {
////                80000000L -> AnonymousMemberImpl(this.sender, group)
//                else -> getOrNew(this.sender)
//            }
            val member = when (this.sender) {
                is MiraiMember -> getOrNew(this.sender as MiraiMember)
                else -> return@subscribeAlways
            }

            rainBot.receiveGroupMessage(member, message)
        }

        // 临时会话事件
        eventChannel.subscribeAlways<GroupTempMessageEvent> {
            val message = message.toMessage() ?: return@subscribeAlways
//            message.temp = true
//            message.group = this.group.id
//            message.qq = this.sender.id

            val member = getOrNew(this.sender as MiraiMember)
            rainBot.receiveTempMessage(member, message)
        }

        // 新好友申请事件
        eventChannel.subscribeAlways<MiraiNewFriendRequestEvent> {
            val ui = UserInfo(
                id = this.fromId,
                avatar = "",
                name = this.fromNick,
                sex = UserSex.none,
                age = 0,
                qqAge = 0,
                level = 0,
                loginDays = 0,
                vips = listOf()
            )
            val g = this.fromGroup?.let { getOrNew(it) }
            val e = NewFriendRequestEvent(ui, g, this.message)
            if (eventBus.post(e)) {
                when (e.accept) {
                    true -> {
                        it.accept()
                        val mf = this.bot.friends[this.fromId]!!
                        val f = FriendImpl(mf)
                        this@MiraiBot.friends[f.id] = f
                    }
                    else -> it.reject()
                }
            }
        }
        // 机器人被邀请入群事件
        eventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            val ui = UserInfo(
                id = this.invitorId,
                avatar = "",
                name = this.invitorNick,
                sex = UserSex.none,
                age = 0,
                qqAge = 0,
                level = 0,
                loginDays = 0,
                vips = listOf()
            )
            val gi = GroupInfo(
                id = this.groupId,
                name = this.groupName,
                maxCount = 0,
                owner = ui,
                admin = listOf()
            )
            val e = GroupInviteEvent(gi, ui, "")
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
        eventChannel.subscribeAlways<MemberJoinRequestEvent> {
            val ui = UserInfo(
                id = this.fromId,
                avatar = "",
                name = this.fromNick,
                sex = UserSex.none,
                age = 0,
                qqAge = 0,
                level = 0,
                loginDays = 0,
                vips = listOf()
            )
            val e = GroupMemberRequestEvent(getOrNew(this.group!!), ui, this.message)
            if (eventBus.post(e) && e.accept != null)
                if (e.accept!!) {
                    it.accept()
                    val m = this.group!![this.fromId]!!
                    val group = getOrNew(this.group!!)
                    val member = GroupMemberImpl(m, group)
                    group.members[member.id] = member
                    eventBus.post(GroupMemberJoinEvent(group, member))
                } else it.reject(e.blackList)
        }

        // 好友部分变动监听
        eventChannel.subscribeAlways<MiraiFriendAddEvent> {
            val friend = FriendImpl(friend)
            this@MiraiBot.friends[friend.id] = friend
            eventBus.post(FriendAddEvent(friend))
        }
        eventChannel.subscribeAlways<MiraiFriendDeleteEvent> {
            val friend = this@MiraiBot.friends[friend.id] ?: return@subscribeAlways
            this@MiraiBot.friends.remove(friend.id)
            eventBus.post(FriendDeleteEvent(friend))
        }
        eventChannel.subscribeAlways<FriendRemarkChangeEvent> {
//            this@MiraiBot.friends[friend.id]?.name = friend.nick
        }

        // 群部分变动监听
        eventChannel.subscribeAlways<MiraiBotJoinGroupEvent> {
            val group = GroupImpl(group)
            this@MiraiBot.groups[group.id] = group
            eventBus.post(BotJoinGroupEvent(group))
        }
        eventChannel.subscribeAlways<BotLeaveEvent> {
            val group = this@MiraiBot.groups[group.id] ?: return@subscribeAlways
            this@MiraiBot.groups.remove(group.id)
            eventBus.post(
                if (this is BotLeaveEvent.Kick) BotLeaveGroupEvent.Kick(group[operator.id])
                else BotLeaveGroupEvent.Leave(group)
            )
        }
        eventChannel.subscribeAlways<GroupNameChangeEvent> {
//            this@MiraiBot.groups[group.id]?.name = group.name
        }
        eventChannel.subscribeAlways<MemberPermissionChangeEvent> {
//            this@MiraiBot.groups[group.id]!!.members[member.id]!!.permission = new.level
//            when (new.level){
//
//            }
            groups[group.id]?.refreshAdmin()
        }

        // 群成员部分变动监听
        eventChannel.subscribeAlways<MemberJoinEvent> {
            val group = getOrNew(member.group)
            val member = GroupMemberImpl(member, group)
            group.members[member.id] = member
            eventBus.post(
                if (this is MemberJoinEvent.Invite) GroupMemberInviteEvent(
                    group,
                    member,
                    member
                ) else GroupMemberJoinEvent.Join(group, member)
            )
        }
        eventChannel.subscribeAlways<MemberLeaveEvent> {
            val group = getOrNew(member.group)
            val member = group[member.id]
            group.members.remove(member.id)
            eventBus.post(
                if (this is MemberLeaveEvent.Kick) GroupMemberKickEvent(
                    group, member, group.members[operator?.id]
                        ?: group.bot
                )
                else GroupMemberLeaveEvent.Leave(group, member)
            )
        }
        eventChannel.subscribeAlways<MemberCardChangeEvent> {
//            this@MiraiBot.groups[member.group.id]?.members?.get(member.id)?.nameCard = member.nameCard
        }
        eventChannel.subscribeAlways<MemberSpecialTitleChangeEvent> {
//            this@MiraiBot.groups[member.group.id]?.members?.get(member.id)?.title = member.specialTitle
        }

        fun GroupMemberEvent.getMember() = getOrNew(member as MiraiMember)

        eventChannel.subscribeAlways<MemberMuteEvent> {
            val member = this.getMember()
            val op = this@MiraiBot.groups[this.group.id]?.members?.get(this.operator?.id ?: -1)
                ?: this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            eventBus.post(GroupBanMemberEvent(member.group, member, op, this.durationSeconds))
        }
        eventChannel.subscribeAlways<MemberUnmuteEvent> {
            val member = this.getMember()
            val op = this@MiraiBot.groups[this.group.id]?.members?.get(this.operator?.id ?: -1)
                ?: this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            eventBus.post(GroupUnBanMemberEvent(member.group, member, op))
        }
        eventChannel.subscribeAlways<BotMuteEvent> {
            val member = this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            val op = this@MiraiBot.groups[this.group.id]?.get(this.operator.id) ?: return@subscribeAlways
            eventBus.post(GroupBanBotEvent(member.group, member, op, this.durationSeconds))
        }
        eventChannel.subscribeAlways<BotUnmuteEvent> {
            val member = this@MiraiBot.groups[this.group.id]?.bot ?: return@subscribeAlways
            val op = this@MiraiBot.groups[this.group.id]?.get(this.operator.id) ?: return@subscribeAlways
            eventBus.post(GroupUnBanBotEvent(member.group, member, op))
        }

//        NudgeEvent


//        eventChannel.subscribeAlways<MemberNudgedEvent> {
//            if (from.id == botId) return@subscribeAlways
//            val group = groups[from.group.id] ?: return@subscribeAlways
//            ClickSomeBodyEvent.Group(group[from.id], group[member.id], action, suffix)()
//        }
//        eventChannel.subscribeAlways<BotNudgedEvent> {
//            if (from.id == botId) return@subscribeAlways
//            if (from is MiraiMember) {
//                val group = groups[(from as MiraiMember).group.id] ?: return@subscribeAlways
//                ClickBotEvent.Group(group[from.id], action, suffix)
//            } else {
//                ClickBotEvent.Private.FriendClick(friends[from.id] ?: return@subscribeAlways, action, suffix)
//            }()
//        }


        eventChannel.subscribeAlways<MessageRecallEvent> {
            eventBus.post(
                when (this) {
                    is MessageRecallEvent.GroupRecall -> {
                        val g = getOrNew(group)
                        GroupRecallEvent(
                            g, g.members[this.authorId] ?: g.bot, g.members[this.operator?.id]
                                ?: g.bot, this.messageIds[0]
                        )
                    }
                    is MessageRecallEvent.FriendRecall -> PrivateRecallEvent(
                        friends[this.authorId] ?: return@subscribeAlways,
                        friends[this.operator.id] ?: return@subscribeAlways,
                        this.messageIds[0]
                    )
                }
            )
        }
    }

//    override fun sendMessage(message: Message) =
//            when {
//                message.temp -> {
//                    groups[message.group!!]!![message.qq!!]
//                }
//                message.group != null -> {
//                    groups[message.group!!]!!
//                }
//                else -> {
//                    friends[message.qq!!]!!
//                }
//            }.sendMessage(message)
//
//
//    override fun recallMessage(messageSource: MessageSource): Int {
//        return messageSource.recall()
//    }

    override val avatar: String
        get() = bot.avatarUrl
    override val id: Long
        get() = botId
    override val name: String
        get() = bot.nick

    override fun canSendMessage() = false

    override fun isFriend() = false
    override fun runtimeName() = "YuQ-Mirai"

    override fun runtimeVersion() = "0.1.0.0-DEV14"

}

object MiraiUILoginSolver : LoginSolver() {

    override val isSliderCaptchaSupported = true

    private var kukuCap = false
    private var inifFx = false

    override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String? {
        return openWindow("Mirai PicCaptcha(${bot.id})") {
            val image = ImageIO.read(data.inputStream())
            JLabel(ImageIcon(image)).append()
        }
    }

    override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {

        val ticket = if (!kukuCap) {
            if (!inifFx) {
                if ("-1" != System.getProperty("YuQ.Mirai.LoginUI")) GlobalScope.launch { launch<MyApp>() }
                else error("当前环境不存在 JavaFx 或是不允许 UI 界面！")
                delay(2000)
            }
            val def = CompletableDeferred<String>()
            PlatformImpl.runLater {
                find<WebView>(FX.defaultScope, mapOf("url" to url, "def" to def)).openWindow()
            }
            def.await()
        } else (web.post("https://api.kuku.me/tool/captcha", mapOf("url" to url)).toJSONObject().getJSONObject("data")
            ?.getString("ticket") ?: "").apply { println("Kuku Cap Result: $this") }
        kukuCap = true
        return ticket
    }

    override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
        val title = "Mirai UnsafeDeviceLoginVerify(${bot.id})"
        return openWindow(title) {
            JLabel(
                """
                <html>
                需要进行账户安全认证<br>
                该账户有[设备锁]/[不常用登录地点]/[不常用设备登录]的问题<br>
                完成以下账号认证即可成功登录|理论本认证在mirai每个账户中最多出现1次<br>
                成功后请关闭该窗口
            """.trimIndent()
            ).append()
            HyperLinkLabel(url, "设备锁验证", title).last()
        }
    }

    suspend fun openWindow(title: String = "", initializer: WindowInitializer.(JFrame) -> Unit = {}): String {
        return openWindow(title, WindowInitializer(initializer))
    }

    suspend fun openWindow(title: String = "", initializer: WindowInitializer = WindowInitializer {}): String {
        val frame = JFrame()
//        frame.iconImage = windowImage
        frame.minimumSize = Dimension(228, 62) // From Windows 10
        val value = JTextField()
        val def = CompletableDeferred<String>()
        value.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {
            }

            override fun keyPressed(e: KeyEvent?) {
                when (e!!.keyCode) {
                    27, 10 -> {
                        def.complete(value.text)
                    }
                }
            }

            override fun keyReleased(e: KeyEvent?) {
            }
        })
        frame.layout = BorderLayout(10, 5)
        frame.add(value, BorderLayout.SOUTH)
        initializer.init(frame)

        frame.pack()
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                def.complete(value.text)
            }
        })
        frame.setLocationRelativeTo(null)
        frame.title = title
        frame.isVisible = true

        return def.await().trim().also {
            SwingUtilities.invokeLater {
                frame.dispose()
            }
        }
    }

}

class WindowInitializer(private val initializer: WindowInitializer.(JFrame) -> Unit) {
    private lateinit var frame0: JFrame
    val frame: JFrame get() = frame0
    fun java.awt.Component.append() {
        frame.add(this, BorderLayout.NORTH)
    }

    fun java.awt.Component.last() {
        frame.add(this)
    }

    internal fun init(frame: JFrame) {
        this.frame0 = frame
        initializer(frame)
    }
}

class HyperLinkLabel constructor(
    url: String,
    text: String,
    fallbackTitle: String
) : JLabel() {
    init {
        super.setText("<html><a href='$url'>$text</a></html>")
        addMouseListener(object : MouseAdapter() {

            override fun mouseClicked(e: MouseEvent) {
                // Try to open browser safely. #694
                try {
                    Desktop.getDesktop().browse(URI(url))
                } catch (ex: Exception) {
                    JOptionPane.showInputDialog(
                        this@HyperLinkLabel,
                        "Mirai 无法直接打开浏览器, 请手动复制以下 URL 打开",
                        fallbackTitle,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        null,
                        url
                    )
                }
            }
        })
    }
}

//internal val windowImage: BufferedImage? by lazy {
//    WindowHelperJvm::class.java.getResourceAsStream("project-mirai.png")?.use {
//        ImageIO.read(it)
//    }
//}
//@JobCenter
//class MiraiJob {
//
//    @Cron("2m")
//    fun cf() {
//        FPMM.clear()
//    }
//
//}