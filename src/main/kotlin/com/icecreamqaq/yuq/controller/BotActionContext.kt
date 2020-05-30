package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.ActionContext
import com.IceCreamQAQ.Yu.entity.Result
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.mirai.message.MiraiMessage
import com.icecreamqaq.yuq.mirai.message.TextImpl

open class BotActionContext : ActionContext {

    override lateinit var path: Array<String>
    override var result: Result? = null

    lateinit var session: ContextSession
    var nextContext: NextActionContext? = null

    private val saved = HashMap<String, Any?>()

    init {
        saved["actionContext"] = this
    }

    var message: Message? = null
        set(message) {
            field = message!!

            path = message.toPath().toTypedArray()

            saved["messageId"] = message.id

            saved["qq"] = message.qq
            saved["group"] = message.group

            saved["sourceMessage"] = message.sourceMessage

            saved["message"] = message
        }


    override fun get(name: String): Any? {
        return saved[name] ?: session[name]
    }

    override fun set(name: String, obj: Any) {
        saved[name] = obj
    }

    override fun buildResult(obj: Any): Message?{
        return when (obj) {
            is String -> {
                val message = this.message!!.newMessage()
                val mb = message.body
                mb.add(TextImpl(obj))
                message
            }
            is MessageItem -> {
                val message = this.message!!.newMessage()
                val mb = message.body
                mb.add(obj)
                message
            }
            is Message -> obj
            else -> buildResult(obj.toString())
        }
    }

}