package com.icecreamqaq.yuq.mirai.logger

import net.mamoe.mirai.utils.MiraiLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class LogBase: MiraiLogger  {
    abstract val log: Logger

    override val isEnabled = true
    override var follower: MiraiLogger? = null

    override fun verbose(message: String?) {
        log.trace(message)
    }

    override fun verbose(message: String?, e: Throwable?) {
        log.trace(message, e)
    }

    override fun debug(message: String?) {
        log.debug(message)
    }

    override fun debug(message: String?, e: Throwable?) {
        log.debug(message, e)
    }

    override fun info(message: String?) {
        log.info(message)
    }

    override fun info(message: String?, e: Throwable?) {
        log.info(message, e)
    }

    override fun warning(message: String?) {
        log.warn(message)
    }

    override fun warning(message: String?, e: Throwable?) {
        log.warn(message, e)
    }

    override fun error(message: String?) {
        log.error(message)
    }

    override fun error(message: String?, e: Throwable?) {
        log.error(message, e)
    }


    override fun <T : MiraiLogger> plus(follower: T): T = follower
}

class Network(override val identity: String?) : LogBase() {
    override val log = LoggerFactory.getLogger(Network::class.java)
}

class Bot(override val identity: String?) : LogBase() {
    override val log = LoggerFactory.getLogger(Bot::class.java)
}

