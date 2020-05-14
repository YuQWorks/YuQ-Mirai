package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.DefaultApp
import com.IceCreamQAQ.Yu.annotation.NotSearch
import com.IceCreamQAQ.Yu.di.ConfigManager
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.event.EventBus
import com.IceCreamQAQ.Yu.event.events.AppStartEvent
import com.IceCreamQAQ.Yu.job.JobManager_
import com.IceCreamQAQ.Yu.loader.AppLoader_
import com.icecreamqaq.yuq.mirai.message.MiraiMessageFactory
import com.icecreamqaq.yuq.mirai.message.MiraiMessageItemFactory
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.join
import javax.inject.Inject

@NotSearch
class YuQMirai {

//    @Inject
//    private lateinit var loader: AppLoader_

    @Inject
    private lateinit var eventBus: EventBus

    @Inject
    private lateinit var miraiBot: MiraiBot

    @Inject
    private lateinit var jobManager: JobManager_

    init {
        val logger = DefaultApp.PrintAppLog()

        val appClassloader = YuQMirai::class.java.classLoader!!
        val configer = ConfigManager(appClassloader, logger, null)
        val context = YuContext(configer, logger)

        context.putBean(ClassLoader::class.java, "appClassLoader", appClassloader)

        val bot = context[MiraiBot::class.java]!!
        bot.init()

        val loader = context[AppLoader_::class.java]!!
        loader.load()

        context.injectBean(this)
        context.injectBean(bot)

    }

    fun start(){
        jobManager.start()

        runBlocking{
            miraiBot.startBot()
        }


        println("过了！")

        eventBus.post(AppStartEvent())
    }



}