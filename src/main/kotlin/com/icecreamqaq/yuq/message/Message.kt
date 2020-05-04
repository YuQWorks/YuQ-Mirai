package com.icecreamqaq.yuq.message

import com.IceCreamQAQ.Yu.entity.Result

abstract class Message : Result(), MessagePlus {

    var id: Int? = null
    var qq: Long? = null
    var group: Long? = null

    lateinit var sourceMessage: String
    var body = ArrayList<MessageItem>()

    fun toPath(): List<String> {
        val paths = ArrayList<String>()
        for (item in body) {
            paths.add(item.toPath())
        }
        return paths
    }

    override operator fun plus(item: MessageItem): Message {
        body.add(item)
        return this
    }

    override fun plus(item: Message): Message {
        body.addAll(item.body)
        return this
    }
}