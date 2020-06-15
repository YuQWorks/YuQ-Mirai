package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.DefaultApp
import com.IceCreamQAQ.Yu.annotation.NotSearch
import com.IceCreamQAQ.Yu.di.ConfigManager
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.event.EventBus
import com.IceCreamQAQ.Yu.event.events.AppStartEvent
import com.IceCreamQAQ.Yu.job.JobManager
import com.IceCreamQAQ.Yu.loader.AppLoader
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@NotSearch
class YuQMirai {

    @Inject
    private lateinit var eventBus: EventBus

    @Inject
    private lateinit var miraiBot: MiraiBot

    @Inject
    private lateinit var jobManager: JobManager

    init {
        val logger = DefaultApp.PrintAppLog()

        val appClassloader = YuQMirai::class.java.classLoader!!
        val configer = ConfigManager(appClassloader, logger, null)
        val context = YuContext(configer, logger)

        context.putBean(ClassLoader::class.java, "appClassLoader", appClassloader)

        val bot = context[MiraiBot::class.java]!!
        bot.init()

        val loader = context[AppLoader::class.java]!!
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